/** Copyright © Microsoft Corporation

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License
 **/

package org.opengroup.osdu.azure.httpconfig;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.resiliency.AzureCircuitBreakerConfiguration;
import org.opengroup.osdu.core.common.http.FetchServiceHttpRequest;
import org.opengroup.osdu.core.common.http.HttpRequest;
import org.opengroup.osdu.core.common.http.UrlFetchServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.net.URISyntaxException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HttpClientAzureTest {
    @InjectMocks
    HttpClientAzure sut;
    @Mock
    UrlFetchServiceImpl urlFetchService;
    @Spy
    AzureCircuitBreakerConfiguration azureCircuitBreakerConfiguration;

    @Test
    public void when_circuitbreaker_name_is_localhost() throws URISyntaxException {

        azureCircuitBreakerConfiguration.setEnable(true);
        azureCircuitBreakerConfiguration.setCircuitBreakerRegistry(CircuitBreakerRegistry.ofDefaults());
        when(azureCircuitBreakerConfiguration.isEnable()).thenReturn(true);
        org.opengroup.osdu.core.common.model.http.HttpResponse  httpResponse = new org.opengroup.osdu.core.common.model.http.HttpResponse();
        httpResponse.setBody("rr");
        when(urlFetchService.sendRequest(any(FetchServiceHttpRequest.class), anyBoolean())).thenReturn(httpResponse);
        sut.send(HttpRequest.builder().url("http://localhost/").build());
        assertEquals("localhost",azureCircuitBreakerConfiguration.getCircuitBreakerRegistry().getAllCircuitBreakers().stream().toList().get(0).getName());
    }

    @Test
    public void when_circuitbreaker_name_is_localhost_with_www() throws URISyntaxException {

        azureCircuitBreakerConfiguration.setEnable(true);
        azureCircuitBreakerConfiguration.setCircuitBreakerRegistry(CircuitBreakerRegistry.ofDefaults());
        when(azureCircuitBreakerConfiguration.isEnable()).thenReturn(true);
        org.opengroup.osdu.core.common.model.http.HttpResponse  httpResponse = new org.opengroup.osdu.core.common.model.http.HttpResponse();
        httpResponse.setBody("rr");
        when(urlFetchService.sendRequest(any(FetchServiceHttpRequest.class), anyBoolean())).thenReturn(httpResponse);
        sut.send(HttpRequest.builder().url("http://www.localhost/").build());
        assertEquals("localhost",azureCircuitBreakerConfiguration.getCircuitBreakerRegistry().getAllCircuitBreakers().stream().toList().get(0).getName());
    }

    @Test
    public void when_circuitbreaker_name_is_entitlements() throws URISyntaxException {

        azureCircuitBreakerConfiguration.setEnable(true);
        azureCircuitBreakerConfiguration.setCircuitBreakerRegistry(CircuitBreakerRegistry.ofDefaults());
        when(azureCircuitBreakerConfiguration.isEnable()).thenReturn(true);
        org.opengroup.osdu.core.common.model.http.HttpResponse  httpResponse = new org.opengroup.osdu.core.common.model.http.HttpResponse();
        httpResponse.setBody("rr");
        when(urlFetchService.sendRequest(any(FetchServiceHttpRequest.class), anyBoolean())).thenReturn(httpResponse);
        sut.send(HttpRequest.builder().url("http://entitlements/api/v2").build());
        assertEquals("entitlements",azureCircuitBreakerConfiguration.getCircuitBreakerRegistry().getAllCircuitBreakers().stream().toList().get(0).getName());
    }
}
