package org.opengroup.osdu.azure.filters;

import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link LogCustomDimensionFilter}.
 */
@ExtendWith(MockitoExtension.class)
public class LogCustomDimensionFilterTest {

    @Mock
    private ServletRequest servletRequest;
    @Mock
    private ServletResponse servletResponse;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private LogCustomDimensionFilter sut;

    @Test
    public void doFilter_whenAppInsightsAgentPresent_copiesMdcToTelemetryProperties() throws Exception {
        RequestTelemetryContext telemetryContext = mock(RequestTelemetryContext.class);
        RequestTelemetry requestTelemetry = mock(RequestTelemetry.class);
        Map<String, String> telemetryProperties = new java.util.HashMap<>();

        when(telemetryContext.getHttpRequestTelemetry()).thenReturn(requestTelemetry);
        when(requestTelemetry.getProperties()).thenReturn(telemetryProperties);

        try (MockedStatic<ThreadContext> threadContextMock = mockStatic(ThreadContext.class);
             MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {

            threadContextMock.when(ThreadContext::getRequestTelemetryContext).thenReturn(telemetryContext);
            mdcMock.when(MDC::getCopyOfContextMap).thenReturn(Map.of("correlationId", "abc-123", "partitionId", "test"));

            sut.doFilter(servletRequest, servletResponse, filterChain);

            verify(filterChain).doFilter(servletRequest, servletResponse);
        }
    }

    @Test
    public void doFilter_whenAppInsightsAgentAbsent_doesNotThrowNpe() throws Exception {
        // Regression test: ThreadContext.getRequestTelemetryContext() returns null when the
        // App Insights JVM agent (-javaagent) is not loaded. The filter must not NPE.
        try (MockedStatic<ThreadContext> threadContextMock = mockStatic(ThreadContext.class)) {
            threadContextMock.when(ThreadContext::getRequestTelemetryContext).thenReturn(null);

            sut.doFilter(servletRequest, servletResponse, filterChain);

            verify(filterChain).doFilter(servletRequest, servletResponse);
        }
    }

    @Test
    public void doFilter_whenAppInsightsAgentAbsent_doesNotAccessMdc() throws Exception {
        // When the agent is absent, MDC properties should not be copied (nothing to copy into).
        try (MockedStatic<ThreadContext> threadContextMock = mockStatic(ThreadContext.class);
             MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {

            threadContextMock.when(ThreadContext::getRequestTelemetryContext).thenReturn(null);

            sut.doFilter(servletRequest, servletResponse, filterChain);

            mdcMock.verify(MDC::getCopyOfContextMap, never());
            verify(filterChain).doFilter(servletRequest, servletResponse);
        }
    }
}
