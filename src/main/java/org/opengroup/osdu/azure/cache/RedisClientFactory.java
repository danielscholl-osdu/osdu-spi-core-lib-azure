package org.opengroup.osdu.azure.cache;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredential;
import com.azure.security.keyvault.secrets.SecretClient;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.codec.RedisCodec;
import org.opengroup.osdu.azure.KeyVaultFacade;
import org.opengroup.osdu.azure.di.MSIConfiguration;
import org.opengroup.osdu.azure.di.RedisAzureConfiguration;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.azure.logging.ICoreLogger;
import org.opengroup.osdu.core.common.cache.IRedisCache;
import org.opengroup.osdu.core.common.cache.RedisCache;
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
    private static final int AZURE_MANAGED_REDIS_PORT = 10000;
    private static RedissonClient redissonClient = null;

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
            if (cacheObject instanceof RedisCache) {
                return cacheObject;
            } else {
                // No Op cache it is. Re-try to initialize cache.
                logger.info("The client is of NoOpRedisCache. Re-checking if Redis is available.");
            }
        }

        IRedisCache<String, V> redisCache = this.redisClientMap.computeIfAbsent(cacheKey, key ->
            createRedisClient(valueClass, redisConfiguration, codec)
        );
        return redisCache == null ? new NoOpRedisCache<>() : redisCache;
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

        // Determine the correct database based on Redis type (new AzureManagedRedis vs old AzureCacheForRedis)
        int database = getDatabaseForRedisType(port, redisConfiguration.getDatabase());

        ClientOptions options = ClientOptions.builder()
            .socketOptions(SocketOptions.builder()
            .connectTimeout(Duration.ofSeconds(redisConfiguration.getConnectionTimeout())).build())
            .build();

        final String principalId = redisConfiguration.getPrincipalId();

        if (canUseMsiAuthentication(principalId)) { // MSI authentication path (both direct hostname and legacy with MSI enabled)
            // Create credentials provider via factory for automatic token refresh
            AzureRedisMsiCredentialsProvider credentialsProvider = new AzureRedisMsiCredentialsProvider(principalId, defaultAzureCredential);

            logger.info("Creating Redis client with AzureRedisMsiCredentialsProvider for automatic MSI token refresh");

            return new RedisCache<String, V>(
                host,
                port,
                credentialsProvider, // Pass provider for automatic token refresh
                redisConfiguration.getExpiration(),
                redisConfiguration.getCommandTimeout(),
                database,
                true, // sslEnabled
                options,
                String.class,
                valueClass
            ) {
                @Override // Anonymous inner class allows per-instance codec customization via closure without creating yet an additional extension/implementation class of the common RedisCache
                public RedisCodec<String, V> getCodec(final Class<String> classOfK, final Class<V> classOfV) {
                    return codec == null ? super.getCodec(classOfK, classOfV) : codec;
                }
            };
        } else {
            // Password-based authentication fallback
            if (msiConfiguration.getIsEnabled()) {
                logger.info("MSI is enabled but no valid principal ID provided, falling back to password authentication.");
            }

            String password = getSecret(redisConfiguration.getPasswordKey());
            if (password == null) {
                logger.warn("Redis password does not exist. Redis is not available yet.");
                return null;
            }

            return new RedisCache<String, V>(
                host,
                port,
                null, // No username for password-based auth
                password,
                redisConfiguration.getExpiration(),
                redisConfiguration.getCommandTimeout(),
                database,
                true, // sslEnabled
                options,
                String.class,
                valueClass
            ) {
                @Override // same comment as above regarding anonymous inner class
                public RedisCodec<String, V> getCodec(final Class<String> classOfK, final Class<V> classOfV) {
                    return codec == null ? super.getCodec(classOfK, classOfV) : codec;
                }
            };
        }
    }

    /**
     * Create a new RedissonClient instance.
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

        // Determine the correct database based on Redis type (new AzureManagedRedis vs old AzureCacheForRedis)
        int database = getDatabaseForRedisType(port, redisAzureConfiguration.getDatabase());

        Config config = new Config();
        var serverConfig = config.useSingleServer()
            .setAddress(String.format("rediss://%s:%d", redisHost, port))
            .setDatabase(database)
            .setKeepAlive(true)
            .setClientName(applicationName);

        final String principalId = redisAzureConfiguration.getPrincipalId();

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
            // Direct hostname path: enforce MSI authentication
            if (!canUseMsiAuthentication(redisConfiguration.getPrincipalId())) {
                throw new IllegalArgumentException("MSI authentication with valid principal ID is required when using direct hostname for Redis connection");
            }
            logger.info("Using direct hostname for Redis connection with MSI authentication");
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
     * @return access token string or null if authentication fails
     */
    private String getTokenFromMsi() {
        final ICoreLogger logger = CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME);
        try {
            TokenRequestContext tokenRequestContext = new TokenRequestContext();
            tokenRequestContext.addScopes(AZURE_REDIS_SCOPE);

            AccessToken accessToken = defaultAzureCredential.getToken(tokenRequestContext).block();
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
                    logger.info("RedissonMsiCredentialsResolver providing fresh credentials for address: {} (principal ID: {})", address, principalId);
                    // Use principal ID as username, not client ID
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
