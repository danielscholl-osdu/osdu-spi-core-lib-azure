/*
 *  Copyright 2020-2024 Google LLC
 *  Copyright 2020-2024 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.azure.cosmosdb.cache.async;

import com.azure.cosmos.CosmosAsyncClient;
import com.google.common.cache.RemovalListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.common.TestUtils;
import org.opengroup.osdu.core.common.cache.VmCache;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CosmosAsyncClientCacheTest {
    @Mock
    private VmCache<String, CosmosAsyncClient> vmCacheMock;
    @Mock
    private RemovalListener removalListener;
    @Mock
    private CosmosAsyncClient client;

    private CosmosAsyncClientCache cache;

    @BeforeEach
    void setUp() {
        cache = new CosmosAsyncClientCache(removalListener, 1,1);
        TestUtils.setFieldValue(cache, "vmCache", vmCacheMock);
    }

    @Test
    void testPutDelegatesToVmCache() {
        cache.put("key1", client);

        verify(vmCacheMock).put("key1", client);
    }

    @Test
    void testGetDelegatesToVmCache() {
        when(vmCacheMock.get("key1")).thenReturn(client);

        CosmosAsyncClient result = cache.get("key1");

        assertSame(result, client);
        verify(vmCacheMock).get("key1");
    }

    @Test
    void testDeleteDelegatesToVmCache() {
        cache.delete("key1");

        verify(vmCacheMock).delete("key1");
    }

    @Test
    void testClearAllDelegatesToVmCache() {
        cache.clearAll();

        verify(vmCacheMock).clearAll();
    }

    @Test
    void testComputeIfAbsentDelegatesToVmCache() {
        when(vmCacheMock.computeIfAbsent(eq("key1"), any())).thenReturn(client);

        CosmosAsyncClient result = cache.computeIfAbsent("key1", k -> mock(CosmosAsyncClient.class));

        assertSame(result, client);
        verify(vmCacheMock).computeIfAbsent(eq("key1"), any(Function.class));
    }
}
