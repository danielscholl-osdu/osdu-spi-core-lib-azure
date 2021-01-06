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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.cache.EventGridTopicClientCache;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.core.common.partition.Property;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventGridTopicClientFactoryImplTest {

    private static final String VALID_TOPIC_NAME = "RecordsChanged";
    private static final String VALID_DATA_PARTIION_ID = "validDataPartitionId";

    @Mock
    private PartitionServiceClient partitionService;

    @InjectMocks
    private EventGridTopicClientFactoryImpl sut;

    @Mock
    private EventGridTopicClientCache clientCache;

    @Test
    public void should_throwException_given_nullDataPartitionId() {

        NullPointerException nullPointerException = Assertions.assertThrows(NullPointerException.class,
                () -> this.sut.getClient(null, TopicName.RECORDS_CHANGED));
        assertEquals("dataPartitionId cannot be null!", nullPointerException.getMessage());
    }

    @Test
    public void should_throwException_given_emptyDataPartitionId() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(IllegalArgumentException.class,
                () -> this.sut.getClient("", TopicName.RECORDS_CHANGED));
        assertEquals("dataPartitionId cannot be empty!", illegalArgumentException.getMessage());
    }

    @Test
    public void should_throwException_given_nullTopicName() {

        NullPointerException nullPointerException = Assertions.assertThrows(NullPointerException.class,
                () -> this.sut.getClient(VALID_DATA_PARTIION_ID, null));
        assertEquals("topicName cannot be null!", nullPointerException.getMessage());
    }

    @Test
    public void should_return_validClient_given_validPartitionId() {
        // Setup
        when(this.partitionService.getPartition(VALID_DATA_PARTIION_ID)).thenReturn(
                PartitionInfoAzure.builder()
                        .idConfig(Property.builder().value(VALID_DATA_PARTIION_ID).build())
                        .eventGridRecordsTopicAccessKeyConfig(Property.builder().value(VALID_TOPIC_NAME).build()).build());

        when(this.clientCache.containsKey(any())).thenReturn(false);

        // Act
        EventGridClient eventGridClient = this.sut.getClient(VALID_DATA_PARTIION_ID, TopicName.RECORDS_CHANGED);

        // Assert
        assertNotNull(eventGridClient);
        verify(this.clientCache, times(1)).put(any(), any());
    }
}