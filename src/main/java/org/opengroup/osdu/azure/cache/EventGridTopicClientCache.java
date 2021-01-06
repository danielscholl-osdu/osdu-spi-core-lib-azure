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

package org.opengroup.osdu.azure.cache;

import com.microsoft.azure.eventgrid.EventGridClient;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Implementation of ICache for TopicClient.
 */
@Component
@Lazy
public class EventGridTopicClientCache extends VmCache<String, EventGridClient> {
    /**
     *  Default cache constructor.
     */
    public EventGridTopicClientCache() {
        super(60 * 60, 1000);
    }

    /**
     * @param key cache key
     * @return true if found in cache
     */
    public boolean containsKey(final String key) {
        return this.get(key) != null;
    }
}
