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
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.core.common.partition.PartitionInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>{@code PartitionFeatureFlagImpl} injects its {@code ICache<String, PartitionInfo>} by type and
 * requires the bean, so under the {@code dataPartition} strategy this configuration always registers
 * exactly one: a Redis-backed cache when caching is enabled and Redis is configured, otherwise an
 * in-memory {@link VmCache} fallback. Both are fail-open at call time (read/write errors are
 * swallowed by the caller), so a Redis outage degrades to direct Partition Service calls without
 * breaking startup.</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "featureFlag", name = "strategy", havingValue = "dataPartition")
public class PartitionFeatureFlagCache {
    private static final ICoreLogger LOGGER =
        CoreLoggerFactory.getInstance().getLogger(PartitionFeatureFlagCache.class.getName());

    private static final int DEFAULT_CACHE_TTL_SECONDS = 600;
    private static final int DEFAULT_CACHE_MAX_SIZE = 2000;

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
     * Preferred bean: created when caching is enabled AND the Redis connection is configured.
     * When it is not registered the in-memory {@link #partitionFeatureFlagInMemoryCache()} fallback
     * takes over so the mandatory injection in {@code PartitionFeatureFlagImpl} is always satisfied.
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

    /**
     * Fallback registered only when the Redis-backed bean is absent (caching disabled or Redis not
     * configured), guaranteeing exactly one {@code ICache<String, PartitionInfo>} for the mandatory
     * by-type injection in {@code PartitionFeatureFlagImpl}.
     *
     * @return in-memory cache of {@link PartitionInfo} keyed by data-partition id
     */
    @Bean("PartitionFeatureFlagCache")
    @ConditionalOnMissingBean(name = "PartitionFeatureFlagCache")
    public ICache<String, PartitionInfo> partitionFeatureFlagInMemoryCache() {
        int ttl = ffExpirationSeconds > 0 ? ffExpirationSeconds : DEFAULT_CACHE_TTL_SECONDS;
        LOGGER.info("Redis-backed PartitionFeatureFlagCache not registered; using in-memory VmCache fallback (ttl={}s).", ttl);
        return new VmCache<>(ttl, DEFAULT_CACHE_MAX_SIZE);
    }
}