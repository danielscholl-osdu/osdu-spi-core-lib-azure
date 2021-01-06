package org.opengroup.osdu.azure.logging;

import com.microsoft.applicationinsights.internal.util.MapUtil;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.web.extensibility.initializers.WebTelemetryInitializerBase;
import org.slf4j.MDC;

/**
 * Enrich telemetry with custom properties that will appear in customDimensions.
 */
public class CustomDimensionsTelemetryInitializer extends WebTelemetryInitializerBase {
    /**
     * Add MDC custom properties to request or exception telemetry.
     *
     * @param telemetry the Telemetry object
     */
    @Override
    protected void onInitializeTelemetry(final Telemetry telemetry) {
        if (telemetry instanceof RequestTelemetry || telemetry instanceof ExceptionTelemetry) {
            MapUtil.copy(MDC.getCopyOfContextMap(), telemetry.getContext().getProperties());
        }
    }
}
