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
public class CosmosAsyncClientRemovalListenerTest {

    @Mock
    private RemovalNotification<String, CosmosAsyncClient> notification;
    @Mock
    private CosmosAsyncClient cosmosAsyncClient;

    private final CosmosAsyncClientRemovalListener removalListener = new CosmosAsyncClientRemovalListener();

    @Test
    public void shouldPass_onRemoval_whenCosmosAsyncClientPresented() {
        when(notification.getValue()).thenReturn(cosmosAsyncClient);

        removalListener.onRemoval(notification);

        verify(cosmosAsyncClient, only()).close();
    }

    @Test
    public void shouldPass_onRemoval_whenCosmosAsyncClientIsNull() {
        when(notification.getValue()).thenReturn(null);

        removalListener.onRemoval(notification);

        verifyNoInteractions(cosmosAsyncClient);
    }

    @Test
    public void shouldPass_onRemoval_whenCosmosAsyncClientCloseThrowsException() {
        when(notification.getValue()).thenReturn(cosmosAsyncClient);
        doThrow(new RuntimeException()).when(cosmosAsyncClient).close();

        removalListener.onRemoval(notification);

        verify(cosmosAsyncClient, only()).close();
    }
}
