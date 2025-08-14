package org.opengroup.osdu.azure.util;

import com.azure.security.keyvault.secrets.SecretClient;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.di.AzureActiveDirectoryConfiguration;
import org.opengroup.osdu.azure.di.LocalDevelopmentConfiguration;
import org.opengroup.osdu.azure.di.PodIdentityConfiguration;
import org.opengroup.osdu.azure.di.WorkloadIdentityConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for local development support in AzureServicePrincipleTokenService.
 */
@ExtendWith(MockitoExtension.class)
public class AzureServicePrincipleTokenServiceLocalDevTest {

    @Mock
    private Environment environment;

    @Mock
    private LocalDevelopmentConfiguration localDevelopmentConfiguration;

    @Mock
    private WorkloadIdentityConfiguration workloadIdentityConfiguration;

    @Mock
    private PodIdentityConfiguration podIdentityConfiguration;

    @Mock
    private AzureActiveDirectoryConfiguration aadConfiguration;

    @Mock
    private SecretClient secretClient;

    @Mock
    private AzureServicePrincipal azureServicePrincipal;

    @InjectMocks
    private AzureServicePrincipleTokenService tokenService;

    private static final String AAD_CLIENT_ID = "test-aad-client-id";

    /**
     * Creates a test JWT token with a valid structure and far-future expiration.
     * This approach avoids hard-coded tokens that may be flagged by secret scanning tools.
     */
    private String createTestJwtToken() {
        // Create expiration time far in the future (1 year from now)
        Date expirationDate = Date.from(Instant.now().plus(365, ChronoUnit.DAYS));
        
        // Use a test-only algorithm with a dummy secret
        Algorithm algorithm = Algorithm.HMAC256("test-secret-for-unit-tests-only");
        
        return JWT.create()
                .withSubject("1234567890")
                .withClaim("name", "Test User")
                .withIssuedAt(new Date())
                .withExpiresAt(expirationDate)
                .sign(algorithm);
    }

    @BeforeEach
    public void setup() {
        // Set up the mocked AzureServicePrincipal
        ReflectionTestUtils.setField(tokenService, "azureServicePrincipal", azureServicePrincipal);
        ReflectionTestUtils.setField(tokenService, "aadClientId", AAD_CLIENT_ID);
    }

    @Test
    public void testGetAuthorizationToken_LocalProfileActive_UsesLocalCredentials() {
        // Arrange
        String testToken = createTestJwtToken();
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);
        when(localDevelopmentConfiguration.getEnableLocalCredentials()).thenReturn(true);
        when(azureServicePrincipal.getLocalDevToken(anyString()))
            .thenReturn(testToken);

        // Act
        String token = tokenService.getAuthorizationToken();

        // Assert
        assertNotNull(token);
        assertEquals(testToken, token);
        
        // Verify local dev token method was called
        verify(azureServicePrincipal).getLocalDevToken(anyString());
        
        // Verify other methods were not called
        verify(azureServicePrincipal, never()).getWIToken(anyString());
        verify(azureServicePrincipal, never()).getMSIToken();
        verify(azureServicePrincipal, never()).getIdToken(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void testGetAuthorizationToken_LocalProfileInactive_UsesWorkloadIdentity() {
        // Arrange
        String testToken = createTestJwtToken();
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
        when(workloadIdentityConfiguration.getIsEnabled()).thenReturn(true);
        when(azureServicePrincipal.getWIToken(eq(AAD_CLIENT_ID)))
            .thenReturn(testToken);

        // Act
        String token = tokenService.getAuthorizationToken();

        // Assert
        assertNotNull(token);
        assertEquals(testToken, token);
        
        // Verify workload identity method was called
        verify(azureServicePrincipal).getWIToken(eq(AAD_CLIENT_ID));
        
        // Verify local dev method was not called
        verify(azureServicePrincipal, never()).getLocalDevToken(anyString());
    }

    @Test
    public void testGetAuthorizationToken_LocalProfileActiveButDisabled_UsesFallback() {
        // Arrange
        String testToken = createTestJwtToken();
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);
        when(localDevelopmentConfiguration.getEnableLocalCredentials()).thenReturn(false);
        when(workloadIdentityConfiguration.getIsEnabled()).thenReturn(false);
        when(podIdentityConfiguration.getIsEnabled()).thenReturn(true);
        when(azureServicePrincipal.getMSIToken())
            .thenReturn(testToken);

        // Act
        String token = tokenService.getAuthorizationToken();

        // Assert
        assertNotNull(token);
        assertEquals(testToken, token);
        
        // Verify MSI method was called
        verify(azureServicePrincipal).getMSIToken();
        
        // Verify local dev method was not called
        verify(azureServicePrincipal, never()).getLocalDevToken(anyString());
    }

    @Test
    public void testGetAuthorizationToken_LocalConfigurationNull_UsesFallback() {
        // Arrange
        String testToken = createTestJwtToken();
        ReflectionTestUtils.setField(tokenService, "localDevelopmentConfiguration", null);
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);
        when(workloadIdentityConfiguration.getIsEnabled()).thenReturn(true);
        when(azureServicePrincipal.getWIToken(eq(AAD_CLIENT_ID)))
            .thenReturn(testToken);

        // Act
        String token = tokenService.getAuthorizationToken();

        // Assert
        assertNotNull(token);
        assertEquals(testToken, token);
        
        // Verify fallback to workload identity
        verify(azureServicePrincipal).getWIToken(eq(AAD_CLIENT_ID));
        
        // Verify local dev method was not called
        verify(azureServicePrincipal, never()).getLocalDevToken(anyString());
    }
}
