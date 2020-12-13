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
        coreLogger = mock(CoreLogger.class, withSettings().useConstructor(spySlf4jLogger, telemetryClient).defaultAnswer(CALLS_REAL_METHODS));
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
                .build();
        coreLogger.logAudit(payload);
        verify(spySlf4jLogger).info("{}", payload);
    }

    @Test
    public void testLogDependency() {
        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry("DependencyName", "Dependency/Command/Name", new com.microsoft.applicationinsights.telemetry.Duration((long) 1000), true);
        telemetry.setResultCode("200");
        telemetry.setType("HTTP");
        telemetry.setTarget("Dependency/Command/Name");

        final ArgumentCaptor<RemoteDependencyTelemetry> telemetryCaptor = ArgumentCaptor.forClass(RemoteDependencyTelemetry.class);
        doNothing().when(telemetryClient).trackDependency(telemetryCaptor.capture());

        DependencyPayload payload = new DependencyPayload("DependencyName", "Dependency/Command/Name", Duration.ofMillis(1000), "200", true);
        coreLogger.logDependency(payload);

        assertEquals(1, telemetryCaptor.getAllValues().size());
        assertEquals("DependencyName", telemetryCaptor.getAllValues().get(0).getName());
        assertEquals("Dependency/Command/Name", telemetryCaptor.getAllValues().get(0).getCommandName());
        assertEquals("HTTP", telemetryCaptor.getAllValues().get(0).getType());
        assertEquals("Dependency/Command/Name", telemetryCaptor.getAllValues().get(0).getTarget());
        assertEquals(1000, telemetryCaptor.getAllValues().get(0).getDuration().getTotalMilliseconds());
        assertEquals("200", telemetryCaptor.getAllValues().get(0).getResultCode());
        assertEquals(true, telemetryCaptor.getAllValues().get(0).getSuccess());
    }
}
