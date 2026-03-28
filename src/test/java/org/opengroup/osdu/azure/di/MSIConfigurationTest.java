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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class MSIConfigurationTest {

    @Test
    public void should_return_false_when_both_disabled() {
        WorkloadIdentityConfiguration wiConfig = new WorkloadIdentityConfiguration();
        wiConfig.setIsEnabled(false);

        MSIConfiguration config = new MSIConfiguration();
        config.setWorkloadIdentityConfiguration(wiConfig);
        config.setIsEnabled(false);

        assertFalse(config.getIsEnabled());
    }

    @Test
    public void should_return_true_when_msi_enabled() {
        WorkloadIdentityConfiguration wiConfig = new WorkloadIdentityConfiguration();
        wiConfig.setIsEnabled(false);

        MSIConfiguration config = new MSIConfiguration();
        config.setWorkloadIdentityConfiguration(wiConfig);
        config.setIsEnabled(true);

        assertTrue(config.getIsEnabled());
    }

    @Test
    public void should_return_true_when_workload_identity_enabled() {
        WorkloadIdentityConfiguration wiConfig = new WorkloadIdentityConfiguration();
        wiConfig.setIsEnabled(true);

        MSIConfiguration config = new MSIConfiguration();
        config.setWorkloadIdentityConfiguration(wiConfig);
        config.setIsEnabled(false);

        assertTrue(config.getIsEnabled());
    }
}
