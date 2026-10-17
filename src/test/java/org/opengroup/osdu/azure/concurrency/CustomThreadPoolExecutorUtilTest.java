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

package org.opengroup.osdu.azure.concurrency;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.fail;

public class CustomThreadPoolExecutorUtilTest {

    private String testMDCKey = "testKey";
    private String testMDCValue = "testValue";
    private MockHttpServletRequest mockRequest;
    private RequestAttributes expectedAttributes;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mockRequest= new MockHttpServletRequest();
        expectedAttributes = new ServletRequestAttributes(mockRequest);
    }

    @Test
    public void wrapWithContext_ShouldReturn_RunnableObjectWithCorrectContext() {
        // 1) clear the current context
        RequestContextHolder.resetRequestAttributes();
        MDC.clear();

        // 2) set test context
        RequestContextHolder.setRequestAttributes(expectedAttributes);
        Map<String,String> testMap = new HashMap<String,String>();
        testMap.put(testMDCKey,testMDCValue);
        MDC.setContextMap(testMap);

        // 3) define Runnable object, this is the one which needs to be wrapped with context
        Runnable testTask = () -> {
            // 5) verify that the correct context is present in the task being executed
            String mdcValue = MDC.getCopyOfContextMap().get(testMDCKey);
            RequestAttributes actualAttributes = RequestContextHolder.getRequestAttributes();
            Assertions.assertEquals(testMDCValue,mdcValue);
            Assertions.assertEquals(expectedAttributes,actualAttributes);
        };

        // 4) Call executor service to execute the testTask
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future result = executorService.submit(CustomThreadPoolExecutorUtil.wrapWithContext(testTask,expectedAttributes));

        // 6) Fail the test in case of exception thrown in testTask explicitly because
        //  if the Assertions fail within 'testTask', the failure is logged but the test does not fail.
        try {
            result.get();
        }
        catch (Exception e) {
            fail(e);
        }
    }
}
