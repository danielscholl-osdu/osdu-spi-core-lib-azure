package org.opengroup.osdu.azure.util;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ChainedTokenCredential;
import com.azure.identity.ChainedTokenCredentialBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.AppException;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for local development authentication in AzureServicePrincipal.
 */
@ExtendWith(MockitoExtension.class)
public class AzureServicePrincipalLocalDevTest {

    private AzureServicePrincipal azureServicePrincipal;
    private AzureServicePrincipal spyServicePrincipal;

    @Mock
    private ChainedTokenCredentialBuilder mockChainedCredentialBuilder;

    @Mock
    private ChainedTokenCredential mockChainedCredential;

    @Mock
    private AccessToken mockAccessToken;

    @BeforeEach
    public void setup() {
        azureServicePrincipal = new AzureServicePrincipal();
    }
    
    private void setupSpyWithMocks() {
        spyServicePrincipal = spy(azureServicePrincipal);
        
        // Setup mock builder to return mock credential
        when(mockChainedCredentialBuilder.build()).thenReturn(mockChainedCredential);
        
        // Make the spy return our mocked builder
        doReturn(mockChainedCredentialBuilder).when(spyServicePrincipal).createLocalChainedTokenCredentialBuilder();
    }

    @Test
    public void testGetLocalDevToken_Success() {
        // Arrange
        setupSpyWithMocks();
        String scope = "https://management.azure.com/.default";
        String expectedToken = "mock-token-value";
        
        when(mockAccessToken.getToken()).thenReturn(expectedToken);
        when(mockChainedCredential.getToken(any(TokenRequestContext.class)))
            .thenReturn(Mono.just(mockAccessToken));
        
        // Act
        String actualToken = spyServicePrincipal.getLocalDevToken(scope);
        
        // Assert
        assertEquals(expectedToken, actualToken);
        
        // Verify the scope was set correctly
        ArgumentCaptor<TokenRequestContext> contextCaptor = ArgumentCaptor.forClass(TokenRequestContext.class);
        verify(mockChainedCredential).getToken(contextCaptor.capture());
        TokenRequestContext capturedContext = contextCaptor.getValue();
        assertTrue(capturedContext.getScopes().contains(scope));
    }

    @Test
    public void testGetLocalDevToken_NullToken() {
        // Arrange
        setupSpyWithMocks();
        String scope = "test-scope";
        
        // Mock the AccessToken to be non-null but getToken() returns null
        when(mockAccessToken.getToken()).thenReturn(null);
        when(mockChainedCredential.getToken(any(TokenRequestContext.class)))
            .thenReturn(Mono.just(mockAccessToken));
        
        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            spyServicePrincipal.getLocalDevToken(scope);
        });
        
        assertEquals(500, exception.getError().getCode());
        assertEquals("Received empty token", exception.getError().getReason());
        assertTrue(exception.getError().getMessage().contains("Local development credential returned null token"));
    }

    @Test
    public void testGetLocalDevToken_NullAccessToken() {
        // Arrange
        setupSpyWithMocks();
        String scope = "test-scope";
        
        // Mock the ChainedTokenCredential to return null AccessToken
        when(mockChainedCredential.getToken(any(TokenRequestContext.class)))
            .thenReturn(Mono.justOrEmpty(null));
        
        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            spyServicePrincipal.getLocalDevToken(scope);
        });
        
        assertEquals(500, exception.getError().getCode());
        assertEquals("Received empty token", exception.getError().getReason());
        assertTrue(exception.getError().getMessage().contains("Local development credential returned null token"));
    }

    @Test
    public void testGetLocalDevToken_ExceptionDuringTokenAcquisition() {
        // Arrange
        setupSpyWithMocks();
        String scope = "test-scope";
        String errorMessage = "Authentication failed";

        when(mockChainedCredential.getToken(any(TokenRequestContext.class)))
            .thenReturn(Mono.error(new RuntimeException(errorMessage)));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            spyServicePrincipal.getLocalDevToken(scope);
        });

        assertEquals(500, exception.getError().getCode());
        assertEquals("Received empty token", exception.getError().getReason());
        // The sanitized message no longer includes the raw SDK exception text to avoid leaking
        // internal details (e.g., filesystem paths) into API responses. The original exception is
        // logged at WARN level for diagnostics.
        assertTrue(exception.getError().getMessage().contains("Local development token acquisition failed"));
        assertFalse(exception.getError().getMessage().contains(errorMessage),
            "Internal error details must not be propagated to the API response");
    }

    @Test
    public void testGetLocalDevToken_ScopeIsForwardedToCredential() {
        // Arrange
        setupSpyWithMocks();
        String scope = "https://management.azure.com/.default";
        String expectedToken = "mock-token-for-scope-forwarding";

        when(mockAccessToken.getToken()).thenReturn(expectedToken);
        when(mockChainedCredential.getToken(any(TokenRequestContext.class)))
            .thenReturn(Mono.just(mockAccessToken));

        // Act
        String token = spyServicePrincipal.getLocalDevToken(scope);

        // Assert
        assertEquals(expectedToken, token);

        // Verify the scope was forwarded correctly to the credential
        ArgumentCaptor<TokenRequestContext> contextCaptor = ArgumentCaptor.forClass(TokenRequestContext.class);
        verify(mockChainedCredential).getToken(contextCaptor.capture());
        assertTrue(contextCaptor.getValue().getScopes().contains(scope));
    }

    @Test
    public void testCreateLocalChainedTokenCredentialBuilder_NotMocked() {
        // This test verifies that the actual implementation returns a non-null builder
        // when not mocked (for coverage of the actual method)
        ChainedTokenCredentialBuilder actualBuilder = azureServicePrincipal.createLocalChainedTokenCredentialBuilder();
        assertNotNull(actualBuilder);
    }
}
