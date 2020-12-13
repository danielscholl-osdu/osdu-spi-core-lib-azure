//  Copyright © Microsoft Corporation
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.opengroup.osdu.azure.cache;

import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticCredentialsCache;

/**
 * abstract class for Elastic credential cache.
 */
public abstract class ElasticCredentialsCache implements IElasticCredentialsCache<String, ClusterSettings> {

  /**
   * @param key cache key
   * @return true if found in cache
   */
  public boolean containsKey(final String key) {
    return get(key) != null;
  }

  /**
   * @param partitionId the tenant for which the request should be cached for.
   * @return cache key for the tenant.
   */
  public String getCacheKey(final String partitionId) {
    return String.format("%s-clusterSettings", partitionId);
  }
}

