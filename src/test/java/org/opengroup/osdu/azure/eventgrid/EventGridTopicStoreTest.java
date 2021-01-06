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

package org.opengroup.osdu.azure.eventgrid;

import com.microsoft.azure.eventgrid.EventGridClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.core.common.logging.ILogger;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.partition.Property;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
class EventGridTopicStoreTest {

    @Mock
    EventGridTopicClientFactoryImpl eventGridTopicClientFactory;

    @Mock
    EventGridClient eventGridClient;

    @Mock
    private ILogger logger;

    @Mock
    private PartitionServiceClient partitionService;

    @InjectMocks
    EventGridTopicStore sut;

    private static final String VALID_DATA_PARTIION_ID = "validDataPartitionId";
    private static final String INVALID_URI = "://invalidUri";

    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @Test
    public void should_throwException_given_invalidURI() {
        when(this.partitionService.getPartition(VALID_DATA_PARTIION_ID)).thenReturn(
                PartitionInfoAzure.builder()
                        .idConfig(Property.builder().value(VALID_DATA_PARTIION_ID).build())
                        .eventGridRecordsTopicEndpointConfig(Property.builder().value(INVALID_URI).build()).build());

        AppException appException = Assertions.assertThrows(AppException.class,
                () -> this.sut.publishToEventGridTopic(VALID_DATA_PARTIION_ID, TopicName.RECORDS_CHANGED, new ArrayList<>()));
        assertEquals("PartitionInfo for eventgrid-recordstopic ://invalidUri", appException.getError().getMessage());
        verify(this.eventGridClient, times(0)).publishEvents(any(), any());
    }

    @Test
    public void should_should_invoke_publishEvents() {
        when(this.partitionService.getPartition(VALID_DATA_PARTIION_ID)).thenReturn(
                PartitionInfoAzure.builder()
                        .idConfig(Property.builder().value(VALID_DATA_PARTIION_ID).build())
                        .eventGridRecordsTopicEndpointConfig(Property.builder().value("validURL").build()).build());
        when(this.eventGridTopicClientFactory.getClient(VALID_DATA_PARTIION_ID, TopicName.RECORDS_CHANGED)).thenReturn(this.eventGridClient);

        this.sut.publishToEventGridTopic(VALID_DATA_PARTIION_ID, TopicName.RECORDS_CHANGED, new ArrayList<>());

        verify(this.eventGridClient, times(1)).publishEvents(any(), any());
    }
}