//  Copyright © Microsoft Corporation
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.opengroup.osdu.azure.health;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.http.ApiVersion;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.actuate.health.HealthEndpointWebExtension;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Implementation for logging features of health check failures.
 * All services can pick up this class from core-lib-azure from the usual dependency that’s added.
 */
@Configuration
@ConditionalOnProperty(value = "azure.health.endpoint.enabled", havingValue = "true", matchIfMissing = false)
public class AzureHealthEndpointWebExtension extends HealthEndpointWebExtension {

    private static final String LOGGER_NAME = AzureHealthEndpointWebExtension.class.getName();

    /**
     * @param registry the HealthContributorRegistry
     * @param groups   the HealthEndpointGroups
     */
    public AzureHealthEndpointWebExtension(final HealthContributorRegistry registry, final HealthEndpointGroups groups) {
        super(registry, groups);

    }

    /**
     * @param apiVersion
     * @param securityContext
     * @param showAll
     * @param path
     * @return
     */
    @Override
    public WebEndpointResponse<HealthComponent> health(final ApiVersion apiVersion, final SecurityContext securityContext,
                                                       final boolean showAll, final String... path) {
        WebEndpointResponse<HealthComponent> response = superClassCall(apiVersion, securityContext, showAll, path);
        HealthComponent health = response.getBody();

        Status status = health.getStatus();
        if (status != Status.UP) {

            Map<String, HealthComponent> map = ((CompositeHealth) health).getComponents();
            for (Map.Entry<String, HealthComponent> entry : map.entrySet())  {
                Status componentStatus = entry.getValue().getStatus();
                String componentLabel = entry.getKey();
                if (componentStatus == Status.DOWN) {
                    CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).error("Health component {} has status {}", componentLabel, componentStatus);
                }
            }
        }
        return response;
    }

    /**
     * @param apiVersion      the Api Version
     * @param securityContext the security Context
     * @param showAll         the boolean flag
     * @param path            the path
     * @return the webEndpointResponse object
     */
    WebEndpointResponse<HealthComponent> superClassCall(final ApiVersion apiVersion, final SecurityContext securityContext, final boolean showAll, final String... path) {
        return super.health(apiVersion, securityContext, showAll, path);
    }

}
