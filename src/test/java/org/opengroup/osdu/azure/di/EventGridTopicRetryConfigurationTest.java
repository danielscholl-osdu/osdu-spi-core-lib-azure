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
package org.opengroup.osdu.azure.di;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class EventGridTopicRetryConfigurationTest {

    @Test
    public void should_return_default_negative_one_when_not_configured() {
        EventGridTopicRetryConfiguration config = new EventGridTopicRetryConfiguration();

        assertEquals(-1, config.getLongRunningOperationRetryTimeout());
        assertFalse(config.isTimeoutConfigured());
    }

    @Test
    public void should_return_configured_value_when_positive() {
        EventGridTopicRetryConfiguration config = new EventGridTopicRetryConfiguration();
        config.setLongRunningOperationRetryTimeout(30);

        assertEquals(30, config.getLongRunningOperationRetryTimeout());
        assertTrue(config.isTimeoutConfigured());
    }

    @Test
    public void should_return_negative_one_when_set_to_negative() {
        EventGridTopicRetryConfiguration config = new EventGridTopicRetryConfiguration();
        config.setLongRunningOperationRetryTimeout(-5);

        assertEquals(-1, config.getLongRunningOperationRetryTimeout());
        assertFalse(config.isTimeoutConfigured());
    }

    @Test
    public void should_return_zero_when_set_to_zero() {
        EventGridTopicRetryConfiguration config = new EventGridTopicRetryConfiguration();
        config.setLongRunningOperationRetryTimeout(0);

        assertEquals(0, config.getLongRunningOperationRetryTimeout());
        assertTrue(config.isTimeoutConfigured());
    }
}
