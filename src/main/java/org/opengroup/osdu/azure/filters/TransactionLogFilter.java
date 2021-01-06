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

package org.opengroup.osdu.azure.filters;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * For logging start and end of request.
 */
@Component
@ConditionalOnProperty(value = "logging.transaction.enabled", havingValue = "true", matchIfMissing = true)
public final class TransactionLogFilter implements Filter {
    private static final String LOGGER_NAME = "TxnLogger";
    private static final List<String> WHITELIST_HEADERS = Arrays.asList(
            DpsHeaders.CORRELATION_ID,
            DpsHeaders.DATA_PARTITION_ID,
            DpsHeaders.CONTENT_TYPE);
    private static final String START_LOG_TEMPLATE = "Start Web-API %s %s %s";
    private static final String END_LOG_TEMPLATE = "End Web-API %s %s %s status=%d time=%d ms";

    @Autowired
    private JaxRsDpsLog jaxRsDpsLog;

    /**
     * Filter logic.
     * @param servletRequest Request object.
     * @param servletResponse Response object.
     * @param filterChain Filter Chain object.
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
                         final FilterChain filterChain) throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        final HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        logTransactionStart(httpRequest);
        final long start = System.currentTimeMillis();
        filterChain.doFilter(servletRequest, servletResponse);
        final long timeTaken = System.currentTimeMillis() - start;
        logTransactionEnd(httpRequest, httpResponse, timeTaken);
    }

    /**
     * Logs start of a request.
     * @param request Request Object.
     */
    private void logTransactionStart(final HttpServletRequest request) {
        jaxRsDpsLog.info(LOGGER_NAME, String.format(START_LOG_TEMPLATE, request.getMethod(), request.getServletPath(),
                getRequestHeadersString(request)));
    }

    /**
     * Logs end of a request.
     * @param request Request object.
     * @param response Response object.
     * @param timeTaken timeTaken in ms taken for request to complete.
     */
    private void logTransactionEnd(final HttpServletRequest request, final HttpServletResponse response,
                                   final long timeTaken) {
        jaxRsDpsLog.info(LOGGER_NAME, String.format(END_LOG_TEMPLATE, request.getMethod(), request.getServletPath(),
                        getResponseHeadersString(response), response.getStatus(), timeTaken));

    }

    /**
     * To construct string representation of request headers.
     * @param request Request Object.
     * @return String representation of request headers.
     */
    private String getRequestHeadersString(final HttpServletRequest request) {
        return getHeadersString(request::getHeader);
    }

    /**
     * To construct string representation of response headers.
     * @param response Response Object.
     * @return String representation of response headers.
     */
    private String getResponseHeadersString(final HttpServletResponse response) {
        return getHeadersString(response::getHeader);
    }

    /**
     * Construct string representation of headers.
     * @param headerGetter Header value supplier
     * @return String representation of headers
     */
    private String getHeadersString(final Function<String, String> headerGetter) {
        final StringBuilder headers = new StringBuilder();
        for (String headerName: WHITELIST_HEADERS) {
            if (headerGetter.apply(headerName) != null) {
                headers.append(headerName);
                headers.append(":");
                headers.append(headerGetter.apply(headerName));
                headers.append(",");
            }
        }

        if (headers.length() != 0) {
            headers.deleteCharAt(headers.length() - 1);
        }
        return String.format("Headers: {%s}", headers.toString());
    }
}
