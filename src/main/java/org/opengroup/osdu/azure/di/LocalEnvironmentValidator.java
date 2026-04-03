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

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Validator to ensure that the local development profile is not accidentally used
 * in production or deployed environments.
 *
 * This component performs safety checks at application startup when the "local" profile
 * is active to prevent security vulnerabilities from using development credentials
 * in production environments.
 */
@Component
@Profile("local")
public class LocalEnvironmentValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalEnvironmentValidator.class);

    /**
     * Validates that the application is not running in a production or cloud environment
     * when the local development profile is active.
     *
     * @throws IllegalStateException if local profile is detected in a production environment
     */
    @PostConstruct
    public void validateEnvironment() {
        LOGGER.warn("==========================================================");
        LOGGER.warn("LOCAL DEVELOPMENT PROFILE IS ACTIVE");
        LOGGER.warn("Using local development credentials (Azure CLI/VS Code/IntelliJ)");
        LOGGER.warn("This configuration should NEVER be used in production!");
        LOGGER.warn("==========================================================");

        // Check if running in Kubernetes
        if (System.getenv("KUBERNETES_SERVICE_HOST") != null) {
            String errorMsg = "SECURITY VIOLATION: Local development profile detected in Kubernetes environment! "
                            + "The 'local' profile must not be used in deployed environments.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // Check if running in Azure App Service
        if (System.getenv("WEBSITE_INSTANCE_ID") != null) {
            String errorMsg = "SECURITY VIOLATION: Local development profile detected in Azure App Service! "
                            + "The 'local' profile must not be used in deployed environments.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // Check if running in Azure Container Instance
        if (System.getenv("ACI_INSTANCE_NAME") != null) {
            String errorMsg = "SECURITY VIOLATION: Local development profile detected in Azure Container Instance! "
                            + "The 'local' profile must not be used in deployed environments.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // Check if running in Azure Container Apps
        if (System.getenv("CONTAINER_APP_NAME") != null) {
            String errorMsg = "SECURITY VIOLATION: Local development profile detected in Azure Container Apps! "
                            + "The 'local' profile must not be used in deployed environments.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // Check if running in Azure Functions
        if (System.getenv("FUNCTIONS_WORKER_RUNTIME") != null) {
            String errorMsg = "SECURITY VIOLATION: Local development profile detected in Azure Functions! "
                            + "The 'local' profile must not be used in deployed environments.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // Check for Azure Managed Identity endpoint (indicates running in Azure)
        if (System.getenv("MSI_ENDPOINT") != null || System.getenv("IDENTITY_ENDPOINT") != null) {
            String errorMsg = "SECURITY VIOLATION: Local development profile detected in Azure environment with Managed Identity! "
                            + "The 'local' profile must not be used in deployed environments.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // Check for common CI/CD environments — local dev credentials must not run in CI pipelines
        if (System.getenv("CI") != null || System.getenv("CONTINUOUS_INTEGRATION") != null) {
            String errorMsg = "SECURITY VIOLATION: Local development profile detected in CI/CD environment! "
                            + "The 'local' profile must not be used in automated pipelines.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        LOGGER.info("Local environment validation passed. Running in local development mode.");
    }
}
