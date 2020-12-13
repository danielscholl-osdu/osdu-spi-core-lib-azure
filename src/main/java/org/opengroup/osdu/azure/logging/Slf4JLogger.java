//  Copyright © Microsoft Corporation
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.opengroup.osdu.azure.logging;

import org.opengroup.osdu.core.common.logging.ILogger;
import org.opengroup.osdu.core.common.logging.audit.AuditPayload;
import org.opengroup.osdu.core.common.model.http.HeadersToLog;
import org.opengroup.osdu.core.common.model.http.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * {@link ILogger} implementation with Slf4J Logger.
 */
@Component
@Primary
@ConditionalOnProperty(value = "logging.slf4jlogger.enabled", havingValue = "true", matchIfMissing = false)
public final class Slf4JLogger implements ILogger {
    private static final String DEFAULT_LOGGER_NAME = Slf4JLogger.class.getName();

    @Autowired
    private HeadersToLog headersToLog;

    @Override
    public void audit(final String logPrefix, final AuditPayload auditPayload, final Map<String, String> headers) {
        this.audit(DEFAULT_LOGGER_NAME, logPrefix, auditPayload, headers);
    }

    @Override
    public void audit(final String loggerName, final String logPrefix, final AuditPayload payload, final Map<String, String> headers) {
        CoreLoggerFactory.getInstance().getLogger(loggerName).info("{} {} {}", logPrefix, payload,
                this.headersToLog.createStandardLabelsFromMap(headers));
    }

    @Override
    public void request(final String logPrefix, final Request request, final Map<String, String> headers) {
        this.request(DEFAULT_LOGGER_NAME, logPrefix, request, headers);
    }

    @Override
    public void request(final String loggerName, final String logPrefix, final Request request, final Map<String, String> headers) {
        CoreLoggerFactory.getInstance().getLogger(loggerName).info("{} {} {}", logPrefix, request,
                this.headersToLog.createStandardLabelsFromMap(headers));
    }

    @Override
    public void info(final String logPrefix, final String message, final Map<String, String> headers) {
        this.info(DEFAULT_LOGGER_NAME, logPrefix, message, headers);
    }

    @Override
    public void info(final String loggerName, final String logPrefix, final String message, final Map<String, String> headers) {
        CoreLoggerFactory.getInstance().getLogger(loggerName).info("{} {} {}", logPrefix, message,
                this.headersToLog.createStandardLabelsFromMap(headers));
    }

    @Override
    public void warning(final String logPrefix, final String message, final Map<String, String> headers) {
        this.warning(DEFAULT_LOGGER_NAME, logPrefix, message, headers);
    }

    @Override
    public void warning(final String loggerName, final String logPrefix, final String message, final Map<String, String> headers) {
        CoreLoggerFactory.getInstance().getLogger(loggerName).warn("{} {} {}", logPrefix, message,
                this.headersToLog.createStandardLabelsFromMap(headers));
    }

    @Override
    public void warning(final String logPrefix, final String message, final Exception e, final Map<String, String> headers) {
        this.warning(DEFAULT_LOGGER_NAME, logPrefix, message, e, headers);
    }

    @Override
    public void warning(final String loggerName, final String logPrefix, final String message, final Exception ex, final Map<String, String> headers) {
        CoreLoggerFactory.getInstance().getLogger(loggerName).warn("{} {} {}", logPrefix, message,
                this.headersToLog.createStandardLabelsFromMap(headers), ex);
    }

    @Override
    public void error(final String logPrefix, final String message, final Map<String, String> headers) {
        this.error(DEFAULT_LOGGER_NAME, logPrefix, message, headers);
    }

    @Override
    public void error(final String loggerName, final String logPrefix, final String message, final Map<String, String> headers) {
        CoreLoggerFactory.getInstance().getLogger(loggerName).error("{} {} {}", logPrefix, message,
                this.headersToLog.createStandardLabelsFromMap(headers));
    }

    @Override
    public void error(final String logPrefix, final String message, final Exception e,
                      final Map<String, String> headers) {
        this.error(DEFAULT_LOGGER_NAME, logPrefix, message, e, headers);
    }

    @Override
    public void error(final String loggerName, final String logPrefix, final String message, final Exception ex, final Map<String, String> headers) {
        CoreLoggerFactory.getInstance().getLogger(loggerName).error("{} {} {}", logPrefix, message,
                this.headersToLog.createStandardLabelsFromMap(headers), ex);
    }

    @Override
    public void close() throws Exception {
        // do nothing
    }
}
