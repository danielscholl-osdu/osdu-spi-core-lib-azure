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

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

/**
 * Dependency payload.
 */
@Getter
@Setter
public final class DependencyPayload {
    private String name;
    private String type = "HTTP";
    private String data;
    private Duration duration;
    private double requestCharge;
    private String resultCode;
    private String target;
    private boolean success;

    /**
     * Instantiate empty payload.
     */
    public DependencyPayload() {
    }

    /**
     * Instantiate payload with specified values.
     *
     * @param dependencyName       the name of the command initiated with this dependency call
     * @param dependencyData       the command initiated by this dependency call
     * @param dependencyDuration   the request duration
     * @param dependencyResultCode the result code of the call
     * @param dependencySuccess    indication of successful or unsuccessful call
     */
    public DependencyPayload(final String dependencyName, final String dependencyData, final Duration dependencyDuration, final String dependencyResultCode, final boolean dependencySuccess) {
        this.name = dependencyName;
        this.data = dependencyData;
        this.duration = dependencyDuration;
        this.resultCode = dependencyResultCode;
        this.success = dependencySuccess;
        this.target = dependencyData;
    }

    @Override
    public String toString() {
        return String.format("{\"name\": \"%s\", \"data\": \"%s\", \"duration\": %d, \"resultCode\": \"%s\", \"success\": %s}", name, data, duration.toNanos(), resultCode, success);
    }
}
