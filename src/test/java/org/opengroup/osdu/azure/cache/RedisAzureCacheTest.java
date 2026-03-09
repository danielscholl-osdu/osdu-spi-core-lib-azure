/*
 * Copyright © Microsoft Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.azure.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.di.RedisAzureConfiguration;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.reset;

@ExtendWith(MockitoExtension.class)
class RedisAzureCacheTest {

    @Mock
    private RedisCache<String, String> mockRedisClient;

    @Mock
    private RedissonClient mockRedissonClient;

    @Mock
    private IRedisClientFactory<String> mockRedisClientFactory;

    @Mock
    private RLock mockRLock;

    private RedisAzureConfiguration azureManagedRedisConfig;
    private RedisAzureConfiguration azureCacheForRedisConfig;

    @BeforeEach
    void setUp() {
        // Azure Managed Redis configuration (port 10000, requires key prefixing)
        // Uses direct hostname with MSI authentication - no Key Vault lookups, no passwords
        azureManagedRedisConfig = createAzureManagedRedisConfig(5);

        // Azure Cache for Redis configuration (port 6380, no key prefixing)
        azureCacheForRedisConfig = new RedisAzureConfiguration(
                7,      // database
                3600,   // expiration
                6380,   // port - Azure Cache for Redis
                10L,    // connectionTimeout
                5,      // commandTimeout
                "testHostKey",
                "testPasswordKey",
                null,   // principalId
                null    // hostname
        );
    }

    @AfterEach
    void tearDown() {
        reset(mockRedisClient, mockRedissonClient, mockRedisClientFactory);
    }

    @Test
    void should_apply_key_prefix_for_azure_managed_redis() {
        // Given: Azure Managed Redis (port 10000) with database 5
        RedisAzureCache<String> cache = new RedisAzureCache<>(String.class, azureManagedRedisConfig);
        ReflectionTestUtils.setField(cache, "redisClientFactory", mockRedisClientFactory);
        when(mockRedisClientFactory.getClient(any(), any(), any())).thenReturn(mockRedisClient);

        // When: Put operation is called
        cache.put("myKey", "myValue");

        // Then: Key is prefixed with "db5:"
        verify(mockRedisClient).put("db5:myKey", "myValue");
    }

    @Test
    void should_not_apply_key_prefix_for_azure_cache_for_redis() {
        // Given: Azure Cache for Redis (port 6380)
        RedisAzureCache<String> cache = new RedisAzureCache<>(String.class, azureCacheForRedisConfig);
        ReflectionTestUtils.setField(cache, "redisClientFactory", mockRedisClientFactory);
        when(mockRedisClientFactory.getClient(any(), any(), any())).thenReturn(mockRedisClient);

        // When: Put operation is called
        cache.put("myKey", "myValue");

        // Then: Key is NOT prefixed
        verify(mockRedisClient).put("myKey", "myValue");
    }

    @ParameterizedTest
    @CsvSource({
        "0, db0:",
        "1, db1:",
        "5, db5:"
    })
    void should_use_correct_database_prefix_for_azure_managed_redis(int database, String expectedPrefix) {
        // Given: Azure Managed Redis with specific database number
        RedisAzureConfiguration config = createAzureManagedRedisConfig(database);
        RedisAzureCache<String> cache = new RedisAzureCache<>(String.class, config);
        ReflectionTestUtils.setField(cache, "redisClientFactory", mockRedisClientFactory);
        when(mockRedisClientFactory.getClient(any(), any(), any())).thenReturn(mockRedisClient);

        // When: Put operation is called
        cache.put("testKey", "testValue");

        // Then: Correct database prefix is applied
        verify(mockRedisClient).put(expectedPrefix + "testKey", "testValue");
    }

    @Test
    void should_verify_all_cache_operations_use_prefixed_keys() {
        // This test ensures comprehensive coverage of all cache operations
        RedisAzureCache<String> cache = new RedisAzureCache<>(String.class, azureManagedRedisConfig);
        ReflectionTestUtils.setField(cache, "redisClientFactory", mockRedisClientFactory);
        ReflectionTestUtils.setField(cache, "applicationName", "testApp");
        when(mockRedisClientFactory.getClient(any(), any(), any())).thenReturn(mockRedisClient);
        when(mockRedisClientFactory.getRedissonClient(anyString(), any())).thenReturn(mockRedissonClient);
        when(mockRedissonClient.getLock(anyString())).thenReturn(mockRLock);

        // Execute all cache operations
        cache.put("key1", "value1");
        cache.get("key2");
        cache.delete("key3");
        cache.updateTtl("key4", 3600);
        cache.getTtl("key5");
        cache.increment("key6");
        cache.incrementBy("key7", 10L);
        cache.decrement("key8");
        cache.decrementBy("key9", 5L);
        cache.getLock("key10");

        // Verify all operations used prefixed keys
        verify(mockRedisClient).put("db5:key1", "value1");
        verify(mockRedisClient).get("db5:key2");
        verify(mockRedisClient).delete("db5:key3");
        verify(mockRedisClient).updateTtl("db5:key4", 3600);
        verify(mockRedisClient).getTtl("db5:key5");
        verify(mockRedisClient).increment("db5:key6");
        verify(mockRedisClient).incrementBy("db5:key7", 10L);
        verify(mockRedisClient).decrement("db5:key8");
        verify(mockRedisClient).decrementBy("db5:key9", 5L);
        verify(mockRedissonClient).getLock("db5:key10");
    }

    /**
     * Helper method to create Azure Managed Redis configuration with MSI and direct hostname.
     * AMR uses direct hostname + MSI authentication (no Key Vault, no passwords).
     */
    private RedisAzureConfiguration createAzureManagedRedisConfig(int database) {
        return new RedisAzureConfiguration(
                database,   // database
                3600,       // expiration
                10000,      // port - Azure Managed Redis
                10L,        // connectionTimeout
                5,          // commandTimeout
                null,       // hostKey - not used with direct hostname
                null,       // passwordKey - not used with MSI
                "test-principal-id",
                "redis-amr.example.com"  // direct hostname for AMR
        );
    }
}
