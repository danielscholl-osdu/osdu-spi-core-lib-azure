package org.opengroup.osdu.azure.util;

import com.azure.security.keyvault.secrets.SecretClient;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.cache.AzureServicePrincipleTokenServiceCache;
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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Mock
    private AzureServicePrincipleTokenServiceCache tokenCache;

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
        // Inject a clean env reader so tests are isolated from host environment variables.
        // Individual tests that need a specific indicator can override this via setEnvReader().
        ReflectionTestUtils.setField(tokenService, "envReader", (Function<String, String>) k -> null);
    }

    /** Helper: inject an env reader that returns a non-null value for one indicator. */
    private void setEnvReader(final String indicatorName) {
        Map<String, String> fakeEnv = new HashMap<>();
        fakeEnv.put(indicatorName, "true");
        ReflectionTestUtils.setField(tokenService, "envReader", (Function<String, String>) fakeEnv::get);
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
        verify(azureServicePrincipal, never()).getWIToken(anyString(), anyString());
        verify(azureServicePrincipal, never()).getMSIToken(anyString());
        verify(azureServicePrincipal, never()).getIdToken(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void testGetAuthorizationToken_LocalProfileInactive_UsesWorkloadIdentity() {
        // Arrange
        String testToken = createTestJwtToken();
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
        when(workloadIdentityConfiguration.getIsEnabled()).thenReturn(true);
        when(azureServicePrincipal.getWIToken(eq(AAD_CLIENT_ID), anyString()))
            .thenReturn(testToken);

        // Act
        String token = tokenService.getAuthorizationToken();

        // Assert
        assertNotNull(token);
        assertEquals(testToken, token);

        // Verify workload identity method was called
        verify(azureServicePrincipal).getWIToken(eq(AAD_CLIENT_ID), anyString());

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
        when(azureServicePrincipal.getMSIToken(anyString()))
            .thenReturn(testToken);

        // Act
        String token = tokenService.getAuthorizationToken();

        // Assert
        assertNotNull(token);
        assertEquals(testToken, token);

        // Verify MSI method was called
        verify(azureServicePrincipal).getMSIToken(anyString());

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
        when(azureServicePrincipal.getWIToken(eq(AAD_CLIENT_ID), anyString()))
            .thenReturn(testToken);

        // Act
        String token = tokenService.getAuthorizationToken();

        // Assert
        assertNotNull(token);
        assertEquals(testToken, token);

        // Verify fallback to workload identity
        verify(azureServicePrincipal).getWIToken(eq(AAD_CLIENT_ID), anyString());

        // Verify local dev method was not called
        verify(azureServicePrincipal, never()).getLocalDevToken(anyString());
    }

    // -------------------------------------------------------------------------
    // assertNotCloudEnvironment() safety checks
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {
        "KUBERNETES_SERVICE_HOST",
        "WEBSITE_INSTANCE_ID",
        "IDENTITY_ENDPOINT",
        "MSI_ENDPOINT",
        "CONTAINER_APP_NAME",
        "FUNCTIONS_WORKER_RUNTIME",
        "ACI_INSTANCE_NAME",
        "CI",
        "CONTINUOUS_INTEGRATION"
    })
    public void testGetAuthorizationToken_CloudIndicatorSet_ThrowsBeforeTokenAcquisition(
            final String cloudIndicator) {
        // Arrange — simulate local profile active with credentials enabled, but a cloud indicator is set
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);
        when(localDevelopmentConfiguration.getEnableLocalCredentials()).thenReturn(true);
        setEnvReader(cloudIndicator);

        // Act & Assert — security check must throw BEFORE any token is acquired
        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> tokenService.getAuthorizationToken());

        assertTrue(ex.getMessage().contains(cloudIndicator),
            "Exception message should identify the triggering indicator");

        // Verify no token acquisition was attempted
        verify(azureServicePrincipal, never()).getLocalDevToken(anyString());
    }
}
