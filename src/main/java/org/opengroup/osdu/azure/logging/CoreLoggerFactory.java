package org.opengroup.osdu.azure.logging;

import com.microsoft.applicationinsights.TelemetryClient;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation for Core Logger Factory instances.
 */
public final class CoreLoggerFactory implements ICoreLoggerFactory {

    private static CoreLoggerFactory instance = new CoreLoggerFactory();

    private Map<String, ICoreLogger> loggers = new HashMap<>();

    private TelemetryClient telemetryClient = new TelemetryClient();

    /**
     * CoreLoggerFactory singleton.
     */
    private CoreLoggerFactory() {
    }

    /**
     * Return this instance.
     *
     * @return this instance
     */
    public static CoreLoggerFactory getInstance() {
        return instance;
    }

    /**
     * @param name the name of the logger
     * @return Return a logger named according to the name parameter.
     */
    public ICoreLogger getLogger(final String name) {
        if (!loggers.containsKey(name)) {
            CoreLogger logger = new CoreLogger(LoggerFactory.getLogger(name), this.telemetryClient);
            loggers.put(name, logger);
        }

        return loggers.get(name);
    }

    /**
     * @param clazz the name of the logger
     * @return Return a logger named corresponding to the class passed as parameter.
     */
    public ICoreLogger getLogger(final Class<?> clazz) {
        return this.getLogger(clazz.getName());
    }
}
