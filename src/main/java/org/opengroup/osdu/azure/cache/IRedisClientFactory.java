// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.azure.cache;

import io.lettuce.core.codec.RedisCodec;
import org.opengroup.osdu.azure.di.RedisAzureConfiguration;
import org.opengroup.osdu.core.common.cache.IRedisCache;
import org.redisson.api.RedissonClient;
import org.springframework.lang.Nullable;

/**
 * Redis client creator.
 * @param <V>
 */
public interface IRedisClientFactory<V> {

    /**
     * Retrieve redis client with optional custom codec. Creates a new client if one does not already exist.
     * @param valueClass value class type
     * @param redisConfiguration configuration for redis client
     * @param codec custom codec for encoding/decoding keys and values (null for default GZIP-compressed JSON)
     * @return redis cache client instance.
     */
    IRedisCache<String, V> getClient(Class<V> valueClass, RedisAzureConfiguration redisConfiguration, @Nullable RedisCodec<String, V> codec);

    /**
     * Create RedissonClient instance assuming redis-host and redis-password already exists.
     * @param applicationName application name.
     * @param redisAzureConfiguration configuration for redis client.
     * @return Redisson client instance
     */
    RedissonClient getRedissonClient(String applicationName, RedisAzureConfiguration redisAzureConfiguration);
}
