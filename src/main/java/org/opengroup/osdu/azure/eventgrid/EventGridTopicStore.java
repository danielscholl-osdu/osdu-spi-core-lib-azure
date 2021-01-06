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
import com.microsoft.azure.eventgrid.models.EventGridEvent;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.core.common.logging.ILogger;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * A simpler interface to interact with Azure Event Grid Topic.
 * Usage examples:
 * <pre>
 * {@code
 *      @Autowired
 *      private EventGridTopicStore eventGridTopicStore;
 *
 *      String publishToEventGridTopic()
 *      {
 *          List<EventGridEvent> eventsList = new ArrayList<>();
 *          eventsList.add(new EventGridEvent(
 *                     UUID.randomUUID().toString(),
 *                     "subject",
 *                     "data",
 *                     "event type",
 *                     DateTime.now(),
 *                     "0.1"
 *            ));
 *
 *          eventGridTopicStore.publishToEventGridTopic("dataPartitionId", "topicName", eventList);
 *      }
 * }
 * </pre>
 */
@Component
public class EventGridTopicStore {

    @Autowired
    private IEventGridTopicClientFactory eventGridTopicClientFactory;

    @Autowired
    private ILogger logger;

    @Autowired
    private PartitionServiceClient partitionService;

    /**
     * @param dataPartitionId Data partition id
     * @param topicName       Topic name
     * @param eventsList      List of Event Grid Events
     */
    public void publishToEventGridTopic(final String dataPartitionId, final TopicName topicName, final List<EventGridEvent> eventsList) {
        PartitionInfoAzure pi = this.partitionService.getPartition(dataPartitionId);

        String endpoint = "";
        if (topicName == TopicName.RECORDS_CHANGED) {
            try {
                endpoint = String.format("https://%s/", new URI(pi.getEventGridRecordsTopicEndpoint()).getHost());
            } catch (URISyntaxException e) {
                throw new AppException(500, "Invalid Event Grid endpoint URI", "PartitionInfo for eventgrid-recordstopic " + pi.getEventGridRecordsTopicEndpoint(), e);
            }
        }

        EventGridClient eventGridClient = eventGridTopicClientFactory.getClient(dataPartitionId, topicName);
        eventGridClient.publishEvents(endpoint, eventsList);
    }
}
