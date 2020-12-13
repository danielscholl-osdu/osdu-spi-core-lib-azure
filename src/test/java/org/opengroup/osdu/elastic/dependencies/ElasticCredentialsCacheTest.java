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

package org.opengroup.osdu.elastic.dependencies;

import org.junit.jupiter.api.Test;
import org.opengroup.osdu.azure.cache.ElasticCredentialsCache;
import org.opengroup.osdu.azure.cache.ElasticCredentialsCacheImpl;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ElasticCredentialsCacheTest {
    private ElasticCredentialsCache cache = new ElasticCredentialsCacheImpl(10000, 10000);

    @Test
    void put_thenGet_works() {
        ClusterSettings s = ClusterSettings.builder().build();
        cache.put("key", s);
        assertEquals(s, cache.get("key"), "Caching did not work");
    }

    @Test
    void put_thenDelete_works() {
        ClusterSettings s = ClusterSettings.builder().build();
        cache.put("key", s);
        cache.delete("key");
        assertNull(cache.get("key"), "Cache delete did not work");
    }

    @Test
    void put_thenClear_works() {
        ClusterSettings s = ClusterSettings.builder().build();
        cache.put("key-1", s);
        cache.put("key-2", s);
        cache.clearAll();
        assertNull(cache.get("key-1"), "Cache clear did not work");
        assertNull(cache.get("key-2"), "Cache clear did not work");
    }
}
