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

package org.opengroup.osdu.azure.logging;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DependencyPayloadTest {

    @Test
    public void testToString() {

        String name = "TestName";
        String data = "TestData";
        Duration duration = Duration.ofSeconds(5);
        String resultCode = "200";
        boolean success = true;

        DependencyPayload payload = new DependencyPayload(name, data, duration, resultCode, success);

        // Payload String Output
        String result = payload.toString();

        // Assert
        String expected = String.format("{\"name\": \"%s\", \"data\": \"%s\", \"duration\": %d, \"resultCode\": \"%s\", \"success\": %s}",
                name, data, duration.toNanos(), resultCode, success);
        assertEquals(expected, result);
    }
}