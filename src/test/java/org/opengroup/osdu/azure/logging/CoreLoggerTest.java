package org.opengroup.osdu.azure.logging;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.logging.audit.AuditAction;
import org.opengroup.osdu.core.common.logging.audit.AuditPayload;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Contains tests for {@link CoreLogger}
 */
@ExtendWith(MockitoExtension.class)
public class CoreLoggerTest {
    private static final String LOGGER_NAME = CoreLoggerTest.class.getName();
    private static final String LOG_MESSAGE = "Hello logger test";
    private static final String EXCEPTION_MESSAGE = "Logger exception message";
    private static final String ARG1 = "Argument 1 value";
    private static final String ARG2 = "Argument 2 value";
    private static final String ARG3 = "Argument 3 value";

    private static Logger spySlf4jLogger;
    private static TelemetryClient telemetryClient;
    private static CoreLogger coreLogger;

    @BeforeAll
    public static void setup() {
        spySlf4jLogger = spy(LoggerFactory.getLogger(LOGGER_NAME));
        telemetryClient = mock(TelemetryClient.class);
        coreLogger = mock(CoreLogger.class, withSettings().useConstructor(spySlf4jLogger).defaultAnswer(CALLS_REAL_METHODS));
    }

    @Test
    public void testInfoWithMessage() {
        coreLogger.info(LOG_MESSAGE);
        verify(spySlf4jLogger).info(LOG_MESSAGE);
    }

    @Test
    public void testInfoWithArguments() {
        coreLogger.info("{} {} {}", ARG1, ARG2, ARG3);
        verify(spySlf4jLogger).info("{} {} {}", ARG1, ARG2, ARG3);
    }

    @Test
    public void testDebugWithMessage() {
        coreLogger.debug(LOG_MESSAGE);
        verify(spySlf4jLogger).debug(LOG_MESSAGE);
    }

    @Test
    public void testDebugWithArguments() {
        coreLogger.debug("{} {} {}", ARG1, ARG2, ARG3);
        verify(spySlf4jLogger).debug("{} {} {}", ARG1, ARG2, ARG3);
    }

    @Test
    public void testWarnWithMessage() {
        coreLogger.warn(LOG_MESSAGE);
        verify(spySlf4jLogger).warn(LOG_MESSAGE);
    }

    @Test
    public void testWarnWithArguments() {
        coreLogger.warn("{} {} {}", ARG1, ARG2, ARG3);
        verify(spySlf4jLogger).warn("{} {} {}", ARG1, ARG2, ARG3);
    }

    @Test
    public void testWarnWithThrowable() {
        Exception e = new Exception(EXCEPTION_MESSAGE);

        coreLogger.warn(LOG_MESSAGE, e);
        verify(spySlf4jLogger).warn(LOG_MESSAGE, e);
    }

    @Test
    public void testErrorWithMessage() {
        coreLogger.error(LOG_MESSAGE);
        verify(spySlf4jLogger).error(LOG_MESSAGE);
    }

    @Test
    public void testErrorWithArguments() {
        coreLogger.error("{} {} {}", ARG1, ARG2, ARG3);
        verify(spySlf4jLogger).error("{} {} {}", ARG1, ARG2, ARG3);
    }

    @Test
    public void testErrorWithThrowable() {
        Exception e = new Exception(EXCEPTION_MESSAGE);

        coreLogger.error(LOG_MESSAGE, e);
        verify(spySlf4jLogger).error(LOG_MESSAGE, e);
    }

    @Test
    public void testLogAudit() {
        List<String> resource = new ArrayList<>();
        resource.add("TestResource");
        AuditPayload payload = AuditPayload.builder()
                .action(AuditAction.READ)
                .status(AuditStatus.FAILURE)
                .actionId("TestAuditId")
                .message("TestAuditMessage")
                .resources(resource)
                .user("TestUser")
                .userIpAddress("127.0.0.1")
                .userAgent("TestAgent")
                .requiredGroupsForAction(new ArrayList<>())
                .userAuthorizedGroupName("")
                .build();
        coreLogger.logAudit(payload);
        verify(spySlf4jLogger).info("{}", payload);
    }

}
