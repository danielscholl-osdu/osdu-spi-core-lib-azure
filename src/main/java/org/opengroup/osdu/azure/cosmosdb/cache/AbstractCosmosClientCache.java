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

package org.opengroup.osdu.azure.cosmosdb.cache;

import com.google.common.cache.RemovalListener;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.cache.VmCache;

import java.util.function.Function;

/**
 * Implementation of the ICache interface for CosmosClients.
 * @param <K>
 * @param <V>
 */
public abstract class AbstractCosmosClientCache<K, V>  implements ICache<K, V> {

    private VmCache<K, V> vmCache;

    /**
     * Constructor for the AbstractCosmosClientCache.
     * @param removalListener should be passed into the subclasses
     * @param expirationTimeInSeconds cache expiration time in seconds
     * @param maximumCacheSize maximum cache size
     */
    public AbstractCosmosClientCache(final RemovalListener<K, V> removalListener,
                                     final Integer expirationTimeInSeconds,
                                     final Integer maximumCacheSize) {
        this.vmCache = new VmCache<>(expirationTimeInSeconds, maximumCacheSize, removalListener);
    }

    /**
     * Put value to cache with key.
     * @param key
     * @param value
     */
    @Override
    public void put(final K key, final V value) {
        this.vmCache.put(key, value);
    }

    /**
     * Retrieve value from cache using key.
     * @param key
     * @return value
     */
    @Override
    public V get(final K key) {
        return this.vmCache.get(key);
    }

    /**
     * Delete value from cache using key.
     * @param key
     */
    @Override
    public void delete(final K key) {
        this.vmCache.delete(key);
    }

    /**
     * Clear cache.
     */
    @Override
    public void clearAll() {
        this.vmCache.clearAll();
    }

    /**
     * Atomically computes a value for the given key if absent.
     *
     * @param key the key with which the computed value is to be associated
     * @param mappingFunction the function to compute a value (must not return null)
     *
     * @return the current (existing or computed) value associated with the key
     */
    public V computeIfAbsent(final K key, final Function<? super K, ? extends V> mappingFunction) {
        return vmCache.computeIfAbsent(key, mappingFunction);
    }
}
