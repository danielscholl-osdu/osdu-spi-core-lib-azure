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
package org.opengroup.osdu.azure.servicebus;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.microsoft.azure.servicebus.ExceptionPhase;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.SubscriptionClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AbstractMessageHandlerTest {

    @Mock
    private SubscriptionClient subscriptionClient;

    @Mock
    private IMessage message;

    private TestMessageHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TestMessageHandler("test-worker", subscriptionClient);
    }

    @Test
    public void onMessageAsync_shouldCompleteMessage_whenProcessingSucceeds() throws Exception {
        when(message.getMessageId()).thenReturn("msg-1");
        when(message.getLockToken()).thenReturn(UUID.randomUUID());
        when(subscriptionClient.completeAsync(any(UUID.class))).thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<Void> result = handler.onMessageAsync(message);

        assertNotNull(result);
        verify(subscriptionClient).completeAsync(any(UUID.class));
    }

    @Test
    public void onMessageAsync_shouldAbandonMessage_whenProcessingFails_andLockNotExpired() throws Exception {
        handler.setShouldThrow(true);
        when(message.getMessageId()).thenReturn("msg-2");
        when(message.getLockToken()).thenReturn(UUID.randomUUID());
        when(message.getExpiresAtUtc()).thenReturn(Instant.now().plusSeconds(300));
        when(subscriptionClient.abandonAsync(any(UUID.class))).thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<Void> result = handler.onMessageAsync(message);

        assertNotNull(result);
        verify(subscriptionClient).abandonAsync(any(UUID.class));
    }

    @Test
    public void onMessageAsync_shouldReturnNull_whenProcessingFails_andLockExpired() throws Exception {
        handler.setShouldThrow(true);
        when(message.getMessageId()).thenReturn("msg-3");
        when(message.getExpiresAtUtc()).thenReturn(Instant.now().minusSeconds(300));

        CompletableFuture<Void> result = handler.onMessageAsync(message);

        assertNull(result);
    }

    @Test
    public void notifyException_shouldNotThrow() {
        // notifyException logs the error — verify it completes without exception
        handler.notifyException(new RuntimeException("test error"), ExceptionPhase.RECEIVE);
        assertNotNull(handler);
    }

    /**
     * Concrete test implementation of AbstractMessageHandler.
     */
    private static class TestMessageHandler extends AbstractMessageHandler {
        private boolean shouldThrow = false;

        TestMessageHandler(String workerName, SubscriptionClient client) {
            super(workerName, client);
        }

        void setShouldThrow(boolean shouldThrow) {
            this.shouldThrow = shouldThrow;
        }

        @Override
        public void processMessage(IMessage message) throws Exception {
            if (shouldThrow) {
                throw new RuntimeException("Processing failed");
            }
        }
    }
}
