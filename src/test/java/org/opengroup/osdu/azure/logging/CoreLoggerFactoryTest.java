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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class CoreLoggerFactoryTest {

    @Test
    public void should_have_coreLoggerType_when_applicationInsightsEnabledFlagIsFalse() {
        System.setProperty("APPINSIGHTS_LOGGING_ENABLED", "false");
        CoreLoggerFactory.resetFactory();
        LoggerType loggerType = CoreLoggerFactory.getInstance().getEnabledLoggerType();
        assertEquals(LoggerType.CoreLogger, loggerType);
    }

    @Test
    public void should_have_applicationInsightsLoggerType_when_applicationInsightsEnabledFlagIsTrue() {
        System.setProperty("APPINSIGHTS_LOGGING_ENABLED", "true");
        CoreLoggerFactory.resetFactory();
        LoggerType loggerType = CoreLoggerFactory.getInstance().getEnabledLoggerType();
        assertEquals(LoggerType.ApplicationInsightsLogger, loggerType);
    }

    @Test
    public void should_have_applicationInsightsLoggerType_when_EnvironmentVariableNotSet() {
        CoreLoggerFactory.resetFactory();
        LoggerType loggerType = CoreLoggerFactory.getInstance().getEnabledLoggerType();
        assertEquals(LoggerType.ApplicationInsightsLogger, loggerType);
    }

    @Test
    public void should_return_coreLogger_when_applicationInsightsEnabledFlagIsFalse() {
        System.setProperty("APPINSIGHTS_LOGGING_ENABLED", "false");
        CoreLoggerFactory.resetFactory();
        ICoreLogger iCoreLogger = CoreLoggerFactory.getInstance().getLogger("testLogger");
        assertEquals(CoreLogger.class, iCoreLogger.getClass());
    }

    @Test
    public void should_return_applicationInsightsLogger_when_applicationInsightsEnabledFlagIsTrue() {
        System.setProperty("APPINSIGHTS_LOGGING_ENABLED", "true");
        CoreLoggerFactory.resetFactory();
        ICoreLogger iCoreLogger = CoreLoggerFactory.getInstance().getLogger("testLogger");
        assertEquals(ApplicationInsightsLogger.class, iCoreLogger.getClass());
    }

    @Test
    public void should_return_applicationInsightsLogger_when_EnvironmentVariableNotSet() {
        CoreLoggerFactory.resetFactory();
        ICoreLogger iCoreLogger = CoreLoggerFactory.getInstance().getLogger("testLogger");
        assertEquals(ApplicationInsightsLogger.class, iCoreLogger.getClass());
    }

}
