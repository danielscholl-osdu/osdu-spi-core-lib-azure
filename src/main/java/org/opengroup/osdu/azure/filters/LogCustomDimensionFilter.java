package org.opengroup.osdu.azure.filters;

import com.microsoft.applicationinsights.internal.util.MapUtil;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * LogCustomDimensionFilter.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public final class LogCustomDimensionFilter implements Filter {
    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException {
        RequestTelemetry requestTelemetry = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry();
        MapUtil.copy(MDC.getCopyOfContextMap(), requestTelemetry.getProperties());
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
