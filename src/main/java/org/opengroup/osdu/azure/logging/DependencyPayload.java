package org.opengroup.osdu.azure.logging;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

/**
 * Dependency payload.
 */
@Getter
@Setter
public final class DependencyPayload {
    private String name;
    private String type = "HTTP";
    private String data;
    private Duration duration;
    private String resultCode;
    private String target;
    private boolean success;

    /**
     * Instantiate empty payload.
     */
    public DependencyPayload() {
    }

    /**
     * Instantiate payload with specified values.
     *
     * @param dependencyData       the name of the command initiated with this dependency call
     * @param dependencyName       the command initiated by this dependency call
     * @param dependencyDuration   the request duration
     * @param dependencyResultCode the result code of the call
     * @param dependencySuccess    indication of successful or unsuccessful call
     */
    public DependencyPayload(final String dependencyName, final String dependencyData, final Duration dependencyDuration, final String dependencyResultCode, final boolean dependencySuccess) {
        this.name = dependencyName;
        this.data = dependencyData;
        this.duration = dependencyDuration;
        this.resultCode = dependencyResultCode;
        this.success = dependencySuccess;
        this.target = dependencyData;
    }

    @Override
    public String toString() {
        return String.format("{\"name\": \"%s\", \"data\": \"%s\", \"duration\": %d, \"resultCode\": \"%s\", \"success\": %s}", name, data, duration.getNano(), resultCode, success);
    }
}
