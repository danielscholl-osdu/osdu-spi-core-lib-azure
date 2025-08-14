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

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for local development authentication using Azure CLI, VS Code, or IntelliJ credentials.
 * This configuration is only active when the "local" Spring profile is enabled.
 *
 * SECURITY WARNING: This configuration should NEVER be used in production environments.
 * It is intended solely for local development to provide a fast feedback loop for developers.
 */
@Configuration
@ConfigurationProperties(prefix = "azure.local.development")
@Profile("local")
@Getter
@Setter
public class LocalDevelopmentConfiguration {
    /**
     * Enable or disable local development credentials.
     * When enabled, the service will use Azure CLI, VS Code, or IntelliJ credentials
     * instead of Service Principal, Managed Identity, or Workload Identity.
     */
    private Boolean enableLocalCredentials = true;
}
