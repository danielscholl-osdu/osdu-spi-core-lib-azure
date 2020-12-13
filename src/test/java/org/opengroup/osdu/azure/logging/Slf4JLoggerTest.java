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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.logging.audit.AuditPayload;
import org.opengroup.osdu.core.common.model.http.HeadersToLog;
import org.opengroup.osdu.core.common.model.http.Request;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Contains tests for {@link Slf4JLogger}
 */
@ExtendWith(MockitoExtension.class)
public class Slf4JLoggerTest {
    private static final String LOG_PREFIX = "services.app";
    private static final String DEFAULT_LOGGER_NAME = Slf4JLogger.class.getName();
    private static final String LOGGER_NAME = Slf4JLoggerTest.class.getName();
    private static final String LOG_MESSAGE = "Hello world !";

    @Mock
    private HeadersToLog headersToLog;

    @Mock
    private AuditPayload auditPayload;

    @Mock
    private CoreLoggerFactory coreLoggerFactory;

    @Mock
    private Request request;

    @Mock
    private CoreLogger coreLogger;

    @Mock
    private Exception e;

    private final Map<String, String> headers = new HashMap<>();

    @InjectMocks
    Slf4JLogger slf4JLogger;

    /**
     * Workaround for inability to mock static methods like getInstance().
     *
     * @param mock CoreLoggerFactory mock instance
     */
    private void mockSingleton(CoreLoggerFactory mock) {
        try {
            Field instance = CoreLoggerFactory.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(instance, mock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reset workaround for inability to mock static methods like getInstance().
     */
    private void resetSingleton() {
        try {
            Field instance = CoreLoggerFactory.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    public void setup() {
        mockSingleton(coreLoggerFactory);
        when(coreLoggerFactory.getLogger(anyString())).thenReturn(coreLogger);
        when(headersToLog.createStandardLabelsFromMap(eq(headers))).thenReturn(headers);
    }

    @AfterEach
    public void takeDown() {
        resetSingleton();
    }

    @Test
    public void testAudit() {
        doNothing().when(coreLogger).info(eq("{} {} {}"), eq(LOG_PREFIX), eq(auditPayload), eq(headers));
        slf4JLogger.audit(LOG_PREFIX, auditPayload, headers);
        verify(coreLogger, times(1)).info(eq("{} {} {}"), eq(LOG_PREFIX), eq(auditPayload), eq(headers));
        verify(coreLoggerFactory, times(1)).getLogger(eq(DEFAULT_LOGGER_NAME));
        verify(headersToLog, times(1)).createStandardLabelsFromMap(eq(headers));
    }

    @Test
    public void testAuditWithLoggerName() {
        doNothing().when(coreLogger).info(eq("{} {} {}"), eq(LOG_PREFIX), eq(auditPayload), eq(headers));
        slf4JLogger.audit(LOGGER_NAME, LOG_PREFIX, auditPayload, headers);
        verify(coreLogger, times(1)).info(eq("{} {} {}"), eq(LOG_PREFIX), eq(auditPayload), eq(headers));
        verify(coreLoggerFactory, times(1)).getLogger(eq(LOGGER_NAME));
        verify(headersToLog, times(1)).createStandardLabelsFromMap(eq(headers));
    }

    @Test
    public void testRequest() {
        doNothing().when(coreLogger).info(eq("{} {} {}"), eq(LOG_PREFIX), eq(request), eq(headers));
        slf4JLogger.request(LOG_PREFIX, request, headers);
        verify(coreLogger, times(1)).info(eq("{} {} {}"), eq(LOG_PREFIX), eq(request), eq(headers));
        verify(coreLoggerFactory, times(1)).getLogger(eq(DEFAULT_LOGGER_NAME));
        verify(headersToLog, times(1)).createStandardLabelsFromMap(eq(headers));
    }

    @Test
    public void testRequestWithLoggerName() {
        doNothing().when(coreLogger).info(eq("{} {} {}"), eq(LOG_PREFIX), eq(request), eq(headers));
        slf4JLogger.request(LOGGER_NAME, LOG_PREFIX, request, headers);
        verify(coreLogger, times(1)).info(eq("{} {} {}"), eq(LOG_PREFIX), eq(request), eq(headers));
        verify(coreLoggerFactory, times(1)).getLogger(eq(LOGGER_NAME));
        verify(headersToLog, times(1)).createStandardLabelsFromMap(eq(headers));
    }

    @Test
    public void testInfo() {
        doNothing().when(coreLogger).info(eq("{} {} {}"), eq(LOG_PREFIX), eq(LOG_MESSAGE), eq(headers));
        slf4JLogger.info(LOG_PREFIX, LOG_MESSAGE, headers);
        verify(coreLogger, times(1)).info(eq("{} {} {}"), eq(LOG_PREFIX), eq(LOG_MESSAGE), eq(headers));
        verify(coreLoggerFactory, times(1)).getLogger(eq(DEFAULT_LOGGER_NAME));
        verify(headersToLog, times(1)).createStandardLabelsFromMap(eq(headers));
    }

    @Test
    public void testInfoWithLoggerName() {
        doNothing().when(coreLogger).info(eq("{} {} {}"), eq(LOG_PREFIX), eq(LOG_MESSAGE), eq(headers));
        slf4JLogger.info(LOGGER_NAME, LOG_PREFIX, LOG_MESSAGE, headers);
        verify(coreLogger, times(1)).info(eq("{} {} {}"), eq(LOG_PREFIX), eq(LOG_MESSAGE), eq(headers));
        verify(coreLoggerFactory, times(1)).getLogger(eq(LOGGER_NAME));
        verify(headersToLog, times(1)).createStandardLabelsFromMap(eq(headers));
    }

    @Test
    public void testWarning() {
        doNothing().when(coreLogger).warn(eq("{} {} {}"), eq(LOG_PREFIX), eq(LOG_MESSAGE), eq(headers));
        slf4JLogger.warning(LOG_PREFIX, LOG_MESSAGE, headers);
        verify(coreLogger, times(1)).warn(eq("{} {} {}"), eq(LOG_PREFIX), eq(LOG_MESSAGE), eq(headers));
        verify(coreLoggerFactory, times(1)).getLogger(eq(DEFAULT_LOGGER_NAME));
        verify(headersToLog, times(1)).createStandardLabelsFromMap(eq(headers));
    }

    @Test
    public void testWarningWithLoggerName() {
        doNothing().when(coreLogger).warn(eq("{} {} {}"), eq(LOG_PREFIX), eq(LOG_MESSAGE), eq(headers));
        slf4JLogger.warning(LOGGER_NAME, LOG_PREFIX, LOG_MESSAGE, headers);
        verify(coreLogger, times(1)).warn(eq("{} {} {}"), eq(LOG_PREFIX), eq(LOG_MESSAGE), eq(headers));
        verify(coreLoggerFactory, times(1)).getLogger(eq(LOGGER_NAME));
        verify(headersToLog, times(1)).createStandardLabelsFromMap(eq(headers));
    }

    @Test
    public void testWarningWithException() {
        doNothing().when(coreLogger).warn(eq("{} {} {}"), eq(LOG_PREFIX), eq(LOG_MESSAGE), eq(headers), eq(e));
        slf4JLogger.warning(LOG_PREFIX, LOG_MESSAGE, e, headers);
        verify(coreLogger, times(1)).warn(eq("{} {} {}"), eq(LOG_PREFIX), eq(LOG_MESSAGE), eq(headers), eq(e));
        verify(coreLoggerFactory, times(1)).getLogger(eq(DEFAULT_LOGGER_NAME));
        verify(headersToLog, times(1)).createStandardLabelsFromMap(eq(headers));
    }

    @Test
    public void testWarningWithExceptionAndLoggerName() {
        doNothing().when(coreLogger).warn(eq("{} {} {}"), eq(LOG_PREFIX), eq(LOG_MESSAGE), eq(headers), eq(e));
        slf4JLogger.warning(LOGGER_NAME, LOG_PREFIX, LOG_MESSAGE, e, headers);
        verify(coreLogger, times(1)).warn(eq("{} {} {}"), eq(LOG_PREFIX), eq(LOG_MESSAGE), eq(headers), eq(e));
        verify(coreLoggerFactory, times(1)).getLogger(eq(LOGGER_NAME));
        verify(headersToLog, times(1)).createStandardLabelsFromMap(eq(headers));
    }

    @Test
    public void testError() {
        doNothing().when(coreLogger).error(eq("{} {} {}"), eq(LOG_PREFIX), eq(LOG_MESSAGE), eq(headers));
        slf4JLogger.error(LOG_PREFIX, LOG_MESSAGE, headers);
        verify(coreLogger, times(1)).error(eq("{} {} {}"), eq(LOG_PREFIX), eq(LOG_MESSAGE), eq(headers));
        verify(coreLoggerFactory, times(1)).getLogger(eq(DEFAULT_LOGGER_NAME));
        verify(headersToLog, times(1)).createStandardLabelsFromMap(eq(headers));
    }

    @Test
    public void testErrorWithLoggerName() {
        doNothing().when(coreLogger).error(eq("{} {} {}"), eq(LOG_PREFIX), eq(LOG_MESSAGE), eq(headers));
        slf4JLogger.error(LOGGER_NAME, LOG_PREFIX, LOG_MESSAGE, headers);
        verify(coreLogger, times(1)).error(eq("{} {} {}"), eq(LOG_PREFIX), eq(LOG_MESSAGE), eq(headers));
        verify(coreLoggerFactory, times(1)).getLogger(eq(LOGGER_NAME));
        verify(headersToLog, times(1)).createStandardLabelsFromMap(eq(headers));
    }

    @Test
    public void testErrorWithException() {
        doNothing().when(coreLogger).error(eq("{} {} {}"), eq(LOG_PREFIX), eq(LOG_MESSAGE), eq(headers), eq(e));
        slf4JLogger.error(LOG_PREFIX, LOG_MESSAGE, e, headers);
        verify(coreLogger, times(1)).error(eq("{} {} {}"), eq(LOG_PREFIX), eq(LOG_MESSAGE), eq(headers), eq(e));
        verify(coreLoggerFactory, times(1)).getLogger(eq(DEFAULT_LOGGER_NAME));
        verify(headersToLog, times(1)).createStandardLabelsFromMap(eq(headers));
    }

    @Test
    public void testErrorWithExceptionAndLoggerName() {
        doNothing().when(coreLogger).error(eq("{} {} {}"), eq(LOG_PREFIX), eq(LOG_MESSAGE), eq(headers), eq(e));
        slf4JLogger.error(LOGGER_NAME, LOG_PREFIX, LOG_MESSAGE, e, headers);
        verify(coreLogger, times(1)).error(eq("{} {} {}"), eq(LOG_PREFIX), eq(LOG_MESSAGE), eq(headers), eq(e));
        verify(coreLoggerFactory, times(1)).getLogger(eq(LOGGER_NAME));
        verify(headersToLog, times(1)).createStandardLabelsFromMap(eq(headers));
    }
}
