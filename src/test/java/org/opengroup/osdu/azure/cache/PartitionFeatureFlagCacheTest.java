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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.core.common.partition.PartitionInfo;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class PartitionFeatureFlagCacheTest {

    private static final int DEFAULT_TTL_SECONDS = 600;

    private PartitionFeatureFlagCache config;

    @BeforeEach
    void setUp() {
        config = new PartitionFeatureFlagCache();
        // Sensible defaults matching the @Value fallbacks; individual tests override as needed.
        ReflectionTestUtils.setField(config, "redisHostname", "");
        ReflectionTestUtils.setField(config, "redisPort", "6380");
        ReflectionTestUtils.setField(config, "redisConnectionTimeout", 15L);
        ReflectionTestUtils.setField(config, "redisCommandTimeout", 1);
        ReflectionTestUtils.setField(config, "redisPrincipalId", "");
        ReflectionTestUtils.setField(config, "database", 0);
        ReflectionTestUtils.setField(config, "ffExpirationSeconds", DEFAULT_TTL_SECONDS);
    }

    @Test
    void inMemoryFallback_returnsUsableVmCache_withConfiguredTtl() {
        ReflectionTestUtils.setField(config, "ffExpirationSeconds", 300);

        ICache<String, PartitionInfo> cache = config.partitionFeatureFlagInMemoryCache();

        assertNotNull(cache);
        assertInstanceOf(VmCache.class, cache);
        PartitionInfo info = new PartitionInfo();
        cache.put("opendes", info);
        assertSame(info, cache.get("opendes"));
    }

    @Test
    void inMemoryFallback_returnsVmCache_whenTtlIsZero() {
        ReflectionTestUtils.setField(config, "ffExpirationSeconds", 0);

        ICache<String, PartitionInfo> cache = config.partitionFeatureFlagInMemoryCache();

        assertInstanceOf(VmCache.class, cache);
    }

    @Test
    void inMemoryFallback_returnsVmCache_whenTtlIsNegative() {
        ReflectionTestUtils.setField(config, "ffExpirationSeconds", -5);

        ICache<String, PartitionInfo> cache = config.partitionFeatureFlagInMemoryCache();

        assertInstanceOf(VmCache.class, cache);
    }

    @Test
    void redisBean_constructsCache_andKeepsValidTtl() {
        ReflectionTestUtils.setField(config, "ffExpirationSeconds", 300);

        RedisAzureCache<PartitionInfo> cache = config.partitionFeatureFlagCache();

        assertNotNull(cache);
        assertEquals(300, ReflectionTestUtils.getField(config, "ffExpirationSeconds"));
    }

    @Test
    void redisBean_resetsTtlToDefault_whenNonPositive() {
        ReflectionTestUtils.setField(config, "ffExpirationSeconds", 0);

        RedisAzureCache<PartitionInfo> cache = config.partitionFeatureFlagCache();

        assertNotNull(cache);
        assertEquals(DEFAULT_TTL_SECONDS, ReflectionTestUtils.getField(config, "ffExpirationSeconds"));
    }
}
