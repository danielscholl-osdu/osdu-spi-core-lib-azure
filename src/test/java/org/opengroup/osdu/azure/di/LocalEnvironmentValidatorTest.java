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

package org.opengroup.osdu.azure.di;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LocalEnvironmentValidator}.
 *
 * <p>Uses the package-private constructor to inject a controlled environment reader,
 * avoiding any dependency on host environment variables.
 */
class LocalEnvironmentValidatorTest {

    /** Creates a validator where the given indicator is set to a non-null value. */
    private LocalEnvironmentValidator validatorWithIndicator(final String indicatorName) {
        Map<String, String> fakeEnv = new HashMap<>();
        fakeEnv.put(indicatorName, "present");
        return new LocalEnvironmentValidator(fakeEnv::get);
    }

    /** Creates a validator that sees no environment variables at all. */
    private LocalEnvironmentValidator cleanValidator() {
        return new LocalEnvironmentValidator(k -> null);
    }

    @Test
    void validateEnvironment_cleanEnvironment_doesNotThrow() {
        assertDoesNotThrow(() -> cleanValidator().validateEnvironment());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "KUBERNETES_SERVICE_HOST",
        "WEBSITE_INSTANCE_ID",
        "ACI_INSTANCE_NAME",
        "CONTAINER_APP_NAME",
        "FUNCTIONS_WORKER_RUNTIME",
        "CI",
        "CONTINUOUS_INTEGRATION"
    })
    void validateEnvironment_cloudIndicatorPresent_throwsIllegalStateException(final String indicator) {
        LocalEnvironmentValidator validator = validatorWithIndicator(indicator);

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            validator::validateEnvironment,
            "Expected IllegalStateException when " + indicator + " is set");

        assertTrue(ex.getMessage().contains("SECURITY VIOLATION"),
            "Error message should contain 'SECURITY VIOLATION'");
    }

    @Test
    void validateEnvironment_msiEndpointSet_throwsIllegalStateException() {
        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> validatorWithIndicator("MSI_ENDPOINT").validateEnvironment());

        assertTrue(ex.getMessage().contains("SECURITY VIOLATION"));
    }

    @Test
    void validateEnvironment_identityEndpointSet_throwsIllegalStateException() {
        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> validatorWithIndicator("IDENTITY_ENDPOINT").validateEnvironment());

        assertTrue(ex.getMessage().contains("SECURITY VIOLATION"));
    }
}
