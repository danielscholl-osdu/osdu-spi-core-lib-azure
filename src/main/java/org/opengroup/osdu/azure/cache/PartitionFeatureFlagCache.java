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

import org.opengroup.osdu.azure.di.RedisAzureConfiguration;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.azure.logging.ICoreLogger;
import org.opengroup.osdu.core.common.partition.PartitionInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>The cache bean is created only when caching is explicitly enabled <b>and</b> the Redis
 * connection is actually configured. The class itself is <b>fail-open</b>: every {@code redis.*}
 * field is defaulted so it is always constructed (even in a partition service with no Redis), and
 * when the Redis connection is absent the bean is simply not registered. The optional injection in
 * {@code PartitionFeatureFlagImpl} then stays {@code null} and the service falls back to making direct
 * Partition Service calls.</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "featureFlag", name = "strategy", havingValue = "dataPartition")
public class PartitionFeatureFlagCache {
    private static final ICoreLogger LOGGER =
        CoreLoggerFactory.getInstance().getLogger(PartitionFeatureFlagCache.class.getName());

    private static final int DEFAULT_CACHE_TTL_SECONDS = 600;

    @Value("${redis.hostname:}")
    private String redisHostname;
    @Value("${redis.port:6380}")
    private String redisPort;
    @Value("${redis.connection.timeout:15}")
    private long redisConnectionTimeout;
    @Value("${redis.command.timeout:1}")
    private int redisCommandTimeout;
    @Value("${redis.principal.id:}")
    private String redisPrincipalId;
    @Value("${redis.database:0}")
    private int database;

    // Feature-flag-cache-specific TTL (independent of the shared DB index above).
    @Value("${featureFlag.dataPartition.cache.ttl:600}")
    private int ffExpirationSeconds;

    /**
     * Created only when caching is enabled AND the Redis connection is configured. If either
     * condition is unmet, no bean is registered and {@code PartitionFeatureFlagImpl} falls back to
     * direct Partition Service calls.
     *
     * @return Redis-backed cache of {@link PartitionInfo} keyed by data-partition id
     */
    @Bean("PartitionFeatureFlagCache")
    @ConditionalOnExpression(
        "${featureFlag.dataPartition.cache.enabled:false} "
      + "and '${redis.hostname:}'.trim() != '' "
      + "and '${redis.port:}'.trim() matches '\\d+'")
    public RedisAzureCache<PartitionInfo> partitionFeatureFlagCache() {
        if (ffExpirationSeconds <= 0) {
            LOGGER.warn("Invalid featureFlag.dataPartition.cache.ttl={} (must be > 0). Using default ttl={}s.",
                ffExpirationSeconds, DEFAULT_CACHE_TTL_SECONDS);
            ffExpirationSeconds = DEFAULT_CACHE_TTL_SECONDS;
        }
        return new RedisAzureCache<>(PartitionInfo.class,
            new RedisAzureConfiguration(database, ffExpirationSeconds,
                Integer.parseInt(redisPort.trim()), redisConnectionTimeout, redisCommandTimeout, redisPrincipalId, redisHostname));
    }
}
