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
package org.opengroup.osdu.azure.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

public class CosmosStorePageRequestTest {

    @Test
    public void should_create_with_constructor() {
        CosmosStorePageRequest request = new CosmosStorePageRequest(0, 10, "token-abc");

        assertEquals(0, request.getPageNumber());
        assertEquals(10, request.getPageSize());
        assertEquals("token-abc", request.getRequestContinuation());
    }

    @Test
    public void should_create_with_static_of() {
        CosmosStorePageRequest request = CosmosStorePageRequest.of(1, 20, "token-xyz");

        assertEquals(1, request.getPageNumber());
        assertEquals(20, request.getPageSize());
        assertEquals("token-xyz", request.getRequestContinuation());
    }

    @Test
    public void should_create_with_sort() {
        Sort sort = Sort.by("id");
        CosmosStorePageRequest request = new CosmosStorePageRequest(0, 10, "token", sort);

        assertEquals("token", request.getRequestContinuation());
        assertEquals(sort, request.getSort());
    }

    @Test
    public void should_create_with_static_of_and_sort() {
        Sort sort = Sort.by("name");
        CosmosStorePageRequest request = CosmosStorePageRequest.of(0, 5, "cont-token", sort);

        assertEquals("cont-token", request.getRequestContinuation());
        assertEquals(sort, request.getSort());
    }

    @Test
    public void should_handle_null_continuation_token() {
        CosmosStorePageRequest request = new CosmosStorePageRequest(0, 10, null);

        assertNull(request.getRequestContinuation());
    }

    @Test
    public void should_be_equal_when_same_values() {
        CosmosStorePageRequest r1 = new CosmosStorePageRequest(0, 10, "token");
        CosmosStorePageRequest r2 = new CosmosStorePageRequest(0, 10, "token");

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    public void should_not_be_equal_when_different_continuation() {
        CosmosStorePageRequest r1 = new CosmosStorePageRequest(0, 10, "token-a");
        CosmosStorePageRequest r2 = new CosmosStorePageRequest(0, 10, "token-b");

        assertNotEquals(r1, r2);
    }

    @Test
    public void should_not_be_equal_when_different_type() {
        CosmosStorePageRequest r1 = new CosmosStorePageRequest(0, 10, "token");

        assertNotEquals("not-a-page-request", r1);
    }

    @Test
    public void should_be_equal_to_itself() {
        CosmosStorePageRequest r1 = new CosmosStorePageRequest(0, 10, "token");

        assertEquals(r1, r1);
    }

    @Test
    public void should_handle_null_continuation_in_equals() {
        CosmosStorePageRequest r1 = new CosmosStorePageRequest(0, 10, null);
        CosmosStorePageRequest r2 = new CosmosStorePageRequest(0, 10, null);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    public void should_not_be_equal_when_one_null_continuation() {
        CosmosStorePageRequest r1 = new CosmosStorePageRequest(0, 10, null);
        CosmosStorePageRequest r2 = new CosmosStorePageRequest(0, 10, "token");

        assertNotEquals(r1, r2);
    }
}
