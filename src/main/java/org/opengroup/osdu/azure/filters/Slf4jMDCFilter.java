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

import com.nimbusds.jwt.JWTClaimsSet;
import org.opengroup.osdu.azure.util.AuthUtils;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MDC Filter for logging.
 * Condition 1: MDC filter should be called before any filter which contains logging logic, like TransactionLogFilter
 * Condition 2: MDC filter should be called after OrderedRequestContextFilter, so that the request scope is set
 * Filter with lowest order value is picked up first
 * The order of TransactionLogFilter is not set, hence it picks up default value which is Ordered.LOWEST_PRECEDENCE (2147483647)
 * The order of OrderedRequestContextFilter is -105
 * Hence setting the order of MDC filter as -104
 * So now order of calling the filters becomes: OrderedRequestContextFilter --> MDC filter --> ... --> TransactionLogFilter
 */
@Component
@ConditionalOnProperty(value = "logging.mdccontext.enabled", havingValue = "true", matchIfMissing = true)
@Order(-104)
public class Slf4jMDCFilter implements Filter {

    private Map<String, String> handlerApiMap = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(Slf4jMDCFilter.class);
    @Autowired
    private DpsHeaders dpsHeaders;
    @Autowired
    private AuthUtils authUtils;
    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    /**
     * Filter logic.
     *
     * @param servletRequest  Request object.
     * @param servletResponse Response object.
     * @param filterChain     Filter Chain object.
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
                         final FilterChain filterChain) throws IOException, ServletException {
        MDC.clear();
        MDC.setContextMap(getContextMap(servletRequest));
        /* fix for api operation name */
        filterChain.doFilter(servletRequest, servletResponse);
    }

    /**
     * Method to create context map for mdc.
     *
     * @param servletRequest Response Object
     * @return Context map.
     */
    private Map<String, String> getContextMap(final ServletRequest servletRequest) {
        final Map<String, String> contextMap = new HashMap<>();
        contextMap.put(DpsHeaders.CORRELATION_ID, dpsHeaders.getCorrelationId());
        contextMap.put(DpsHeaders.DATA_PARTITION_ID, dpsHeaders.getPartitionId());

        //Adding custom columns for business metric
        contextMap.put("api-method", ((HttpServletRequest) servletRequest).getMethod());
        String operationApiPath = getOperationName(servletRequest);
        if (operationApiPath != null) {
            contextMap.put("operation-name", operationApiPath);
        }

        if (dpsHeaders.getAppId() != null) {
            contextMap.put("app-id", dpsHeaders.getAppId());
        }

        String userId = getUserId();
        if (userId != null) {
            contextMap.put("user-id", userId);
        }

        String oid = getOid();
        if (oid != null) {
            contextMap.put("oid", oid);
        }
        return contextMap;
    }

    /**
     * Get oid from Authorization payload (JWT token).
     *
     * @return the oid
     */
    private String getOid() {
        String authorization = dpsHeaders.getAuthorization();
        return authorization == null ? null : authUtils.getOidFromJwtToken(authorization);
    }

    /**
     * Get user ID from Authorization payload (JWT token).
     *
     * @return the user ID
     */
    private String getUserId() {
        JWTClaimsSet claimsSet = authUtils.getClaimsFromJwtToken(dpsHeaders.getAuthorization());
        return claimsSet == null ? null : claimsSet.getSubject();
    }

    /**
     * Method to create context map for mdc.
     *
     * @param servletRequest Response Object
     * @return OperationApiPath name
     */
    private String getOperationName(final ServletRequest servletRequest) {
        String operationApiPath = "";

        if (!ServletRequestPathUtils.hasParsedRequestPath(servletRequest)) {
            ServletRequestPathUtils.parseAndCache((HttpServletRequest) servletRequest);
        }

        if (handlerApiMap.isEmpty()) {
            parseAndCacheApiPaths();
        }

        try {
            HandlerExecutionChain handlerExecutionChain = requestMappingHandlerMapping.getHandler((HttpServletRequest) servletRequest);
            if (handlerExecutionChain == null) {
                return operationApiPath;
            }
            operationApiPath = handlerApiMap.get(Objects.requireNonNull(handlerExecutionChain).getHandler().toString());
        } catch (Exception e) {
            LOGGER.warn("Unable to get the operation-name due to {}", e.getMessage(), e);
        }
        return operationApiPath;
    }

    /**
     * Populates and caches context map.
     */
    private void parseAndCacheApiPaths() {
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();
        /* Swapping key value pair */
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            handlerApiMap.put(entry.getValue().toString(), entry.getKey().toString());
        }
    }
}

