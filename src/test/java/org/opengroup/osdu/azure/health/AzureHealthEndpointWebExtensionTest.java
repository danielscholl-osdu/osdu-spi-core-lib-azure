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

package org.opengroup.osdu.azure.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.logging.CoreLogger;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.actuate.health.Status;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for LoggingHealthEndPointWebExtension.
 */
@ExtendWith(MockitoExtension.class)
public class AzureHealthEndpointWebExtensionTest {

    private static AzureHealthEndpointWebExtension loggingHealthEndpointWebExtension;
    private static AzureHealthEndpointWebExtension spy;
    private static HealthContributorRegistry registry;
    private static HealthEndpointGroups groups;
    private static Duration slowIndicatorLoggingThreshold;
    private static Map<String, HealthComponent> componentMap; //Contains the 'components' of the health endpoint response

    @Mock
    private CoreLoggerFactory coreLoggerFactory;

    @Mock
    private CoreLogger logger;

    @Mock
    SecurityContext securityContext;

    @Mock
    WebServerNamespace serverNamespace;

    @Mock
    Health health;

    @Mock
    CompositeHealth compositeHealth;

    /**
     * Workaround for inability to mock static methods like getInstance().
     *
     * @param mock CoreLoggerFactory mock instance
     */
    private void mockSingleton(CoreLoggerFactory mock) {
        try {
            Field instance = CoreLoggerFactory.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, mock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    public void setup() {
        registry = mock(HealthContributorRegistry.class);
        groups = mock(HealthEndpointGroups.class);
        slowIndicatorLoggingThreshold = mock(Duration.class);
        loggingHealthEndpointWebExtension = new AzureHealthEndpointWebExtension(registry,groups,slowIndicatorLoggingThreshold);
        spy = Mockito.spy(loggingHealthEndpointWebExtension);
        componentMap = new HashMap<>();
        componentMap.put("keyvault", health);
    }

    /**
     Verify that nothing is logged when Status of health endpoint is UP.
     */

    @Test
    public void healthTest_whenStatusIs_UP() {
        Mockito.when(health.getStatus()).thenReturn(Status.UP);
        Mockito.when(compositeHealth.getComponents()).thenReturn(componentMap);

        WebEndpointResponse<HealthComponent> expected = new WebEndpointResponse<>(compositeHealth, WebEndpointResponse.STATUS_OK);

        doReturn(expected).when(spy).superClassCall(ApiVersion.LATEST, serverNamespace, securityContext, true);
        spy.health(ApiVersion.LATEST, serverNamespace, securityContext, true, new String[0]);

        verify(logger, times(0)).error(anyString(), any(), any());

    }

    /**
     Verify that a single line is logged when ONE HealthComponent has Status DOWN.
     */
    @Test
    public void healthTest_whenStatusIs_DOWN() throws Exception{
        mockSingleton(coreLoggerFactory);
        when(coreLoggerFactory.getLogger(anyString())).thenReturn(logger);

        Mockito.when(health.getStatus()).thenReturn(Status.DOWN);
        Mockito.when(compositeHealth.getComponents()).thenReturn(componentMap);

        WebEndpointResponse<HealthComponent> expected = new WebEndpointResponse<>(compositeHealth, WebEndpointResponse.STATUS_SERVICE_UNAVAILABLE);

        doReturn(expected).when(spy).superClassCall(ApiVersion.LATEST, serverNamespace, securityContext, true);

        spy.health(ApiVersion.LATEST, serverNamespace, securityContext, true, new String[0]);
        verify(logger, times(1)).error(anyString(), any(), any());

    }

}
