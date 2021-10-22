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
// limitations under the License

package org.opengroup.osdu.azure.httpconfig;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.decorators.Decorators;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.azure.resiliency.AzureCircuitBreakerConfiguration;
import org.opengroup.osdu.core.common.http.FetchServiceHttpRequest;
import org.opengroup.osdu.core.common.http.HttpRequest;
import org.opengroup.osdu.core.common.http.IHttpClient;
import org.opengroup.osdu.core.common.http.HttpResponse;
import org.opengroup.osdu.core.common.http.UrlFetchServiceImpl;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Supplier;

import static java.util.Arrays.asList;

/**
 * Extends URlFetchService and implements IHttpClient to send requests.
 */
@Primary
@Component
@ConditionalOnProperty(value = "requestScope.enabled", havingValue = "true", matchIfMissing = true)
public class HttpClientAzure implements IHttpClient {
    private static final String LOGGER_NAME = HttpClientAzure.class.getName();

    @Autowired
    private AzureCircuitBreakerConfiguration azureCircuitBreakerConfiguration;

    @Autowired
    private UrlFetchServiceImpl urlFetchService;
    /**
     * Decorated method to send request with circuitbreaker.
     *
     * @param httpRequest made by user class
     * @return HttpResponse
     */
    public HttpResponse decoratedSend(final HttpRequest httpRequest) {
        org.opengroup.osdu.core.common.model.http.HttpResponse response = null;
        try {
            response = this.urlFetchService.sendRequest(FetchServiceHttpRequest.builder()
                    .body(httpRequest.getBody())
                    .httpMethod(httpRequest.getHttpMethod())
                    .queryParams(httpRequest.getQueryParams())
                    .url(httpRequest.getUrl())
                    .headers(httpRequest.getHeaders())
                    .build());
        } catch (URISyntaxException e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getReason(), "URI Syntax is not correct", e);
        }
        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setBody(response.getBody());
        httpResponse.setResponseCode(response.getResponseCode());
        httpResponse.setContentType(response.getContentType());
        httpResponse.setRequest(httpRequest);
        return httpResponse;
    }

    /**
     * calls urlfetchservice's send request after applying a circuitbreaker.
     *
     * @param httpRequest made by user class
     * @return HttpResponse
     */
    @Override
    public HttpResponse send(final HttpRequest httpRequest) {
        if (!azureCircuitBreakerConfiguration.isEnable()) {
            // Call method without CircuitBreaker
            return this.decoratedSend(httpRequest);
        }
        String circuitBreakerName = getHostName(httpRequest.getUrl());
        if (circuitBreakerName == null) {
            // Call method without CircuitBreaker
            return this.decoratedSend(httpRequest);
        }
        CircuitBreaker circuitBreaker = azureCircuitBreakerConfiguration.getCircuitBreakerRegistry().circuitBreaker(circuitBreakerName);
        circuitBreaker.getEventPublisher()
                .onStateTransition(event ->  CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("CircuitBreakerEvent : {}", event));
        Supplier<HttpResponse> httpResponseSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, () -> this.decoratedSend(httpRequest));

        // Ensuring CallNotPermittedException that is being thrown by CircuitBreaker is caught and Error Code 503 is thrown.
        // We are throwing Error 503 based on information from https://docs.microsoft.com/en-us/azure/architecture/patterns/circuit-breaker
        return Decorators.ofSupplier(httpResponseSupplier).withFallback(asList(CallNotPermittedException.class), throwable -> {
            CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info(throwable.getMessage());
            throw new AppException(HttpStatus.SC_SERVICE_UNAVAILABLE, "Service Unavailable", "Service Unavailable");
        }).get();
    }

    /**
     * Fetches hostname from URL.
     * http://entitlements/api/entitlements/v2 --> entitlements
     * @param url eg : http://entitlements/api/entitlements/v2
     * @return will return "entitlements" or null if there's an error with URL
     */
    public String getHostName(final String url) {
        URI uri = null;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
        String hostname = uri.getHost();
        // to provide faultproof result, check if not null then return only hostname, without www.
        if (hostname != null) {
            return hostname.startsWith("www.") ? hostname.substring(4) : hostname;
        }
        return null;
    }
}
