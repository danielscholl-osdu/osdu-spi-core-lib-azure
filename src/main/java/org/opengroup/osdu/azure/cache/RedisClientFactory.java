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
 * @param <K>
 * @param <V>
 */
@Component
@Lazy
public class RedisClientFactory<K, V> implements IRedisClientFactory<K, V> {
    private static final String LOGGER_NAME = RedisClientFactory.class.getName();
    private static final String AZURE_REDIS_SCOPE = "https://redis.azure.com/.default";
    private static RedissonClient redissonClient = null;

    @Autowired
    private DefaultAzureCredential defaultAzureCredential;

    @Autowired
    private SecretClient secretClient;

    @Autowired
    private MSIConfiguration msiConfiguration;

    private Map<String, IRedisCache<K, V>> redisClientMap;

    /**
     * Initializes the private variables as required.
     */
    @PostConstruct
    public void initialize() {
        redisClientMap = new ConcurrentHashMap<>();
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

    /**
     * Get redis client with optional custom codec.
     * @param keyClass Class type for key
     * @param valueClass Class type for value
     * @param redisConfiguration configuration for redis client
     * @param codec custom codec for encoding/decoding (null to use default)
     * @return Redis client
     */
    @Override
    public IRedisCache<K, V> getClient(
        final Class<K> keyClass,
        final Class<V> valueClass,
        final RedisAzureConfiguration redisConfiguration,
        final RedisCodec<K, V> codec) {

        final ICoreLogger logger = CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME);

        String codecKeyPart = codec == null ? "defaultCodec" : codec.getClass().getName();
        String cacheKey = String.format("%s-%s-%s", keyClass.toString(), valueClass.toString(), codecKeyPart);

        // With RedisCredentialsProvider, Lettuce handles MSI token refresh automatically
        // No need for manual connection recreation - credentials provider is called on AUTH errors
        if (this.redisClientMap.containsKey(cacheKey)) {
            IRedisCache<K, V> cacheObject = this.redisClientMap.get(cacheKey);
            if (cacheObject instanceof RedisCache) {
                return cacheObject;
            } else {
                // No Op cache it is. Re-try to initialize cache.
                logger.info("The client is of NoOpRedisCache. Re-checking if Redis is available.");
            }
        }

        IRedisCache<K, V> redisCache = this.redisClientMap.computeIfAbsent(cacheKey, key ->
            createRedisClient(keyClass, valueClass, redisConfiguration, codec)
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
     * @param keyClass Class type for key
     * @param valueClass Class type for value
     * @param redisConfiguration configuration for redis client
     * @param codec custom codec for encoding/decoding (null to use default)
     * @return redis client
     */
    private IRedisCache<K, V> createRedisClient(
        final Class<K> keyClass,
        final Class<V> valueClass,
        final RedisAzureConfiguration redisConfiguration,
        final RedisCodec<K, V> codec) {

        final ICoreLogger logger = CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME);
        final String host = getSecret(redisConfiguration.getHostKey());

        if (host == null) {
            logger.warn("Redis host does not exist. Redis is not available yet.");
            return null;
        }

        ClientOptions options = ClientOptions.builder()
            .socketOptions(SocketOptions.builder()
            .connectTimeout(Duration.ofSeconds(redisConfiguration.getConnectionTimeout())).build())
            .build();

        final String principalId = redisConfiguration.getPrincipalId();
        if (canUseMsiAuthentication(principalId)) {
            // Create credentials provider via factory for automatic token refresh
            AzureRedisMsiCredentialsProvider credentialsProvider = new AzureRedisMsiCredentialsProvider(principalId, defaultAzureCredential);

            logger.info("Creating Redis client with AzureRedisMsiCredentialsProvider for automatic MSI token refresh");

            return new RedisCache<K, V>(
                host,
                redisConfiguration.getPort(),
                credentialsProvider, // Pass provider for automatic token refresh
                redisConfiguration.getExpiration(),
                redisConfiguration.getCommandTimeout(),
                redisConfiguration.getDatabase(),
                true, // sslEnabled
                options,
                keyClass,
                valueClass
            ) {
                @Override // Anonymous inner class allows per-instance codec customization via closure without creating yet an additional extension/implementation class of the common RedisCache
                public RedisCodec<K, V> getCodec(final Class<K> classOfK, final Class<V> classOfV) {
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

            // When MSI is enabled, use principalId as username even with password auth
            String username = msiConfiguration.getIsEnabled() ? principalId : null;

            return new RedisCache<>(
                host,
                redisConfiguration.getPort(),
                username, // principalId if MSI enabled, null otherwise
                password,
                redisConfiguration.getExpiration(),
                redisConfiguration.getCommandTimeout(),
                redisConfiguration.getDatabase(),
                true, // sslEnabled
                options,
                keyClass,
                valueClass
            ) {
                @Override // same comment as above regarding anonymous inner class
                public RedisCodec<K, V> getCodec(final Class<K> classOfK, final Class<V> classOfV) {
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
        final String redisHost = getSecret(redisAzureConfiguration.getHostKey());
        if (redisHost == null) {
            logger.warn("Redis host does not exist. Redisson client is not available yet.");
            return null;
        }

        Config config = new Config();
        var serverConfig = config.useSingleServer()
            .setAddress(String.format("rediss://%s:%d", redisHost, redisAzureConfiguration.getPort()))
            .setDatabase(redisAzureConfiguration.getDatabase())
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
}
