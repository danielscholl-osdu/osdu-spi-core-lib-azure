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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.logging.CoreLogger;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;

@ExtendWith(MockitoExtension.class)
public class NoOpRedisCacheTest {

    @Mock
    private CoreLoggerFactory coreLoggerFactory;

    @Mock
    private CoreLogger coreLogger;

    private NoOpRedisCache<String> cache;

    @BeforeEach
    void setUp() {
        mockSingleton(coreLoggerFactory);
        when(coreLoggerFactory.getLogger(anyString())).thenReturn(coreLogger);
        cache = new NoOpRedisCache<>();
    }

    @AfterEach
    void tearDown() {
        resetSingleton();
    }

    @Test
    public void put_should_not_store_value() {
        cache.put("key", "value");
        assertNull(cache.get("key"));
    }

    @Test
    public void put_with_ttl_should_not_store_value() {
        cache.put("key", 3600L, "value");
        assertNull(cache.get("key"));
    }

    @Test
    public void get_should_return_null() {
        assertNull(cache.get("key"));
    }

    @Test
    public void delete_should_not_throw() {
        cache.put("key", "value");
        cache.delete("key");
        assertNull(cache.get("key"));
    }

    @Test
    public void clearAll_should_not_throw() {
        cache.put("key", "value");
        cache.clearAll();
        assertNull(cache.get("key"));
    }

    @Test
    public void updateTtl_should_return_false() {
        assertFalse(cache.updateTtl("key", 100L));
    }

    @Test
    public void getTtl_should_return_zero() {
        assertEquals(0L, cache.getTtl("key"));
    }

    @Test
    public void info_should_return_null() {
        assertNull(cache.info());
    }

    @Test
    public void increment_should_return_null() {
        assertNull(cache.increment("key"));
    }

    @Test
    public void incrementBy_should_return_null() {
        assertNull(cache.incrementBy("key", 5L));
    }

    @Test
    public void decrement_should_return_null() {
        assertNull(cache.decrement("key"));
    }

    @Test
    public void decrementBy_should_return_null() {
        assertNull(cache.decrementBy("key", 3L));
    }

    @Test
    public void getCodec_should_return_null() {
        assertNull(cache.getCodec(String.class, String.class));
    }

    @Test
    public void authenticate_should_return_true() {
        assertTrue(cache.authenticate("user", "pass"));
    }

    private void mockSingleton(CoreLoggerFactory mock) {
        try {
            Field instance = CoreLoggerFactory.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, mock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void resetSingleton() {
        try {
            Field instance = CoreLoggerFactory.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
            instance.setAccessible(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
