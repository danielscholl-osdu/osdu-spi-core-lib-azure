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
package org.opengroup.osdu.azure.publisherFacade;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.CollaborationContext;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;

@ExtendWith(MockitoExtension.class)
public class MessagePublisherTest {

    @Mock
    private EventGridPublisher eventGridPublisher;

    @Mock
    private ServiceBusPublisher serviceBusPublisher;

    @Mock
    private PubsubConfiguration pubsubConfiguration;

    @Mock
    private DpsHeaders dpsHeaders;

    @InjectMocks
    private MessagePublisher messagePublisher;

    private PublisherInfo publisherInfo;

    @BeforeEach
    void setUp() {
        publisherInfo = PublisherInfo.builder()
                .batch(new Object())
                .eventGridTopicName("test-topic")
                .build();
    }

    @Test
    public void should_publish_to_service_bus_when_enabled() {
        when(pubsubConfiguration.getIsServiceBusEnabled()).thenReturn("true");
        when(pubsubConfiguration.getIsEventGridEnabled()).thenReturn("false");

        messagePublisher.publishMessage(dpsHeaders, publisherInfo, Optional.empty());

        verify(serviceBusPublisher).publishToServiceBus(dpsHeaders, publisherInfo, Optional.empty());
        verifyNoInteractions(eventGridPublisher);
    }

    @Test
    public void should_publish_to_event_grid_when_enabled() {
        when(pubsubConfiguration.getIsServiceBusEnabled()).thenReturn("false");
        when(pubsubConfiguration.getIsEventGridEnabled()).thenReturn("true");

        messagePublisher.publishMessage(dpsHeaders, publisherInfo, Optional.empty());

        verify(eventGridPublisher).publishToEventGrid(dpsHeaders, publisherInfo);
        verifyNoInteractions(serviceBusPublisher);
    }

    @Test
    public void should_publish_to_both_when_both_enabled() {
        when(pubsubConfiguration.getIsServiceBusEnabled()).thenReturn("true");
        when(pubsubConfiguration.getIsEventGridEnabled()).thenReturn("true");

        messagePublisher.publishMessage(dpsHeaders, publisherInfo, Optional.empty());

        verify(serviceBusPublisher).publishToServiceBus(dpsHeaders, publisherInfo, Optional.empty());
        verify(eventGridPublisher).publishToEventGrid(dpsHeaders, publisherInfo);
    }

    @Test
    public void should_throw_when_batch_is_null() {
        PublisherInfo nullBatchInfo = PublisherInfo.builder().batch(null).build();
        Optional<CollaborationContext> noContext = Optional.empty();

        assertThrows(NullPointerException.class, () -> messagePublisher.publishMessage(dpsHeaders, nullBatchInfo, noContext));
    }
}
