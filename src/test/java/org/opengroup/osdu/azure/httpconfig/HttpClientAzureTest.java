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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import org.opengroup.osdu.core.common.http.HttpResponse;
import org.opengroup.osdu.core.common.model.http.AppException;

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

    @Test
    public void when_circuitbreaker_disabled_calls_decoratedSend_directly() throws URISyntaxException {
        when(azureCircuitBreakerConfiguration.isEnable()).thenReturn(false);
        org.opengroup.osdu.core.common.model.http.HttpResponse httpResponse = new org.opengroup.osdu.core.common.model.http.HttpResponse();
        httpResponse.setBody("response-body");
        httpResponse.setResponseCode(200);
        when(urlFetchService.sendRequest(any(FetchServiceHttpRequest.class), anyBoolean())).thenReturn(httpResponse);

        HttpResponse result = sut.send(HttpRequest.builder().url("http://localhost/").build());

        assertNotNull(result);
        assertEquals("response-body", result.getBody());
        assertEquals(200, result.getResponseCode());
    }

    @Test
    public void when_url_is_invalid_returns_null_hostname() {
        String hostname = sut.getHostName("not-a-valid-url");
        assertNull(hostname);
    }

    @Test
    public void when_url_has_no_host_returns_null() {
        String hostname = sut.getHostName("file:///tmp/test");
        assertNull(hostname);
    }

    @Test
    public void decoratedSend_single_arg_delegates_to_two_arg() throws URISyntaxException {
        org.opengroup.osdu.core.common.model.http.HttpResponse httpResponse = new org.opengroup.osdu.core.common.model.http.HttpResponse();
        httpResponse.setBody("body");
        when(urlFetchService.sendRequest(any(FetchServiceHttpRequest.class), anyBoolean())).thenReturn(httpResponse);

        HttpResponse result = sut.decoratedSend(HttpRequest.builder().url("http://test/api").build());

        assertNotNull(result);
    }

    @Test
    public void send_single_arg_delegates_to_two_arg() throws URISyntaxException {
        when(azureCircuitBreakerConfiguration.isEnable()).thenReturn(false);
        org.opengroup.osdu.core.common.model.http.HttpResponse httpResponse = new org.opengroup.osdu.core.common.model.http.HttpResponse();
        httpResponse.setBody("body");
        when(urlFetchService.sendRequest(any(FetchServiceHttpRequest.class), anyBoolean())).thenReturn(httpResponse);

        HttpResponse result = sut.send(HttpRequest.builder().url("http://test/api").build());

        assertNotNull(result);
    }

    @Test
    public void send_with_null_hostname_url_skips_circuitbreaker() throws URISyntaxException {
        when(azureCircuitBreakerConfiguration.isEnable()).thenReturn(true);
        org.opengroup.osdu.core.common.model.http.HttpResponse httpResponse = new org.opengroup.osdu.core.common.model.http.HttpResponse();
        httpResponse.setBody("body");
        when(urlFetchService.sendRequest(any(FetchServiceHttpRequest.class), anyBoolean())).thenReturn(httpResponse);

        // URL with no host component — getHostName returns null, so CB is skipped
        HttpResponse result = sut.send(HttpRequest.builder().url("not-a-valid-url").build());

        assertNotNull(result);
    }
}
