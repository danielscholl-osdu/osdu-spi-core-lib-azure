package org.opengroup.osdu.azure.cache;

import io.lettuce.core.codec.RedisCodec;
import org.opengroup.osdu.azure.di.RedisAzureConfiguration;
import org.opengroup.osdu.core.common.cache.IRedisCache;
import org.redisson.api.RedissonClient;

/**
 * Redis client creator.
 * @param <K>
 * @param <V>
 */
public interface IRedisClientFactory<K, V> {

    /**
     * Retrieve redis client with optional custom codec. Creates a new client if one does not already exist.
     * @param keyClass key class type
     * @param valueClass value class type
     * @param redisConfiguration configuration for redis client
     * @param codec custom codec for encoding/decoding keys and values (null for default GZIP-compressed JSON)
     * @return redis cache client instance.
     */
    IRedisCache<K, V> getClient(Class<K> keyClass, Class<V> valueClass, RedisAzureConfiguration redisConfiguration, RedisCodec<K, V> codec);

    /**
     * Create RedissonClient instance assuming redis-host and redis-password already exists.
     * @param applicationName application name.
     * @param redisAzureConfiguration configuration for redis client.
     * @return Redisson client instance
     */
    RedissonClient getRedissonClient(String applicationName, RedisAzureConfiguration redisAzureConfiguration);
}
