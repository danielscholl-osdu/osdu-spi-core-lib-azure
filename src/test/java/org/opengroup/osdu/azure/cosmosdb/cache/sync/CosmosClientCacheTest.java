package org.opengroup.osdu.azure.cosmosdb.cache.sync;

import com.azure.cosmos.CosmosClient;
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
public class CosmosClientCacheTest {

    @Mock
    private VmCache<String, CosmosClient> vmCacheMock;
    @Mock
    private RemovalListener removalListener;
    @Mock
    private CosmosClient client;

    private CosmosClientCache cache;

    @BeforeEach
    void setUp() {
        cache = new CosmosClientCache(removalListener, 1,1);
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

        CosmosClient result = cache.get("key1");

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

        CosmosClient result = cache.computeIfAbsent("key1", k -> mock(CosmosClient.class));

        assertSame(result, client);
        verify(vmCacheMock).computeIfAbsent(eq("key1"), any(Function.class));
    }
}
