package org.opengroup.osdu.azure.cache;

import io.lettuce.core.codec.CompressionCodec;
import io.lettuce.core.codec.RedisCodec;
import org.opengroup.osdu.azure.di.RedisAzureConfiguration;
import org.opengroup.osdu.core.common.cache.IRedisCache;
import org.opengroup.osdu.core.common.cache.JsonCodec;
import org.redisson.api.RedissonClient;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Redis implementation for Azure.
 * <p>
 * This class delegates to a factory that creates properly configured RedisCache instances.
 * Subclasses can override getCodec() to provide a custom codec implementation.
 * </p>
 * @param <V> value class
 */
public class RedisAzureCache<V> implements IRedisCache<String, V> {

    private static final int AZURE_MANAGED_REDIS_PORT = 10000;

    private final Class<V> valueClass;
    private final RedisAzureConfiguration redisConfiguration;
    private final RedisCodec<String, V> codec;
    private final String keyPrefix;

    @Value("${spring.application.name}:corelibazure")
    private String applicationName;

    @Autowired
    private IRedisClientFactory<V> redisClientFactory;

    /**
     * Constructor.
     * @param valueClassType value class type
     * @param redisAzureConfiguration configuration for redis client
     */
    public RedisAzureCache(final Class<V> valueClassType, final RedisAzureConfiguration redisAzureConfiguration) {
        this.valueClass = valueClassType;
        this.redisConfiguration = redisAzureConfiguration;
        this.codec = getCodec(String.class, valueClassType); // can be overridden by subclasses

        // Azure Managed Redis (AMR) on port 10000 doesn't support multiple logical databases,
        // so we use a key prefix like "db7:" to simulate database isolation and avoid collisions.
        // Azure Cache for Redis on port 6380 supports multiple logical databases natively,
        // so an empty keyPrefix means no prefixing is needed.
        this.keyPrefix = redisAzureConfiguration.getPort() == AZURE_MANAGED_REDIS_PORT
            ? "db" + redisAzureConfiguration.getDatabase() + ":"
            : "";
    }

    /**
     * Put value in cache.
     * @param key key to use
     * @param value value to save
     */
    @Override
    public void put(final String key, final V value) {
        getClient().put(getPrefixedKey(key), value);
    }

    /**
     * Get value from cache.
     * @param key key
     * @return value
     */
    @Override
    public V get(final String key) {
        return getClient().get(getPrefixedKey(key));
    }

    /**
     * Delete value from cache.
     * @param key key
     */
    @Override
    public void delete(final String key) {
        getClient().delete(getPrefixedKey(key));
    }

    /**
     * Clear all entries from cache.
     * <p>
     * For Azure Managed Redis (AMR), which has a single logical database shared across apps,
     * a blanket FLUSHDB would evict keys belonging to other applications. Instead, we delete
     * only the keys that carry this app's key prefix, preserving isolation. For Azure Cache
     * for Redis, which supports multiple logical databases, the standard clearAll() is used.
     * </p>
     */
    @Override
    public void clearAll() {
        if (keyPrefix.isEmpty()) {
            getClient().clearAll();
        } else {
            // Delete only keys belonging to this app's namespace (e.g. "db7:*")
            deleteByPattern("*");
        }
    }

    /**
     * Puts entry in cache with ttl measured in milliseconds.
     * @param k key
     * @param l ttl in milliseconds
     * @param o value
     */
    @Override
    public void put(final String k, final long l, final V o) {
        getClient().put(getPrefixedKey(k), l, o);
    }

    /**
     * Updates a key's ttl in milliseconds.
     * @param k key
     * @param l ttl in milliseconds
     * @return true if updated, false otherwise
     */
    @Override
    public boolean updateTtl(final String k, final long l) {
        return getClient().updateTtl(getPrefixedKey(k), l);
    }

    /**
     * Gets the ttl for a key in milliseconds.
     * @param k key
     * @return ttl in milliseconds
     */
    @Override
    public Long getTtl(final String k) {
        return getClient().getTtl(getPrefixedKey(k));
    }

    /**
     * Get redis INFO.
     * @return info string
     */
    @Override
    public String info() {
        return getClient().info();
    }

    /**
     * Increment the integer value of a key by one.
     * @param k key
     * @return incremented value
     */
    @Override
    public Long increment(final String k) {
        return getClient().increment(getPrefixedKey(k));
    }

    /**
     * Increment the integer value of a key by the given amount.
     * @param k key
     * @param l amount to increment by
     * @return incremented value
     */
    @Override
    public Long incrementBy(final String k, final long l) {
        return getClient().incrementBy(getPrefixedKey(k), l);
    }

    /**
     * Decrement the integer value of a key by one.
     * @param k key
     * @return decremented value
     */
    @Override
    public Long decrement(final String k) {
        return getClient().decrement(getPrefixedKey(k));
    }

    /**
     * Decrement the integer value of a key by the given amount.
     * @param k key
     * @param l amount to decrement by
     * @return decremented value
     */
    @Override
    public Long decrementBy(final String k, final long l) {
        return getClient().decrementBy(getPrefixedKey(k), l);
    }

    /**
     * Get codec for performing encoding and decoding of key and values present in redis cache.
     * <p>
     * Subclasses can override this method to provide a custom codec implementation.
     * The codec is evaluated once during construction and passed to the factory.
     * </p>
     * @param classOfK key class type
     * @param classOfV value class type
     * @return RedisCodec instance to use for serialization
     */
    @Override
    public RedisCodec<String, V> getCodec(final Class<String> classOfK, final Class<V> classOfV) {
        return CompressionCodec.valueCompressor(new JsonCodec<>(classOfK, classOfV), CompressionCodec.CompressionType.GZIP);
    }

    /**
     * Authenticate against the redis cache with the given username and password.
     * @param username username
     * @param password password
     * @return true if authentication was successful, false otherwise
     */
    @Override
    public boolean authenticate(final String username, final String password) {
        return getClient().authenticate(username, password);
    }

    /**
     * Deletes all keys matching the specified pattern.
     * The pattern is automatically prefixed with the configured keyPrefix before being passed to the underlying client.
     * @param pattern Redis glob-style pattern (e.g., "cache:*", "*:session:*")
     * @return number of keys deleted, or -1 if operation failed
     */
    @Override
    public long deleteByPattern(final String pattern) {
        // Apply the keyPrefix to the pattern to ensure we only delete keys in our database namespace
        String prefixedPattern = getPrefixedKey(pattern);
        return getClient().deleteByPattern(prefixedPattern);
    }

    /**
     * Get redis lock for the given lockKey.
     * @param lockKey name of the lockKey.
     * @return lock object
     */
    public RLock getLock(final String lockKey) {
        RedissonClient redissonClient = redisClientFactory.getRedissonClient(applicationName, redisConfiguration);
        if (redissonClient == null) {
            return null;
        }

        return redissonClient.getLock(getPrefixedKey(lockKey));
    }

    /**
     * Get the underlying Redis client, lazily created by the factory.
     * @return the Redis cache client
     */
    private IRedisCache<String, V> getClient() {
        return redisClientFactory.getClient(valueClass, redisConfiguration, codec);
    }

    /**
     * Applies the key prefix to simulate database isolation.
     * @param key the original key
     * @return the prefixed key (for AMR) or the original key (for Azure Cache for Redis)
     */
    private String getPrefixedKey(final String key) {
        return keyPrefix + key;
    }
}
