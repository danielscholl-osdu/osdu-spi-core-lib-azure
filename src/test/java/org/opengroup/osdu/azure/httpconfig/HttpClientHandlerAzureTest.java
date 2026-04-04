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
package org.opengroup.osdu.azure.httpconfig;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.opengroup.osdu.azure.di.RetryAndTimeoutConfiguration;

public class HttpClientHandlerAzureTest {

    @Test
    public void should_create_handler_with_default_configuration() {
        RetryAndTimeoutConfiguration config = new RetryAndTimeoutConfiguration();
        config.setMaxRetry(3);
        config.setConnectTimeout(5000);
        config.setRequestTimeout(5000);
        config.setSocketTimeout(5000);

        HttpClientHandlerAzure handler = new HttpClientHandlerAzure(config);

        assertNotNull(handler);
    }

    @Test
    public void should_create_handler_with_custom_configuration() {
        RetryAndTimeoutConfiguration config = new RetryAndTimeoutConfiguration();
        config.setMaxRetry(5);
        config.setConnectTimeout(10000);
        config.setRequestTimeout(15000);
        config.setSocketTimeout(20000);

        HttpClientHandlerAzure handler = new HttpClientHandlerAzure(config);

        assertNotNull(handler);
    }
}
