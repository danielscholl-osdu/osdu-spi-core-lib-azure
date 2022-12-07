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

package org.opengroup.osdu.azure.publisherFacade;

import com.google.common.base.Preconditions;
import org.opengroup.osdu.core.common.model.http.CollaborationContext;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Implementation of message publisher.
 */
@Component
@ConditionalOnProperty(value = "azure.pubsub.publish", havingValue = "true", matchIfMissing = false)
public class MessagePublisher {
    @Autowired
    private EventGridPublisher eventGridPublisher;
    @Autowired
    private ServiceBusPublisher serviceBusPublisher;
    @Autowired
    private PubsubConfiguration pubsubConfiguration;

    /**
     * @param publisherInfo        Contains publisher data and info
     * @param headers              DpsHeaders
     * @param collaborationContext CollaborationContext
     */
    public void publishMessage(final DpsHeaders headers, final PublisherInfo publisherInfo, final Optional<CollaborationContext> collaborationContext) {
        Preconditions.checkNotNull(publisherInfo.getBatch());
        if (Boolean.parseBoolean(pubsubConfiguration.getIsServiceBusEnabled())) {
            serviceBusPublisher.publishToServiceBus(headers, publisherInfo, collaborationContext);
        }
        if (Boolean.parseBoolean(pubsubConfiguration.getIsEventGridEnabled())) {
            eventGridPublisher.publishToEventGrid(headers, publisherInfo);
        }
    }
}
