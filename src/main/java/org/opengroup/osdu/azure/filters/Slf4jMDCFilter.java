package org.opengroup.osdu.azure.filters;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
    @Autowired
    private DpsHeaders dpsHeaders;

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
        MDC.setContextMap(getContextMap());
        filterChain.doFilter(servletRequest, servletResponse);
    }

    /**
     * Method to create context map for mdc.
     *
     * @return Context map.
     */
    private Map<String, String> getContextMap() {
        final Map<String, String> contextMap = new HashMap<>();
        contextMap.put(DpsHeaders.CORRELATION_ID, dpsHeaders.getCorrelationId());
        contextMap.put(DpsHeaders.DATA_PARTITION_ID, dpsHeaders.getPartitionId());
        return contextMap;
    }
}
