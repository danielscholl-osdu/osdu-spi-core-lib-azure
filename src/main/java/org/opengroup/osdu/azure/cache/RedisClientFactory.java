package org.opengroup.osdu.azure.cache;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredential;
import com.azure.security.keyvault.secrets.SecretClient;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
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
import java.time.Instant;
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
    private static final Duration TOKEN_EXPIRY_THRESHOLD = Duration.ofMinutes(30);
    private static RedissonClient redissonClient = null;

    @Autowired
    private DefaultAzureCredential defaultAzureCredential;

    @Autowired
    private SecretClient secretClient;

    @Autowired
    private MSIConfiguration msiConfiguration;

    private Map<String, IRedisCache<K, V>> redisClientMap;
    private Map<String, Instant> redisClientAuthenticationTimes;

    /**
     * Initializes the private variables as required.
     */
    @PostConstruct
    public void initialize() {
        redisClientMap = new ConcurrentHashMap<>();
        redisClientAuthenticationTimes = new ConcurrentHashMap<>();
    }

    /**
     * CredentialsResolver implementation for MSI authentication with Redisson 3.52+.
     * Dynamically fetches fresh MSI tokens for Redis authentication on each connection.
     * This enables automatic token refresh without manual client recreation.
     */
    private class RedissonMsiCredentialsResolver implements CredentialsResolver {
        private final String objectId;

        /**
         * Constructor for RedissonMsiCredentialsResolver.
         * @param objectIdParam the object ID for MSI authentication
         */
        RedissonMsiCredentialsResolver(final String objectIdParam) {
            this.objectId = objectIdParam;
        }

        @Override
        public CompletionStage<Credentials> resolve(final InetSocketAddress address) {
            final ICoreLogger logger = CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME);

            try {
                final String token = getTokenFromMsi(objectId);
                if (token != null) {
                    logger.info("RedissonMsiCredentialsResolver providing fresh credentials for address: {}", address);
                    return CompletableFuture.completedFuture(new Credentials(objectId, token));
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
     * Get redis client.
     * @param keyClass Class type for key
     * @param valueClass Class type for value
     * @param redisConfiguration configuration for redis client
     * @return Redis client
     */
    @Override
    public IRedisCache<K, V> getClient(final Class<K> keyClass, final Class<V> valueClass, final RedisAzureConfiguration redisConfiguration) {
        final ICoreLogger logger = CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME);
        String cacheKey = String.format("%s-%s", keyClass.toString(), valueClass.toString());

        // Check if we need to re-authenticate due to MSI token expiration
        final String objectId = redisConfiguration.getObjectId();
        final boolean useMsiAuth = canUseMsiAuthentication(objectId);

        if (useMsiAuth && needsRefresh(cacheKey)) {
            IRedisCache<K, V> existingClient = this.redisClientMap.get(cacheKey);
            if (existingClient != null) {
                logger.info("Redis client last authentication done more than {} minutes ago, re-authenticating with fresh MSI token.", TOKEN_EXPIRY_THRESHOLD.toMinutes());
                final String token = getTokenFromMsi(objectId);
                if (token != null) {
                    boolean authSuccess = existingClient.authenticate(objectId, token);
                    if (authSuccess) {
                        // Update the authentication time to reflect successful re-authentication
                        redisClientAuthenticationTimes.put(cacheKey, Instant.now());
                        logger.info("Redis client successfully re-authenticated with fresh MSI token.");
                        return existingClient;
                    } else {
                        logger.warn("Redis client re-authentication failed. Removing cached client to force recreation.");
                    }
                } else {
                    logger.warn("Failed to obtain fresh MSI credentials. Removing cached client to force recreation.");
                }

                this.redisClientMap.remove(cacheKey);
                this.redisClientAuthenticationTimes.remove(cacheKey);
            }
        }
        if (this.redisClientMap.containsKey(cacheKey)) {
            IRedisCache<K, V> cacheObject = this.redisClientMap.get(cacheKey);
            if (cacheObject instanceof RedisCache) {
                return cacheObject;
            } else {
                // No Op cache it is. Re-try to initialize cache.
                logger.info("The client is of NoOpRedisCache. Re-checking if Redis is available.");
            }
        }

        IRedisCache<K, V> redisCache = this.redisClientMap.computeIfAbsent(cacheKey, key -> {
            IRedisCache<K, V> client = createRedisClient(keyClass, valueClass, redisConfiguration);
            if (client != null && useMsiAuth) {
                redisClientAuthenticationTimes.put(key, Instant.now());
            }
            return client;
        });
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
     * @return redis client
     */
    private IRedisCache<K, V> createRedisClient(final Class<K> keyClass, final Class<V> valueClass, final RedisAzureConfiguration redisConfiguration) {
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

        String username = null;
        String password = null;

        final String objectId = redisConfiguration.getObjectId();
        if (canUseMsiAuthentication(objectId)) {
            final String token = getTokenFromMsi(objectId);
            if (token != null) {
                username = objectId; // Use object ID as username
                password = token; // Use access token as password
                logger.info("Using MSI authentication for Redis client");
            } else {
                logger.warn("Unable to get MSI token, falling back to password authentication.");
            }
        } else {
            if (msiConfiguration.getIsEnabled()) {
                logger.info("MSI is enabled but no valid object ID provided, falling back to password authentication.");
            }
        }

        // Only fetch password from Key Vault if we're using password authentication (username is null)
        if (username == null) {
            password = getSecret(redisConfiguration.getPasswordKey());
            if (password == null) {
                logger.warn("Redis password does not exist. Redis is not available yet.");
                return null;
            }
        }

        return new RedisCache<K, V>(
            host,
            redisConfiguration.getPort(),
            username, // null for password auth, objectId for MSI auth
            password, // password from Key Vault or access token from MSI
            redisConfiguration.getExpiration(),
            redisConfiguration.getCommandTimeout(),
            redisConfiguration.getDatabase(),
            true, // sslEnabled
            options,
            keyClass,
            valueClass);
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

        final String objectId = redisAzureConfiguration.getObjectId();
        if (canUseMsiAuthentication(objectId)) {
            // Use CredentialsResolver for dynamic MSI token refresh - this is the proper way in Redisson 3.52+
            serverConfig.setCredentialsResolver(new RedissonMsiCredentialsResolver(objectId));
            logger.info("Configured Redisson client with RedissonMsiCredentialsResolver for dynamic token refresh");
        } else {
            if (msiConfiguration.getIsEnabled()) {
                logger.info("MSI is enabled but no valid object ID provided, using password authentication for Redisson client.");
            }
            // Static password authentication for non-MSI scenarios or MSI without valid object ID
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
     * @param objectId the object ID from configuration
     * @return true if MSI is enabled and object ID is valid
     */
    private boolean canUseMsiAuthentication(final String objectId) {
        return msiConfiguration.getIsEnabled() && objectId != null && !objectId.isBlank();
    }

    /**
     * Get MSI authentication token for Redis.
     * Validates the object ID is configured and returns a fresh access token.
     * @param objectId the object ID for MSI authentication
     * @return access token string or null if authentication fails
     */
    private String getTokenFromMsi(final String objectId) {
        final ICoreLogger logger = CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME);
        try {
            if (objectId == null || objectId.isBlank()) {
                logger.info("Redis object ID is not configured, can't get token.");
                return null;
            }

            TokenRequestContext tokenRequestContext = new TokenRequestContext();
            tokenRequestContext.addScopes(AZURE_REDIS_SCOPE);

            AccessToken accessToken = defaultAzureCredential.getToken(tokenRequestContext).block();
            final String token = accessToken == null ? null : accessToken.getToken();
            if (token == null || token.isBlank()) {
                logger.warn("Failed to acquire access token from MSI");
                return null;
            }

            logger.info("Successfully obtained access token for Redis MSI authentication");
            return token;

        } catch (Exception e) {
            logger.warn("Failed to authenticate with MSI", e);
            return null;
        }
    }

    /**
     * Check if a Redis client needs refresh due to token expiration.
     * @param cacheKey the cache key for the Redis client
     * @return true if the client needs refresh, false otherwise
     */
    private boolean needsRefresh(final String cacheKey) {
        Instant authenticationTime = redisClientAuthenticationTimes.get(cacheKey);
        if (authenticationTime == null) {
            return false; // No authentication time tracked, let normal flow handle it
        }
        return Instant.now().isAfter(authenticationTime.plus(TOKEN_EXPIRY_THRESHOLD));
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
