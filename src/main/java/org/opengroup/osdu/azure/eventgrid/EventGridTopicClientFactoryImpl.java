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
import com.microsoft.azure.eventgrid.TopicCredentials;
import com.microsoft.azure.eventgrid.implementation.EventGridClientImpl;
import org.opengroup.osdu.azure.cache.EventGridTopicClientCache;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.common.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for IEventGridTopicClientFactory.
 */
@Component
public class EventGridTopicClientFactoryImpl implements IEventGridTopicClientFactory {

    @Autowired
    private PartitionServiceClient partitionService;

    @Autowired
    private EventGridTopicClientCache clientCache;

    /**
     * @param dataPartitionId Data partition id
     * @param topicName       Topic Name
     * @return EventGridClient
     */
    @Override
    public EventGridClient getClient(final String dataPartitionId, final TopicName topicName) {
        Validators.checkNotNullAndNotEmpty(dataPartitionId, "dataPartitionId");
        Validators.checkNotNull(topicName, "topicName");

        String cacheKey = String.format("%s-%s", dataPartitionId, topicName);
        if (this.clientCache.containsKey(cacheKey)) {
            return this.clientCache.get(cacheKey);
        }

        PartitionInfoAzure pi = this.partitionService.getPartition(dataPartitionId);

        TopicCredentials topicCredentials = null;
        if (topicName == TopicName.RECORDS_CHANGED) {
            topicCredentials = new TopicCredentials(pi.getEventGridRecordsTopicAccessKey());
        }

        EventGridClient eventGridClient = new EventGridClientImpl(topicCredentials);
        this.clientCache.put(cacheKey, eventGridClient);
        return eventGridClient;
    }
}
