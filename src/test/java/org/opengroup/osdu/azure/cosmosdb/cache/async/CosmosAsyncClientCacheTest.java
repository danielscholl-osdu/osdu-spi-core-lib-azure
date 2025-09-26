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
