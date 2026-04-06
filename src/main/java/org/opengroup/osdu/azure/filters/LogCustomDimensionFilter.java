package org.opengroup.osdu.azure.filters;

import com.microsoft.applicationinsights.internal.util.MapUtil;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
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
        // ThreadContext is only populated when the Application Insights JVM agent is loaded at startup.
        // Guard against null to allow the service to run without the agent (e.g. local dev, community deployments).
        RequestTelemetryContext telemetryContext = ThreadContext.getRequestTelemetryContext();
        if (telemetryContext != null) {
            MapUtil.copy(MDC.getCopyOfContextMap(), telemetryContext.getHttpRequestTelemetry().getProperties());
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
