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

import javax.inject.Named;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Elastic credential cache used by the Azure implementation of OSDU.
 */
@Component("clusterSettingsCache")
@Lazy
@ConditionalOnProperty(value = "cache.provider", havingValue = "vm", matchIfMissing = true)
public class ElasticCredentialsCacheImpl extends ElasticCredentialsCache {

  /**
   * Underlying cache.
   */
  private VmCache<String, ClusterSettings> cache;

  /**
   * @param cacheExpirationMinutes The cache expiration time, in minutes.
   * @param maxCachedObjectEntries The max number of objects that can be in the cache.
   */
  public ElasticCredentialsCacheImpl(
      @Named("ELASTIC_CACHE_EXPIRATION") final Integer cacheExpirationMinutes,
      @Named("MAX_CACHE_VALUE_SIZE") final Integer maxCachedObjectEntries) {
    cache = new VmCache<>(cacheExpirationMinutes * 60, maxCachedObjectEntries);
  }

  /**
   * @param s Key of item to insert.
   * @param o The data to insert.
   */
  @Override
  public void put(final String s, final ClusterSettings o) {
    cache.put(s, o);
  }

  /**
   * @param s The cache key.
   * @return The data cached with that key.
   */
  @Override
  public ClusterSettings get(final String s) {
    return cache.get(s);
  }

  /**
   * Delete an item from the cache.
   *
   * @param s The key to use for the delete operation.
   */
  @Override
  public void delete(final String s) {
    cache.delete(s);
  }

  /**
   * Clear all items from the cache.
   */
  @Override
  public void clearAll() {
    cache.clearAll();
  }

}

