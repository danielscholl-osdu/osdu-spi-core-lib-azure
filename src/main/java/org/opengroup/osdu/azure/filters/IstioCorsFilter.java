package org.opengroup.osdu.azure.filters;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.FilterChain;
import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.web.cors.CorsUtils.isCorsRequest;
import static org.springframework.web.cors.CorsUtils.isPreFlightRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

    /**
     * For Implement istio CORS policy.
     */
@Component
@ConditionalOnProperty(value = "azure.istio.auth.enabled", havingValue = "true", matchIfMissing = false)
@Order(Integer.MIN_VALUE)
public final class IstioCorsFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(IstioCorsFilter.class);
    private static final String LOGGER_NAME = "CORSLogger";

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response,
                         final FilterChain chain) throws IOException, ServletException {

        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;

        //Only execute CORS policy if request is cross-origin and a Preflight request
        if (isCorsRequest(httpRequest) && isPreFlightRequest(httpRequest)) {
            Map<String, String> responseHeaders = getCorsHeadersWithNullValues();
            for (Map.Entry<String, String> header : responseHeaders.entrySet()) {
                httpResponse.addHeader(header.getKey(), header.getValue());
            }
            LOGGER.info(LOGGER_NAME + ": core-lib-azure sets response headers to null successfully");
            //For HTTP OPTIONS method reply with ACCEPTED status code -- per CORS handshake
            httpResponse.setStatus(HttpStatus.SC_OK);
            return;
        }
        chain.doFilter(httpRequest, httpResponse);
    }

    /**
     * Updates response headers to null and returns control to caller so that istio's CORS is honored.
     * @return response headers
     */
    public Map<String, String> getCorsHeadersWithNullValues() {
        Map<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Access-Control-Allow-Origin", null);
        responseHeaders.put("Access-Control-Allow-Methods", null);
        responseHeaders.put("Access-Control-Max-Age", null);
        responseHeaders.put("Access-Control-Allow-Headers", null);
        responseHeaders.put("Access-Control-Expose-Headers", null);
        return responseHeaders;
    }
    @Override
    public void destroy() {
    }
}

