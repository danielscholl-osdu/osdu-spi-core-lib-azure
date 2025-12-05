package org.opengroup.osdu.azure.util;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.identity.WorkloadIdentityCredential;
import com.azure.identity.WorkloadIdentityCredentialBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.verify;
import org.opengroup.osdu.core.common.model.http.AppException;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
public class AzureServicePrincipalTest {

    private static final String accessTokenContent = "some-access-token";
    private static final String spId = "client-id";
    private static final String spSecret = "client-secret";
    private static final String tenantId = "tenant-id";
    private static final String appResourceId = "app-resource-id";

    @Mock
    ClientSecretCredentialBuilder clientSecretCredentialBuilder;

    @Mock
    ClientSecretCredential clientSecretCredential;

    @Mock
    ManagedIdentityCredentialBuilder managedIdentityCredentialBuilder;

    @Mock
    ManagedIdentityCredential managedIdentityCredential;

    @Mock
    private Mono<AccessToken> responseMono;

    @Spy
    private AzureServicePrincipal azureServicePrincipal;

    @Mock
    WorkloadIdentityCredentialBuilder workloadIdentityCredentialBuilder;

    @Mock
    WorkloadIdentityCredential workloadIdentityCredential;

    @Test
    public void TestGenerateIDToken() throws Exception {
        // Test with invalid parameters to ensure method exists and handles failures
        assertThrows(AppException.class, () -> azureServicePrincipal.getIdToken("", "", "", ""));
    }

    @Test
    public void TestGenerateIDToken_failure() throws Exception {
        // Test with empty parameters to trigger failure
        assertThrows(AppException.class, () -> azureServicePrincipal.getIdToken("", "", "", ""));
    }

    @Test
    public void TestGenerateMsiToken() throws Exception {
        // Add ArgumentCaptor to verify scope
        ArgumentCaptor<TokenRequestContext> requestCaptor = ArgumentCaptor.forClass(TokenRequestContext.class);

        when(azureServicePrincipal.createManagedIdentityCredentialBuilder()).thenReturn(managedIdentityCredentialBuilder);
        when(managedIdentityCredentialBuilder.build()).thenReturn(managedIdentityCredential);
        when(managedIdentityCredential.getToken(requestCaptor.capture())).thenReturn(responseMono);
        when(responseMono.block()).thenReturn(new AccessToken(accessTokenContent, OffsetDateTime.now()));

        String result = azureServicePrincipal.getMSIToken();
        assertEquals(accessTokenContent, result);

        // Verify the scope
        TokenRequestContext capturedRequest = requestCaptor.getValue();
        assertEquals("https://management.azure.com/.default", capturedRequest.getScopes().get(0));

        verify(managedIdentityCredentialBuilder, times(1)).build();
        verify(managedIdentityCredential, times(1)).getToken(any(TokenRequestContext.class));
        verify(responseMono, times(1)).block();
    }

    @Test
    public void TestGenerateMsiToken_failure() throws Exception {
        // Add ArgumentCaptor to verify scope
        ArgumentCaptor<TokenRequestContext> requestCaptor = ArgumentCaptor.forClass(TokenRequestContext.class);

        when(azureServicePrincipal.createManagedIdentityCredentialBuilder()).thenReturn(managedIdentityCredentialBuilder);
        when(managedIdentityCredentialBuilder.build()).thenReturn(managedIdentityCredential);
        when(managedIdentityCredential.getToken(requestCaptor.capture())).thenReturn(responseMono);
        when(responseMono.block()).thenReturn(null);

        assertThrows(AppException.class, () -> azureServicePrincipal.getMSIToken());

        // Verify the scope
        TokenRequestContext capturedRequest = requestCaptor.getValue();
        assertEquals("https://management.azure.com/.default", capturedRequest.getScopes().get(0));

        verify(managedIdentityCredentialBuilder, times(1)).build();
        verify(managedIdentityCredential, times(1)).getToken(any(TokenRequestContext.class));
        verify(responseMono, times(1)).block();
    }

    @Test
    public void TestGenerateWIToken() throws Exception {
        // Add ArgumentCaptor to verify scopes
        ArgumentCaptor<TokenRequestContext> requestCaptor = ArgumentCaptor.forClass(TokenRequestContext.class);

        when(azureServicePrincipal.createworkloadIdentityClientBuilder()).thenReturn(workloadIdentityCredentialBuilder);
        when(workloadIdentityCredentialBuilder.build()).thenReturn(workloadIdentityCredential);
        when(workloadIdentityCredential.getToken(requestCaptor.capture())).thenReturn(responseMono);
        when(responseMono.block()).thenReturn(new AccessToken(accessTokenContent, OffsetDateTime.now()));

        String result = azureServicePrincipal.getWIToken(appResourceId);
        assertEquals(accessTokenContent, result);

        // Verify both scopes in the correct order
        TokenRequestContext capturedRequest = requestCaptor.getValue();
        assertEquals("https://management.azure.com/.default", capturedRequest.getScopes().get(0));
        assertEquals(appResourceId + "/.default", capturedRequest.getScopes().get(1));

        verify(workloadIdentityCredentialBuilder, times(1)).build();
        verify(workloadIdentityCredential, times(1)).getToken(any(TokenRequestContext.class));
        verify(responseMono, times(1)).block();
    }

    @Test
    public void TestGenerateWIToken_failure() throws Exception {
        // Add ArgumentCaptor to verify scopes
        ArgumentCaptor<TokenRequestContext> requestCaptor = ArgumentCaptor.forClass(TokenRequestContext.class);

        when(azureServicePrincipal.createworkloadIdentityClientBuilder()).thenReturn(workloadIdentityCredentialBuilder);
        when(workloadIdentityCredentialBuilder.build()).thenReturn(workloadIdentityCredential);
        when(workloadIdentityCredential.getToken(requestCaptor.capture())).thenReturn(responseMono);
        when(responseMono.block()).thenReturn(null);

        assertThrows(AppException.class, () -> azureServicePrincipal.getWIToken(appResourceId));

        // Verify both scopes in the correct order
        TokenRequestContext capturedRequest = requestCaptor.getValue();
        assertEquals("https://management.azure.com/.default", capturedRequest.getScopes().get(0));
        assertEquals(appResourceId + "/.default", capturedRequest.getScopes().get(1));

        verify(workloadIdentityCredentialBuilder, times(1)).build();
        verify(workloadIdentityCredential, times(1)).getToken(any(TokenRequestContext.class));
        verify(responseMono, times(1)).block();
    }

    /**
     *  This test is added for end to verification whether tokens are getting generated.
     */
    //
    @Disabled
    @Test
    public void VerifyingEndToEndScenario() throws Exception {

        String spId = "";
        String spSecret = "";
        String tenantId = "";
        String appResourceId = "";

        String result = new AzureServicePrincipal().getIdToken(spId, spSecret, tenantId, appResourceId);
        assertNotNull(result);
    }
}
