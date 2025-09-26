package org.opengroup.osdu.azure.cosmosdb.cache.sync;

import com.azure.cosmos.CosmosClient;
import com.google.common.cache.RemovalNotification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CosmosClientRemovalListenerTest {

    @Mock
    private RemovalNotification<String, CosmosClient> notification;
    @Mock
    private CosmosClient cosmosClient;

    private final CosmosClientRemovalListener removalListener = new CosmosClientRemovalListener();

    @Test
    public void shouldPass_onRemoval_whenCosmosClientPresented() {
        when(notification.getValue()).thenReturn(cosmosClient);

        removalListener.onRemoval(notification);

        verify(cosmosClient, only()).close();
    }

    @Test
    public void shouldPass_onRemoval_whenCosmosClientIsNull() {
        when(notification.getValue()).thenReturn(null);

        removalListener.onRemoval(notification);

        verifyNoInteractions(cosmosClient);
    }

    @Test
    public void shouldPass_onRemoval_whenCosmosClientCloseThrowsException() {
        when(notification.getValue()).thenReturn(cosmosClient);
        doThrow(new RuntimeException()).when(cosmosClient).close();

        removalListener.onRemoval(notification);

        verify(cosmosClient, only()).close();
    }
}