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
import org.opengroup.osdu.azure.cosmosdb.cache.AbstractCosmosClientCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Implementation of the AbstractCosmosClientCache for CosmosAsyncClient type.
 */
@Component
public class CosmosAsyncClientCache extends AbstractCosmosClientCache<String, CosmosAsyncClient> {

  /**
   * Constructor of the class.
   * @param removalListener required to initialize superclass
   * @param expirationTimeInSeconds cache expiration time in seconds
   * @param maximumCacheSize maximum cache size
   */
  public CosmosAsyncClientCache(final RemovalListener<String, CosmosAsyncClient> removalListener,
                                @Value("${cosmos.cache.expirationTimeInSeconds:1800}") final Integer expirationTimeInSeconds,
                                @Value("${cosmos.cache.maximumSize:1000}") final Integer maximumCacheSize) {
    super(removalListener, expirationTimeInSeconds, maximumCacheSize);
  }
}