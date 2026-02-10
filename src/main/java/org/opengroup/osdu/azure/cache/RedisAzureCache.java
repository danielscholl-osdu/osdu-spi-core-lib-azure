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
 * @param <K> key class
 * @param <V> value class
 */
public class RedisAzureCache<K, V> implements IRedisCache<K, V> {

    private final Class<K> keyClass;
    private final Class<V> valueClass;
    private final RedisAzureConfiguration redisConfiguration;
    private final RedisCodec<K, V> codec;

    @Value("${spring.application.name}:corelibazure")
    private String applicationName;

    @Autowired
    private IRedisClientFactory<K, V> redisClientFactory;

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
        this.codec = getCodec(keyClassType, valueClassType); // can be overridden by subclasses
    }

    /**
     * Put value in cache.
     * @param key key to use
     * @param value value to save
     */
    @Override
    public void put(final K key, final V value) {
        getClient().put(key, value);
    }

    /**
     * Get value from cache.
     * @param key key
     * @return value
     */
    @Override
    public V get(final K key) {
        return getClient().get(key);
    }

    /**
     * Delete value from cache.
     * @param key key
     */
    @Override
    public void delete(final K key) {
        getClient().delete(key);
    }

    /**
     * Clear all entries from cache.
     */
    @Override
    public void clearAll() {
        getClient().clearAll();
    }

    /**
     * Puts entry in cache with ttl measured in milliseconds.
     * @param k key
     * @param l ttl in milliseconds
     * @param o value
     */
    @Override
    public void put(final K k, final long l, final V o) {
        getClient().put(k, l, o);
    }

    /**
     * Updates a key's ttl in milliseconds.
     * @param k key
     * @param l ttl in milliseconds
     * @return true if updated, false otherwise
     */
    @Override
    public boolean updateTtl(final K k, final long l) {
        return getClient().updateTtl(k, l);
    }

    /**
     * Gets the ttl for a key in milliseconds.
     * @param k key
     * @return ttl in milliseconds
     */
    @Override
    public Long getTtl(final K k) {
        return getClient().getTtl(k);
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
    public Long increment(final K k) {
        return getClient().increment(k);
    }

    /**
     * Increment the integer value of a key by the given amount.
     * @param k key
     * @param l amount to increment by
     * @return incremented value
     */
    @Override
    public Long incrementBy(final K k, final long l) {
        return getClient().incrementBy(k, l);
    }

    /**
     * Decrement the integer value of a key by one.
     * @param k key
     * @return decremented value
     */
    @Override
    public Long decrement(final K k) {
        return getClient().decrement(k);
    }

    /**
     * Decrement the integer value of a key by the given amount.
     * @param k key
     * @param l amount to decrement by
     * @return decremented value
     */
    @Override
    public Long decrementBy(final K k, final long l) {
        return getClient().decrementBy(k, l);
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
    public RedisCodec<K, V> getCodec(final Class<K> classOfK, final Class<V> classOfV) {
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

    /**
     * Get the underlying Redis client, lazily created by the factory.
     * @return the Redis cache client
     */
    private IRedisCache<K, V> getClient() {
        return redisClientFactory.getClient(keyClass, valueClass, redisConfiguration, codec);
    }
}
