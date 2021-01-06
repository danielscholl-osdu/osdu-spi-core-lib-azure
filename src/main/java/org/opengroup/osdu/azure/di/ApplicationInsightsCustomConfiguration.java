package org.opengroup.osdu.azure.di;

import com.microsoft.applicationinsights.extensibility.TelemetryInitializer;
import lombok.Getter;
import org.opengroup.osdu.azure.logging.CustomDimensionsTelemetryInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * A configuration bean class to set up Application Insights custom configurations.
 */
@Configuration
@Getter
@Lazy
public class ApplicationInsightsCustomConfiguration {
    /**
     * Bean for CustomDimensionsTelemetryInitializer.
     *
     * @return instance of {@link CustomDimensionsTelemetryInitializer}
     */
    @Bean
    public TelemetryInitializer customDimensionsTelemetryInitializer() {
        return new CustomDimensionsTelemetryInitializer();
    }
}
