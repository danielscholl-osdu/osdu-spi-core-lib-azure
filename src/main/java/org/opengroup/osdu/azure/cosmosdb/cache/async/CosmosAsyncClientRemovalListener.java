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
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Implementation for RemovalListener interface for CosmosAsyncClient, required for the CosmosAsyncClient cache.
 */
@Component
@Slf4j
public class CosmosAsyncClientRemovalListener implements RemovalListener<String, CosmosAsyncClient> {

    /**
     * Implementation for the interface's method. Closes all cosmos async clients before it will be removed from cache.
     * @param notification
     */
    @Override
    public void onRemoval(final RemovalNotification<String, CosmosAsyncClient> notification) {
        CosmosAsyncClient cosmosAsyncClient = notification.getValue();
        if (cosmosAsyncClient != null) {
            try {
                cosmosAsyncClient.close();
            } catch (Exception e) {
                log.error("Error while closing cosmos async client {}", e.getMessage(), e);
            }
        }
    }

}