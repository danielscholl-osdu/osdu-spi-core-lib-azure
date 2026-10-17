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

package org.opengroup.osdu.azure.logging;

import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation for Core Logger Factory instances.
 */
public final class CoreLoggerFactory implements ICoreLoggerFactory {

    private static CoreLoggerFactory instance = new CoreLoggerFactory();

    private Map<String, ICoreLogger> loggers = new HashMap<>();

    private LoggerType enabledLoggerType;

    private static final String APPLICATION_INSIGHTS_ENABLE = "APPINSIGHTS_LOGGING_ENABLED";

    /**
     * CoreLoggerFactory singleton constructor resolves enabled loggerType for logging data to desired destination.
     */
    private CoreLoggerFactory() {
        this.enabledLoggerType = resolveEnabledLoggerType();
    }

    /**
     * EnabledLoggerType getter.
     * @return enabledLoggerType
     */
    public LoggerType getEnabledLoggerType() {
        return enabledLoggerType;
    }
    /**
     * Return this instance.
     *
     * @return this instance
     */
    public static CoreLoggerFactory getInstance() {
        if (instance == null) {
            instance = new CoreLoggerFactory();
        }
        return instance;
    }

    /**
     * @param name the name of the logger
     * @return Return a logger named according to the name parameter.
     */
    public ICoreLogger getLogger(final String name) {
        if (!loggers.containsKey(name)) {
            ICoreLogger logger = createLogger(enabledLoggerType, name);
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

    /**
     * Reset factory to get latest data.
     */
    public static void resetFactory() {
        instance = new CoreLoggerFactory();
    }

    /**
     *  function to resolve enabled logger type.
     * @return eligible loggerType using environment variable.
     */
    private LoggerType resolveEnabledLoggerType() {
        Object applicationInsightsEnabled = System.getProperty(APPLICATION_INSIGHTS_ENABLE);
        if (Objects.nonNull(applicationInsightsEnabled) && applicationInsightsEnabled.equals(String.valueOf(Boolean.FALSE))) {
            return LoggerType.CoreLogger;
        }
        return LoggerType.ApplicationInsightsLogger;
    }

    /**
     * Creating logger using loggerType and loggerName.
     * @param loggerType loggerType
     * @param loggerName loggerName
     * @return logger according to desired params
     */
    private ICoreLogger createLogger(final LoggerType loggerType, final String loggerName) {
        switch (loggerType) {
            case ApplicationInsightsLogger:
                return new ApplicationInsightsLogger(LoggerFactory.getLogger(loggerName));
            default:
                return new CoreLogger(LoggerFactory.getLogger(loggerName));
        }
    }
}
