package org.opengroup.osdu.azure.util;

import com.azure.security.keyvault.secrets.SecretClient;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.opengroup.osdu.azure.cache.AzureServicePrincipleTokenServiceCache;
import org.opengroup.osdu.azure.di.AzureActiveDirectoryConfiguration;
import org.opengroup.osdu.azure.di.PodIdentityConfiguration;
import org.opengroup.osdu.azure.di.WorkloadIdentityConfiguration;

import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.IdToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AzureServicePrincipleTokenServiceTest {

    private static final String clientSecret = "client-secret";
    private static final String tokenCacheKey = "token";

    @Mock
    private PodIdentityConfiguration podIdentityConfiguration;

    @Mock
    private AzureActiveDirectoryConfiguration aadConfiguration;

    @Mock
    private SecretClient sc;

    @Mock
    private AzureServicePrincipal azureServicePrincipal;

    @Mock
    private AzureServicePrincipleTokenServiceCache tokenCache;

    @Mock
    private WorkloadIdentityConfiguration workloadIdentityConfiguration;

    @InjectMocks
    private AzureServicePrincipleTokenService azureServicePrincipleTokenService;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetAuthorizationToken_Error() {
        when(podIdentityConfiguration.getIsEnabled()).thenReturn(false);
        when(workloadIdentityConfiguration.getIsEnabled()).thenReturn(false);
        when(this.azureServicePrincipal.getIdToken(
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )).thenThrow(new AppException(HttpStatus.SC_NOT_FOUND, "error", "message"));

        assertThrows(AppException.class, () -> azureServicePrincipleTokenService.getAuthorizationToken());
    }

    @Test
    public void should_throw_AppException_if_both_client_secret_equals() {
        when(podIdentityConfiguration.getIsEnabled()).thenReturn(false);
        when(workloadIdentityConfiguration.getIsEnabled()).thenReturn(false);
        when(aadConfiguration.getClientSecret()).thenReturn(clientSecret);
        when(azureServicePrincipal.getIdToken(
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )).thenThrow(new AppException(HttpStatus.SC_UNAUTHORIZED, "error", "message"));

        assertThrows(AppException.class, () -> azureServicePrincipleTokenService.getAuthorizationToken());
    }

    @Test
    public void should_return_cached_token_when_not_expiring() {
        // 10 minutes in the future from the current system time to simulate a valid token that is not expiring soon.
        final long expirationTimeForValidToken = System.currentTimeMillis() + 10 * 60 * 1000;
        final String cachedTokenValue = "cached-token";

        IdToken cachedToken = IdToken.builder()
            .tokenValue(cachedTokenValue)
            .expirationTimeMillis(expirationTimeForValidToken)
            .build();
        
        // Mock JWT.decode for any token
        DecodedJWT mockJwt = Mockito.mock(DecodedJWT.class);
        when(mockJwt.getExpiresAt()).thenReturn(new java.util.Date(expirationTimeForValidToken));

        try (var jwtMockedStatic = mockStatic(JWT.class)) {
            // Mock JWT.decode for ANY string to be safe
            jwtMockedStatic.when(() -> JWT.decode(anyString())).thenReturn(mockJwt);
            
            when(tokenCache.get(anyString())).thenReturn(cachedToken);

            String token = azureServicePrincipleTokenService.getAuthorizationToken();
            assertEquals(cachedTokenValue, token);
        }
    }

    @Test
    public void should_refresh_token_when_expired() throws Exception {
        // 5 minutes in the past from the current system time to simulate an expired token.
        final long expirationTimeForExpiredToken = System.currentTimeMillis() - 5 * 60 * 1000;

        // 1 hour in future from the current system time to simulate a new valid token that is not expiring soon.
        final long newExpirationForValidToken = System.currentTimeMillis() + 3600 * 1000;
        final String newTokenValue = "new-token";
        final String expiredTokenValue = "expired-token";

        // Set required fields for the service principal (reflection, since they are injected in prod)
        Field aadClientIdField = AzureServicePrincipleTokenService.class.getDeclaredField("aadClientId");
        aadClientIdField.setAccessible(true);
        aadClientIdField.set(azureServicePrincipleTokenService, "test-aad-client-id");

        IdToken expiredToken = IdToken.builder()
            .tokenValue(expiredTokenValue)
            .expirationTimeMillis(expirationTimeForExpiredToken)
            .build();

        // Mock JWT.decode to return a DecodedJWT with the desired expiration
        DecodedJWT mockJwt = Mockito.mock(DecodedJWT.class);
        when(mockJwt.getExpiresAt()).thenReturn(new java.util.Date(newExpirationForValidToken));

        // Set up the static mock FIRST and keep it open for the entire test
        try (var jwtMockedStatic = mockStatic(JWT.class)) {
            // Key fix: Mock JWT.decode for the specific token that will be returned
            jwtMockedStatic.when(() -> JWT.decode(newTokenValue)).thenReturn(mockJwt);
            
            // Set up other mocks
            when(this.tokenCache.get(anyString())).thenReturn(expiredToken); // Use anyString for cache key
            when(this.workloadIdentityConfiguration.getIsEnabled()).thenReturn(true);
            when(this.podIdentityConfiguration.getIsEnabled()).thenReturn(false);
            when(this.azureServicePrincipal.getWIToken(anyString(), anyString())).thenReturn(newTokenValue);

            // Call the method
            String token = this.azureServicePrincipleTokenService.getAuthorizationToken();
            assertEquals(newTokenValue, token);

            // Verify cache was updated
            ArgumentCaptor<IdToken> captor = ArgumentCaptor.forClass(IdToken.class);
            verify(tokenCache).put(anyString(), captor.capture()); // Use anyString for cache key
            IdToken cached = captor.getValue();
            assertEquals(newTokenValue, cached.getTokenValue());
            assertEquals(newExpirationForValidToken, cached.getExpirationTimeMillis());

            // For the second call, mock the cache to return the new token
            when(this.tokenCache.get(anyString())).thenReturn(cached);
            
            // Verify that the token is returned from the cache after refresh
            String tokenInCache = azureServicePrincipleTokenService.getAuthorizationToken();
            assertEquals(newTokenValue, tokenInCache);
        }
    }
}