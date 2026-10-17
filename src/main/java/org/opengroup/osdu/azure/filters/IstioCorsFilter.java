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

package org.opengroup.osdu.azure.filters;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.web.cors.CorsUtils.isCorsRequest;
import static org.springframework.web.cors.CorsUtils.isPreFlightRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * To set response headers to null for a Preflight CORS Request. For non-CORS requests, request directed to next Filter in chain.
 */
@Component
@ConditionalOnProperty(value = "azure.istio.corsEnabled", havingValue = "true", matchIfMissing = false)
@Order(Integer.MIN_VALUE)
public final class IstioCorsFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(IstioCorsFilter.class);
    private static final String LOGGER_NAME = "CORSLogger";

    /**
     * Filter logic.
     *
     * @param request  Request object.
     * @param response Response object.
     * @param chain    Filter Chain object.
     */
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;

        //Only execute CORS policy if request is cross-origin and a Preflight request
        if (isCorsRequest(httpRequest) && isPreFlightRequest(httpRequest)) {
            Map<String, String> responseHeaders = getCorsHeadersWithNullValues();
            for (Map.Entry<String, String> header : responseHeaders.entrySet()) {
                httpResponse.addHeader(header.getKey(), header.getValue());
            }
            LOGGER.info(LOGGER_NAME + ": core-lib-azure sets response headers to null successfully");
            //For HTTP OPTIONS method reply with ACCEPTED status code -- per CORS handshake
            httpResponse.setStatus(HttpStatus.SC_OK);
            return;
        }
        chain.doFilter(httpRequest, httpResponse);
    }

    /**
     * Function sets response headers to null.
     * @return responseHeaders Null response.
     */
    public Map<String, String> getCorsHeadersWithNullValues() {
        Map<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Access-Control-Allow-Origin", null);
        responseHeaders.put("Access-Control-Allow-Methods", null);
        responseHeaders.put("Access-Control-Max-Age", null);
        responseHeaders.put("Access-Control-Allow-Headers", null);
        responseHeaders.put("Access-Control-Expose-Headers", null);
        return responseHeaders;
    }
    @Override
    public void destroy() {
    }
}

