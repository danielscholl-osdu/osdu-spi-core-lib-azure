/*
 * Copyright © Microsoft Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opengroup.osdu.azure.cache;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class CacheConstructorTest {

    @Test
    public void subscriptionClientCache_should_construct_and_containsKey_returns_false() {
        SubscriptionClientCache cache = new SubscriptionClientCache();
        assertNotNull(cache);
        assertFalse(cache.containsKey("nonexistent"));
    }

    @Test
    public void eventGridTopicClientCache_should_construct_and_containsKey_returns_false() {
        EventGridTopicClientCache cache = new EventGridTopicClientCache();
        assertNotNull(cache);
        assertFalse(cache.containsKey("nonexistent"));
    }

    @Test
    public void eventGridManagerCache_should_construct_and_containsKey_returns_false() {
        EventGridManagerCache cache = new EventGridManagerCache();
        assertNotNull(cache);
        assertFalse(cache.containsKey("nonexistent"));
    }

    @Test
    public void cryptographyClientCache_should_construct_and_containsKey_returns_false() {
        CryptographyClientCache cache = new CryptographyClientCache();
        assertNotNull(cache);
        assertFalse(cache.containsKey("nonexistent"));
    }

    @Test
    public void azureServicePrincipleTokenServiceCache_should_construct_and_containsKey_returns_false() {
        AzureServicePrincipleTokenServiceCache cache = new AzureServicePrincipleTokenServiceCache();
        assertNotNull(cache);
        assertFalse(cache.containsKey("nonexistent"));
    }

    @Test
    public void partitionServiceEventGridCache_should_construct_and_containsKey_returns_false() {
        PartitionServiceEventGridCache cache = new PartitionServiceEventGridCache();
        assertNotNull(cache);
        assertFalse(cache.containsKey("nonexistent"));
    }
}
