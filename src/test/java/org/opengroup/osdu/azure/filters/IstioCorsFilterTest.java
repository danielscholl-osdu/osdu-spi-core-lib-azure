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

package org.opengroup.osdu.azure.filters;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import jakarta.servlet.FilterChain;

/**
 * Tests for {@link IstioCorsFilter}
 */
public class IstioCorsFilterTest {

    /**
     * Tests for a valid CORS and Preflight request.
     */
    @Test
    public void testdoFilter_Success() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://www.apache.org");
        request.setMethod("OPTIONS");
        request.addHeader("Access-Control-Request-Method", "OPTIONS");

        IstioCorsFilter istioCorsFilter = new IstioCorsFilter();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        istioCorsFilter.doFilter(request, response, filterChain);

        assertTrue(response.getHeader("Access-Control-Allow-Origin")==null);
        assertTrue(response.getHeader("Access-Control-Allow-Methods")== null);
        assertTrue(response.getHeader("Access-Control-Max-Age")==null);
        assertTrue(response.getHeader("Access-Control-Allow-Headers")==null);
        assertTrue(response.getHeader("Access-Control-Expose-Headers")==null);
        assertTrue(response.getStatus()==200);
    }

    /**
     * Tests for a non-CORS request.
     */
    @ExtendWith(MockitoExtension.class)
    @Test
    public void testdoFilter_NonCORSRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        request.addHeader("Origin", "https://www.apache.org");
        request.setMethod("GET");
        request.addHeader("Access-Control-Request-Method", "GET");
        IstioCorsFilter istioCorsFilter = new IstioCorsFilter();

        istioCorsFilter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    /**
     * Tests if a GET request is treated as simple request; Not a Preflight request.
     */
    @Test
    public void testisPreFlightRequest_SimpleGET() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        request.setMethod("GET");
        request.addHeader("Origin", "https://www.apache.org");

        IstioCorsFilter istioCorsFilter = new IstioCorsFilter();

        istioCorsFilter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    /**
     * Tests if request origin is null; It is not a Preflight request.
     */
    @Test
    public void testisPreFlightRequest_OriginNull() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        request.setMethod("OPTIONS");

        IstioCorsFilter istioCorsFilter = new IstioCorsFilter();

        istioCorsFilter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    /**
     * Tests if request is valid or not for invalid origin (origin = null)
     */
    @Test
    public void testisCorsRequest_OriginNull() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        request.setMethod("OPTIONS");

        IstioCorsFilter istioCorsFilter = new IstioCorsFilter();

        istioCorsFilter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    /**
     * Tests if request is valid or not for a valid origin
     */
    @Test
    public void testisCorsRequest_ValidOrigin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        request.addHeader("Origin", "https://www.apache.org");
        request.setMethod("OPTIONS");

        IstioCorsFilter istioCorsFilter = new IstioCorsFilter();

        istioCorsFilter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

}
