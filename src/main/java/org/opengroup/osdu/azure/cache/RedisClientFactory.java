package org.opengroup.osdu.azure.cache;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredential;
import com.azure.security.keyvault.secrets.SecretClient;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.MappingSocketAddressResolver;
import io.lettuce.core.internal.HostAndPort;
import org.opengroup.osdu.azure.KeyVaultFacade;
import org.opengroup.osdu.azure.di.MSIConfiguration;
import org.opengroup.osdu.azure.di.RedisAzureConfiguration;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.azure.logging.ICoreLogger;
import org.opengroup.osdu.core.common.cache.IRedisCache;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.cache.RedisClusterCache;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.Credentials;
import org.redisson.config.CredentialsResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis client factory.
 * @param <V>
 */
@Component
@Lazy
public class RedisClientFactory<V> implements IRedisClientFactory<V> {
    private static final String LOGGER_NAME = RedisClientFactory.class.getName();
    private static final String AZURE_REDIS_SCOPE = "https://redis.azure.com/.default";
    private static final int AZURE_MANAGED_REDIS_DATABASE = 0; // AMR only has support for a single logical DB

    /** Maximum time to wait for a token from IMDS before giving up. */
    private static final Duration IMDS_TIMEOUT = Duration.ofSeconds(10);
    private static final int AZURE_MANAGED_REDIS_PORT = 10000;

    private static RedissonClient redissonClient = null;
    private static ClientResources clusterClientResources = null;

    @Autowired
    private DefaultAzureCredential defaultAzureCredential;

    @Autowired
    private SecretClient secretClient;

    @Autowired
    private MSIConfiguration msiConfiguration;

    private Map<String, IRedisCache<String, V>> redisClientMap;

    /**
     * Initializes the private variables as required.
     */
    @PostConstruct
    public void initialize() {
        redisClientMap = new ConcurrentHashMap<>();
    }

    /**
     * Get redis client with optional custom codec.
     * @param valueClass Class type for value
     * @param redisConfiguration configuration for redis client
     * @param codec custom codec for encoding/decoding (null to use default)
     * @return Redis client
     */
    @Override
    public IRedisCache<String, V> getClient(
        final Class<V> valueClass,
        final RedisAzureConfiguration redisConfiguration,
        final @Nullable RedisCodec<String, V> codec) {

        final ICoreLogger logger = CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME);

        String codecKeyPart = codec == null ? "defaultCodec" : codec.getClass().getName();
        String cacheKey = String.format("%s-%s", valueClass.toString(), codecKeyPart);

        // With RedisCredentialsProvider, Lettuce handles MSI token refresh automatically
        // No need for manual connection recreation - credentials provider is called on AUTH errors
        if (this.redisClientMap.containsKey(cacheKey)) {
            IRedisCache<String, V> cacheObject = this.redisClientMap.get(cacheKey);
            String cachedType = getCacheTypeNameForLogging(cacheObject);
            if (cacheObject instanceof RedisCache || cacheObject instanceof RedisClusterCache) {
                logger.debug("Returning existing Redis client from cache for key: {}, type: {}", cacheKey, cachedType);
                return cacheObject;
            } else {
                // Cached client is not a valid Redis client (likely NoOpRedisCache). Re-try to initialize.
                logger.info("Cached client type {} is not a valid Redis client. Re-checking if Redis is available.", cachedType);
            }
        }

        IRedisCache<String, V> redisCache = this.redisClientMap.computeIfAbsent(cacheKey, key ->
            createRedisClient(valueClass, redisConfiguration, codec)
        );

        if (redisCache == null) {
            logger.warn("Redis client creation returned null for cache key: {}. Returning NoOpRedisCache.", cacheKey);
            return new NoOpRedisCache<>();
        }

        logger.info("Successfully obtained Redis client for cache key: {}, type: {}", cacheKey, getCacheTypeNameForLogging(redisCache));
        return redisCache;
    }

    /**
     * Create RedissonClient instance assuming redis-host and redis-password already exists.
     * @param applicationName application name.
     * @param redisAzureConfiguration configuration for redis client.
     * @return Redisson client instance
     */
    @Override
    public RedissonClient getRedissonClient(final String applicationName, final RedisAzureConfiguration redisAzureConfiguration) {
        final ICoreLogger logger = CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME);

        // With CredentialsResolver, Redisson handles MSI token refresh automatically on reconnection
        // No need for proactive client replacement - let Redisson manage token lifecycle
        if (redissonClient == null) {
            synchronized (RedissonClient.class) {
                if (redissonClient == null) {
                    redissonClient = createNewRedissonClient(applicationName, redisAzureConfiguration);
                    logger.info("Created new Redisson client with CredentialsResolver for automatic MSI token management");
                }
            }
        }
        return redissonClient;
    }

    /**
     * Get a clean type name for logging purposes.
     * Handles anonymous classes and provides specific names for known types.
     * @param cache the cache instance to get the type name for
     * @return human-readable type name for logging
     */
    private String getCacheTypeNameForLogging(final IRedisCache<?, ?> cache) {
        if (cache instanceof RedisClusterCache) {
            return "RedisClusterCache";
        }
        if (cache instanceof RedisCache) {
            return "RedisCache";
        }
        Class<?> cacheClass = cache.getClass();
        return cacheClass.getSimpleName().isEmpty() ? cacheClass.getName() : cacheClass.getSimpleName();
    }

    /**
     * Create redis client object.
     * @param valueClass Class type for value
     * @param redisConfiguration configuration for redis client
     * @param codec custom codec for encoding/decoding (null to use default)
     * @return redis client
     */
    private IRedisCache<String, V> createRedisClient(
        final Class<V> valueClass,
        final RedisAzureConfiguration redisConfiguration,
        final RedisCodec<String, V> codec) {

        final ICoreLogger logger = CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME);
        String host = resolveRedisHost(redisConfiguration);

        if (host == null) {
            logger.warn("Redis host does not exist. Redis is not available yet.");
            return null;
        }

        final int port = redisConfiguration.getPort();
        final String principalId = redisConfiguration.getPrincipalId();

        logger.info("Starting Redis client creation - Host: {}, Port: {}, Principal ID configured: {}", host, port, (principalId != null && !principalId.isBlank() ? "yes" : "no"));

        // Determine the correct database based on Redis type (new AzureManagedRedis vs old AzureCacheForRedis)
        // Note: For AMR (cluster mode), the database value is used as a key prefix to maintain backward compatibility, since AMR only supports database 0.
        int database = getDatabaseForRedisType(port, redisConfiguration.getDatabase());
        final int expiration = redisConfiguration.getExpiration();
        final int commandTimeout = redisConfiguration.getCommandTimeout();

        ClientOptions options = ClientOptions.builder()
            .socketOptions(SocketOptions.builder()
            .connectTimeout(Duration.ofSeconds(redisConfiguration.getConnectionTimeout())).build())
            .build();

        // Configure cluster options with topology refresh for Azure Managed Redis
        // Topology refresh ensures cluster changes are detected and routing is updated
        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
            .enablePeriodicRefresh(Duration.ofSeconds(30))
            .enableAllAdaptiveRefreshTriggers()
            .build();

        ClusterClientOptions clusterOptions = ClusterClientOptions.builder()
            .socketOptions(options.getSocketOptions())
            .timeoutOptions(options.getTimeoutOptions())
            .topologyRefreshOptions(topologyRefreshOptions)
            .build();

        // Resolve authentication: MSI or password
        AzureRedisMsiCredentialsProvider credentialsProvider = null;
        String password = null;

        if (canUseMsiAuthentication(principalId)) {
            logger.info("Using MSI authentication with RedisCredentialsProvider");
            credentialsProvider = new AzureRedisMsiCredentialsProvider(principalId, defaultAzureCredential);
        } else {
            if (msiConfiguration.getIsEnabled()) {
                logger.info("MSI enabled but no valid principal ID, falling back to password authentication");
            }
            password = getSecret(redisConfiguration.getPasswordKey());
            if (password == null) {
                logger.warn("Redis password does not exist. Redis is not available yet.");
                return null;
            }
        }

        // Azure Managed Redis (port 10000) always runs in cluster mode
        // Azure Cache for Redis (port 6380) always runs in standalone mode
        if (port == AZURE_MANAGED_REDIS_PORT) {
            logger.info("Creating Redis CLUSTER client for Azure Managed Redis");

            try {
                // Get or create ClientResources with address resolver for cluster mode
                // This maps cluster node IP addresses to the Azure hostname for proper SSL/TLS verification
                ClientResources resources = getOrCreateClusterClientResources(host);

                if (credentialsProvider == null) {
                    logger.info("Creating RedisClusterCache with password authentication for host: {}, port: {}", host, port);
                    return new RedisClusterCache<V>(host, port, null, password, expiration, commandTimeout, clusterOptions, resources, valueClass) {
                        @Override
                        public RedisCodec<String, V> getCodec(final Class<String> classOfK, final Class<V> classOfV) {
                            return codec == null ? super.getCodec(classOfK, classOfV) : codec;
                        }
                    };
                } else {
                    logger.info("Creating RedisClusterCache with MSI authentication for host: {}, port: {}", host, port);
                    return new RedisClusterCache<V>(host, port, credentialsProvider, expiration, commandTimeout, clusterOptions, resources, valueClass) {
                        @Override
                        public RedisCodec<String, V> getCodec(final Class<String> classOfK, final Class<V> classOfV) {
                            return codec == null ? super.getCodec(classOfK, classOfV) : codec;
                        }
                    };
                }
            } catch (Exception e) {
                logger.error("Failed to create Redis CLUSTER client for Azure Managed Redis. Host: {}, Port: {}, Exception: {}", host, port, e.getMessage(), e);
                return null;
            }
        } else {
            logger.info("Creating standalone Redis client for Azure Cache for Redis with no cluster support.");

            try {
                if (credentialsProvider == null) {
                    logger.info("Creating RedisCache with password authentication for host: {}, port: {}", host, port);
                    return new RedisCache<String, V>(host, port, null, password, expiration, commandTimeout, database, true, options, String.class, valueClass) {
                        @Override
                        public RedisCodec<String, V> getCodec(final Class<String> classOfK, final Class<V> classOfV) {
                            return codec == null ? super.getCodec(classOfK, classOfV) : codec;
                        }
                    };
                } else {
                    logger.info("Creating RedisCache with MSI authentication for host: {}, port: {}", host, port);
                    return new RedisCache<String, V>(host, port, credentialsProvider, expiration, commandTimeout, database, true, options, String.class, valueClass) {
                        @Override
                        public RedisCodec<String, V> getCodec(final Class<String> classOfK, final Class<V> classOfV) {
                            return codec == null ? super.getCodec(classOfK, classOfV) : codec;
                        }
                    };
                }
            } catch (Exception e) {
                logger.error("Failed to create standalone Redis client for Azure Cache for Redis. Host: {}, Port: {}, Exception: {}", host, port, e.getMessage(), e);
                return null;
            }
        }
    }

    /**
     * Create a new RedissonClient instance.
     * Automatically detects Azure Managed Redis (port 10000) and configures cluster mode.
     * @param applicationName application name for the client
     * @param redisAzureConfiguration configuration for redis client
     * @return new RedissonClient instance or null if creation fails
     */
    private RedissonClient createNewRedissonClient(final String applicationName, final RedisAzureConfiguration redisAzureConfiguration) {
        final ICoreLogger logger = CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME);
        String redisHost = resolveRedisHost(redisAzureConfiguration);
        if (redisHost == null) {
            logger.warn("Redis host does not exist. Redisson client is not available yet.");
            return null;
        }

        final int port = redisAzureConfiguration.getPort();
        final String principalId = redisAzureConfiguration.getPrincipalId();

        // Determine the correct database based on Redis type (new AzureManagedRedis vs old AzureCacheForRedis)
        int database = getDatabaseForRedisType(port, redisAzureConfiguration.getDatabase());

        // Azure Managed Redis (AMR) runs in cluster mode - detect by port 10000
        boolean isAzureManagedRedis = (port == AZURE_MANAGED_REDIS_PORT);

        Config config = new Config();

        if (isAzureManagedRedis) {
            // Configure for cluster mode (AMR)
            logger.info("Detected Azure Managed Redis (port 10000) - configuring Redisson for cluster mode");

            var clusterConfig = config.useClusterServers()
                .addNodeAddress(String.format("rediss://%s:%d", redisHost, port)) // Single seed node; client will auto-discover remaining cluster nodes
                .setScanInterval(30000) // Scan cluster topology every 30 seconds
                .setKeepAlive(true)
                .setClientName(applicationName);

            if (canUseMsiAuthentication(principalId)) {
                // Use CredentialsResolver for dynamic MSI token refresh in cluster mode
                clusterConfig.setCredentialsResolver(new RedissonMsiCredentialsResolver(principalId));
                logger.info("Configured Redisson CLUSTER client with RedissonMsiCredentialsResolver for dynamic token refresh");
            } else {
                if (msiConfiguration.getIsEnabled()) {
                    logger.info("MSI is enabled but no valid principal ID provided, using password authentication for Redisson CLUSTER client.");
                }
                String password = getSecret(redisAzureConfiguration.getPasswordKey());
                if (password == null) {
                    logger.warn("Redis password does not exist. Redisson CLUSTER client is not available yet.");
                    return null;
                }
                clusterConfig.setPassword(password);
            }
        } else {
            // Configure for single-server mode (Azure Cache for Redis)
            logger.info("Configuring Redisson for single-server mode (Azure Cache for Redis)");

            var serverConfig = config.useSingleServer()
                .setAddress(String.format("rediss://%s:%d", redisHost, port))
                .setDatabase(database)
                .setKeepAlive(true)
                .setClientName(applicationName);

            if (canUseMsiAuthentication(principalId)) {
                // Use CredentialsResolver for dynamic MSI token refresh - this is the proper way in Redisson 3.52+
                serverConfig.setCredentialsResolver(new RedissonMsiCredentialsResolver(principalId));
                logger.info("Configured Redisson client with RedissonMsiCredentialsResolver for dynamic token refresh");
            } else {
                if (msiConfiguration.getIsEnabled()) {
                    logger.info("MSI is enabled but no valid principal ID provided, using password authentication for Redisson client.");
                }
                // Static password authentication for non-MSI scenarios or MSI without valid principal ID
                String password = getSecret(redisAzureConfiguration.getPasswordKey());
                if (password == null) {
                    logger.warn("Redis password does not exist. Redisson client is not available yet.");
                    return null;
                }
                serverConfig.setPassword(password);
            }
        }

        return Redisson.create(config);
    }

    /**
     * Check if MSI authentication can be used.
     * @param principalId the principal ID from configuration
     * @return true if MSI authentication is enabled and principal ID is valid
     */
    private boolean canUseMsiAuthentication(final String principalId) {
        return msiConfiguration.getIsEnabled() && principalId != null && !principalId.isBlank();
    }

    /**
     * Resolve the Redis hostname from either direct configuration or Azure Key Vault.
     * When direct hostname is provided, enforces MSI authentication.
     * @param redisConfiguration the Redis configuration
     * @return the Redis hostname, or null if not available
     * @throws IllegalArgumentException if direct hostname is provided without valid MSI authentication
     */
    private String resolveRedisHost(final RedisAzureConfiguration redisConfiguration) {
        final ICoreLogger logger = CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME);

        String host = redisConfiguration.getHostname();

        if (host != null && !host.isBlank()) {
            // Direct hostname path: supports both MSI and password authentication
            logger.info("Using direct hostname for Redis connection: {}", host);
            return host;
        }

        // Legacy path: get host from Azure Key Vault
        return getSecret(redisConfiguration.getHostKey());
    }

    /**
     * Determine the database number to use based on Redis type.
     * Azure Managed Redis (port 10000) does not support database selection, so always use 0.
     * Azure Cache for Redis (other ports) uses the configured database number.
     * @param port the Redis port from configuration
     * @param configuredDatabase the database number from configuration
     * @return 0 for Azure Managed Redis, configured database for Azure Cache for Redis
     */
    private int getDatabaseForRedisType(final int port, final int configuredDatabase) {
        return port == AZURE_MANAGED_REDIS_PORT ? AZURE_MANAGED_REDIS_DATABASE : configuredDatabase;
    }

    /**
     * Get MSI authentication token for Redis.
     * A 10-second timeout guards against IMDS throttling under thundering-herd
     * conditions (many pods refreshing simultaneously at token expiry).
     * @return access token string or null if authentication fails
     */
    private String getTokenFromMsi() {
        final ICoreLogger logger = CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME);
        try {
            TokenRequestContext tokenRequestContext = new TokenRequestContext();
            tokenRequestContext.addScopes(AZURE_REDIS_SCOPE);

            AccessToken accessToken = defaultAzureCredential.getToken(tokenRequestContext)
                    .timeout(IMDS_TIMEOUT)
                    .block();

            final String token = accessToken == null ? null : accessToken.getToken();
            if (token == null || token.isBlank()) {
                logger.warn("Failed to acquire access token from MSI");
                return null;
            }

            if (accessToken.getExpiresAt() != null) {
                logger.info("Successfully obtained access token for Redis MSI authentication. Token expires at: {}", accessToken.getExpiresAt());
            } else {
                logger.info("Successfully obtained access token for Redis MSI authentication (no expiry info available)");
            }
            return token;

        } catch (Exception e) {
            logger.warn("Failed to authenticate with MSI", e);
            return null;
        }
    }

    /**
     * Get secret from Key vault.
     * @param keyName name of the secret
     * @return Secret value
     */
    private String getSecret(final String keyName) {
        return KeyVaultFacade.getSecretWithDefault(secretClient, keyName, null);
    }

    /**
     * Get or create ClientResources with address resolver for cluster mode.
     * Azure Managed Redis cluster topology discovery returns IP addresses, but SSL/TLS
     * hostname verification requires connecting via the hostname. This resolver maps all
     * cluster node addresses to the Azure hostname while preserving port numbers.
     * This allows full SSL/TLS validation including hostname verification.
     * @param redisHost the Azure Redis hostname (e.g., "yourcache.redis.cache.windows.net")
     * @return ClientResources configured with address mapping for cluster nodes
     */
    private ClientResources getOrCreateClusterClientResources(final String redisHost) {
        if (clusterClientResources == null) {
            synchronized (RedisClientFactory.class) {
                if (clusterClientResources == null) {
                    final ICoreLogger logger = CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME);
                    logger.info("Creating ClientResources with MappingSocketAddressResolver for host: {}", redisHost);

                    try {
                        // Map all cluster node addresses (returned as IPs) to the public hostname
                        // This ensures SSL/TLS uses the hostname for SNI and certificate validation
                        clusterClientResources = DefaultClientResources.builder()
                            .socketAddressResolver(MappingSocketAddressResolver.create(
                                (HostAndPort hostAndPort) -> HostAndPort.of(redisHost, hostAndPort.getPort())
                            ))
                            .build();
                        logger.info("Successfully created ClientResources for host: {}", redisHost);
                    } catch (Exception e) {
                        logger.error("Failed to create ClientResources for host: {}. Exception: {}", redisHost, e.getMessage(), e);
                        throw e; // Re-throw to propagate to caller
                    }
                }
            }
        }
        return clusterClientResources;
    }

    /**
     * CredentialsResolver implementation for MSI authentication with Redisson 3.52+.
     * Dynamically fetches fresh MSI tokens for Redis authentication on each connection.
     * This enables automatic token refresh without manual client recreation.
     */
    private class RedissonMsiCredentialsResolver implements CredentialsResolver {
        private final String principalId;

        /**
         * Constructor for RedissonMsiCredentialsResolver.
         * @param principalIdParam the principal ID for MSI authentication
         */
        RedissonMsiCredentialsResolver(final String principalIdParam) {
            this.principalId = principalIdParam;
        }

        /**
         * Resolves fresh MSI credentials for the Redis connection.
         * @param address the Redis server address
         * @return a CompletionStage containing the credentials
         */
        @Override
        public CompletionStage<Credentials> resolve(final InetSocketAddress address) {
            final ICoreLogger logger = CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME);

            try {
                final String token = getTokenFromMsi();
                if (token != null) {
                    logger.debug("RedissonMsiCredentialsResolver providing fresh credentials for address: {} (principal ID: {})", address, principalId);
                    return CompletableFuture.completedFuture(new Credentials(principalId, token));
                } else {
                    logger.error("RedissonMsiCredentialsResolver failed to obtain MSI credentials for address: {}", address);
                    return CompletableFuture.failedFuture(new RuntimeException("Failed to obtain MSI credentials"));
                }
            } catch (Exception e) {
                logger.error("RedissonMsiCredentialsResolver error resolving credentials for address: {}", address, e);
                return CompletableFuture.failedFuture(e);
            }
        }
    }
}
