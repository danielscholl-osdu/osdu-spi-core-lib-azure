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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class RedisAzureConfigurationTest {

    @Test
    public void should_create_with_basic_constructor() {
        RedisAzureConfiguration config = new RedisAzureConfiguration(0, 3600, 6380, 10L, 5);

        assertEquals(0, config.getDatabase());
        assertEquals(3600, config.getExpiration());
        assertEquals(6380, config.getPort());
        assertEquals(10L, config.getConnectionTimeout());
        assertEquals(5, config.getCommandTimeout());
        assertEquals("redis-hostname", config.getHostKey());
        assertEquals("redis-password", config.getPasswordKey());
        assertNull(config.getPrincipalId());
        assertNull(config.getHostname());
    }

    @Test
    public void should_create_with_principal_id_constructor() {
        RedisAzureConfiguration config = new RedisAzureConfiguration(0, 3600, 6380, 10L, 5, "principal-123");

        assertEquals("principal-123", config.getPrincipalId());
        assertNull(config.getHostname());
    }

    @Test
    public void should_create_with_direct_hostname_constructor() {
        RedisAzureConfiguration config = new RedisAzureConfiguration(0, 3600, 6380, 10L, 5, "principal-123", "redis.example.com");

        assertEquals("principal-123", config.getPrincipalId());
        assertEquals("redis.example.com", config.getHostname());
    }

    @Test
    public void should_create_with_all_args_constructor() {
        RedisAzureConfiguration config = new RedisAzureConfiguration(1, 7200, 6379, 15L, 10, "host-key", "pass-key", "principal", "redis.local");

        assertEquals(1, config.getDatabase());
        assertEquals(7200, config.getExpiration());
        assertEquals(6379, config.getPort());
        assertEquals(15L, config.getConnectionTimeout());
        assertEquals(10, config.getCommandTimeout());
        assertEquals("host-key", config.getHostKey());
        assertEquals("pass-key", config.getPasswordKey());
        assertEquals("principal", config.getPrincipalId());
        assertEquals("redis.local", config.getHostname());
    }
}
