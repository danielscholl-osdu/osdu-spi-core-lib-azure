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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        assertTrue(exception.getError().getMessage().contains("Local development token acquisition failed"));
        assertTrue(exception.getError().getMessage().contains(errorMessage));
    }

    @Test
    public void testGetLocalDevToken_MultipleScopes() {
        // Arrange
        setupSpyWithMocks();
        String scope1 = "https://management.azure.com/.default";
        String scope2 = "https://graph.microsoft.com/.default";
        String expectedToken = "mock-token-for-multiple-scopes";
        
        when(mockAccessToken.getToken()).thenReturn(expectedToken);
        when(mockChainedCredential.getToken(any(TokenRequestContext.class)))
            .thenReturn(Mono.just(mockAccessToken));
        
        // Act - test with first scope
        String token1 = spyServicePrincipal.getLocalDevToken(scope1);
        
        // Act - test with second scope
        String token2 = spyServicePrincipal.getLocalDevToken(scope2);
        
        // Assert
        assertEquals(expectedToken, token1);
        assertEquals(expectedToken, token2);
        
        // Verify both calls were made with correct scopes
        ArgumentCaptor<TokenRequestContext> contextCaptor = ArgumentCaptor.forClass(TokenRequestContext.class);
        verify(mockChainedCredential, times(2)).getToken(contextCaptor.capture());
        
        assertTrue(contextCaptor.getAllValues().get(0).getScopes().contains(scope1));
        assertTrue(contextCaptor.getAllValues().get(1).getScopes().contains(scope2));
    }

    @Test
    public void testCreateLocalChainedTokenCredentialBuilder_NotMocked() {
        // This test verifies that the actual implementation returns a non-null builder
        // when not mocked (for coverage of the actual method)
        ChainedTokenCredentialBuilder actualBuilder = azureServicePrincipal.createLocalChainedTokenCredentialBuilder();
        assertNotNull(actualBuilder);
    }
}
