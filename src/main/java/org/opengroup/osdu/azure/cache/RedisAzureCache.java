package org.opengroup.osdu.azure.cache;

import com.lambdaworks.redis.codec.CompressionCodec;
import com.lambdaworks.redis.codec.RedisCodec;
import org.opengroup.osdu.azure.di.RedisAzureConfiguration;
import org.opengroup.osdu.core.common.cache.JsonCodec;
import org.redisson.api.RedissonClient;
import org.redisson.api.RLock;
import org.opengroup.osdu.core.common.cache.IRedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Redis implementation for Azure.
 * @param <K> key class
 * @param <V> value class
 */
public class RedisAzureCache<K, V> implements IRedisCache<K, V> {

    // Add Exception handling
    // Add database number
    private static final String LOGGER_NAME = RedisAzureCache.class.getName();

    private Class<K> keyClass;
    private Class<V> valueClass;
    private RedisAzureConfiguration redisConfiguration;

    @Value("${spring.application.name}:corelibazure")
    private String applicationName;

    @Autowired
    private IRedisClientFactory redisClientFactory;

    /**
     * Constructor.
     * @param keyClassType key class type
     * @param valueClassType value class type
     * @param redisAzureConfiguration configuration for redis client
     */
    public RedisAzureCache(final Class<K> keyClassType, final Class<V> valueClassType, final RedisAzureConfiguration redisAzureConfiguration) {
        this.keyClass = keyClassType;
        this.valueClass = valueClassType;
        this.redisConfiguration = redisAzureConfiguration;
    }

    /**
     * Put value in cache.
     * @param key key to use
     * @param value value to save
     */
    @Override
    public void put(final K key, final V value) {
        IRedisCache<K, V> redisCache = redisClientFactory.getClient(keyClass, valueClass, redisConfiguration);
        redisCache.put(key, value);
    }

    /**
     * Get value from cache.
     * @param key key
     * @return value
     */
    @Override
    public V get(final K key) {
        IRedisCache<K, V> redisCache = redisClientFactory.getClient(keyClass, valueClass, redisConfiguration);
        return redisCache.get(key);
    }

    /**
     * Delete value from cache.
     * @param key key
     */
    @Override
    public void delete(final K key) {
        IRedisCache<K, V> redisCache = redisClientFactory.getClient(keyClass, valueClass, redisConfiguration);
        redisCache.delete(key);
    }

    /**
     *
     * Clear all entries from cache.
     */
    @Override
    public void clearAll() {
        IRedisCache<K, V> redisCache = redisClientFactory.getClient(keyClass, valueClass, redisConfiguration);
        redisCache.clearAll();
    }

    /**
     * Puts entry in cache with ttl measured in milliseconds.
     */
    @Override
    public void put(final K k, final long l, final V o) {
        IRedisCache<K, V> redisCache = redisClientFactory.getClient(keyClass, valueClass, redisConfiguration);
        redisCache.put(k, l, o);
    }

    /**
     * Updates a key's ttl in milliseconds.
     */
    @Override
    public boolean updateTtl(final K k, final long l) {
        IRedisCache<K, V> redisCache = redisClientFactory.getClient(keyClass, valueClass, redisConfiguration);
        return redisCache.updateTtl(k, l);
    }

    /**
     * Gets the ttl for a key in milliseconds.
     */
    @Override
    public long getTtl(final K k) {
        IRedisCache<K, V> redisCache = redisClientFactory.getClient(keyClass, valueClass, redisConfiguration);
        return redisCache.getTtl(k);
    }

    /**
     * Get redis INFO.
     */
    @Override
    public String info() {
        IRedisCache<K, V> redisCache = redisClientFactory.getClient(keyClass, valueClass, redisConfiguration);
        return redisCache.info();
    }

    /**
     * Increment the integer value of a key by one.
     */
    @Override
    public Long increment(final K k) {
        IRedisCache<K, V> redisCache = redisClientFactory.getClient(keyClass, valueClass, redisConfiguration);
        return redisCache.increment(k);
    }

    /**
     * Increment the integer value of a key by the given amount.
     */
    @Override
    public Long incrementBy(final K k, final long l) {
        IRedisCache<K, V> redisCache = redisClientFactory.getClient(keyClass, valueClass, redisConfiguration);
        return redisCache.incrementBy(k, l);
    }

    /**
     * Decrement the integer value of a key by one.
     */
    @Override
    public Long decrement(final K k) {
        IRedisCache<K, V> redisCache = redisClientFactory.getClient(keyClass, valueClass, redisConfiguration);
        return redisCache.decrement(k);
    }

    /**
     * Decrement the integer value of a key by the given amount.
     */
    @Override
    public Long decrementBy(final K k, final long l) {
        IRedisCache<K, V> redisCache = redisClientFactory.getClient(keyClass, valueClass, redisConfiguration);
        return redisCache.decrementBy(k, l);
    }

    /**
     * Get codec for performing encoding and decoding of key and values present in redis cache.
     */
    @Override
    public RedisCodec<K, V> getCodec(final Class<K> classOfK, final Class<V> classOfV) {
        return CompressionCodec.valueCompressor(new JsonCodec<>(classOfK, classOfV), CompressionCodec.CompressionType.GZIP);
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

        return redissonClient.getLock(lockKey);
    }
}
