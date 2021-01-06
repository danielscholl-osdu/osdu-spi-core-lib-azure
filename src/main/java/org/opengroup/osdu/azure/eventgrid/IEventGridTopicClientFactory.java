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

/**
 * Interface for Event Grid Topic client factory to return appropriate
 * blobServiceClient based on the data partition id, and topic name.
 */
public interface IEventGridTopicClientFactory {

    /**
     * @param dataPartitionId Data partition id
     * @param topicName       Topic name
     * @return EventGridClient
     */
    EventGridClient getClient(String dataPartitionId, TopicName topicName);
}
