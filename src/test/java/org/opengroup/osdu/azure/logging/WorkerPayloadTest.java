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
package org.opengroup.osdu.azure.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class WorkerPayloadTest {

    @Test
    public void should_create_payload_with_all_fields() {
        WorkerPayload payload = new WorkerPayload("msg-1", "worker-name", "data-1", "prefix");

        assertEquals("msg-1", payload.getId());
        assertEquals("worker-name", payload.getName());
        assertEquals("data-1", payload.getData());
        assertEquals("prefix", payload.getLogPrefix());
    }

    @Test
    public void should_format_toString_correctly() {
        WorkerPayload payload = new WorkerPayload("id-123", "myWorker", "someData", "LOG");

        String result = payload.toString();

        assertNotNull(result);
        assertEquals("LOG : {messageId: id-123, workerName: myWorker, data: someData }", result);
    }

    @Test
    public void should_support_setters() {
        WorkerPayload payload = new WorkerPayload("id", "name", "data", "prefix");

        payload.setId("new-id");
        payload.setName("new-name");
        payload.setData("new-data");
        payload.setLogPrefix("new-prefix");

        assertEquals("new-id", payload.getId());
        assertEquals("new-name", payload.getName());
        assertEquals("new-data", payload.getData());
        assertEquals("new-prefix", payload.getLogPrefix());
    }
}
