package org.opengroup.osdu.azure.util;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.AppException;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.identity.WorkloadIdentityCredential;
import com.azure.identity.WorkloadIdentityCredentialBuilder;

import reactor.core.publisher.Mono;

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

    @Mock
    private Mono<AccessToken> timeoutMono;

    @Spy
    private AzureServicePrincipal azureServicePrincipal;

    @Mock
    WorkloadIdentityCredentialBuilder workloadIdentityCredentialBuilder;

    @Mock
    WorkloadIdentityCredential workloadIdentityCredential;

    @Test
    public void TestGenerateIDToken() throws Exception {
        setupClientSecretCredentialMocks();

        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(responseMono);
        mockSuccessfulTokenResponse(createAccessToken());

        String result = azureServicePrincipal.getIdToken(spId, spSecret, tenantId, appResourceId);
        assertEquals(accessTokenContent, result);

        verify(clientSecretCredentialBuilder, times(1)).build();
        verify(clientSecretCredential, times(1)).getToken(any(TokenRequestContext.class));
        verify(timeoutMono, times(1)).block();
    }

    @Test
    public void TestGenerateIDToken_failure() throws Exception {
        setupClientSecretCredentialMocks();

        when(clientSecretCredential.getToken(any(TokenRequestContext.class))).thenReturn(responseMono);
        mockFailedTokenResponse();

        assertThrows(AppException.class, () -> azureServicePrincipal.getIdToken(spId, spSecret, tenantId, appResourceId));

        verify(clientSecretCredentialBuilder, times(1)).build();
        verify(clientSecretCredential, times(1)).getToken(any(TokenRequestContext.class));
        verify(timeoutMono, times(1)).block();    }

    @Test
    public void TestGenerateMsiToken() throws Exception {
        ArgumentCaptor<TokenRequestContext> requestCaptor = ArgumentCaptor.forClass(TokenRequestContext.class);
        AccessToken token = new AccessToken(accessTokenContent, OffsetDateTime.now().plusHours(1));

        setupManagedIdentityCredentialMocks();
        // Use Mono.just so the full reactive chain (including .timeout()) works without breaking
        when(managedIdentityCredential.getToken(requestCaptor.capture())).thenReturn(Mono.just(token));

        String result = azureServicePrincipal.getMSIToken();
        assertEquals(accessTokenContent, result);

        // Verify the scope
        TokenRequestContext capturedRequest = requestCaptor.getValue();
        assertEquals("https://management.azure.com/.default", capturedRequest.getScopes().get(0));

        verify(managedIdentityCredentialBuilder, times(1)).build();
        verify(managedIdentityCredential, times(1)).getToken(any(TokenRequestContext.class));
    }

    @Test
    public void TestGenerateMsiToken_failure() throws Exception {
        ArgumentCaptor<TokenRequestContext> requestCaptor = ArgumentCaptor.forClass(TokenRequestContext.class);

        setupManagedIdentityCredentialMocks();
        // Use Mono.empty() (null block() result) so the full reactive chain (including .timeout()) works
        when(managedIdentityCredential.getToken(requestCaptor.capture())).thenReturn(Mono.empty());

        assertThrows(AppException.class, () -> azureServicePrincipal.getMSIToken());

        // Verify the scope
        TokenRequestContext capturedRequest = requestCaptor.getValue();
        assertEquals("https://management.azure.com/.default", capturedRequest.getScopes().get(0));

        verify(managedIdentityCredentialBuilder, times(1)).build();
        verify(managedIdentityCredential, times(1)).getToken(any(TokenRequestContext.class));
    }

    @Test
    public void TestGenerateMsiToken_timeout_throwsAppException() throws Exception {
        setupManagedIdentityCredentialMocks();
        // Simulate what Reactor's .timeout() produces: a RuntimeException wrapping a TimeoutException
        when(managedIdentityCredential.getToken(any(TokenRequestContext.class)))
            .thenReturn(Mono.error(new RuntimeException(new java.util.concurrent.TimeoutException("IMDS timeout after 10s"))));

        AppException ex = assertThrows(AppException.class, () -> azureServicePrincipal.getMSIToken());

        // The message should mention the timeout so operators know IMDS is throttled
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("timed out") || ex.getMessage().contains("timeout"),
            "Expected AppException to mention timeout but got: " + ex.getMessage());
    }

    @Test
    public void TestGenerateWIToken() throws Exception {
        ArgumentCaptor<TokenRequestContext> requestCaptor = ArgumentCaptor.forClass(TokenRequestContext.class);

        setupWorkloadIdentityCredentialMocks();
        when(workloadIdentityCredential.getToken(requestCaptor.capture())).thenReturn(responseMono);
        mockSuccessfulTokenResponse(createAccessToken());

        String result = azureServicePrincipal.getWIToken(appResourceId);
        assertEquals(accessTokenContent, result);

        // Verify both scopes in the correct order
        TokenRequestContext capturedRequest = requestCaptor.getValue();
        assertEquals("https://management.azure.com/.default", capturedRequest.getScopes().get(0));
        assertEquals(appResourceId + "/.default", capturedRequest.getScopes().get(1));

        verify(workloadIdentityCredentialBuilder, times(1)).build();
        verify(workloadIdentityCredential, times(1)).getToken(any(TokenRequestContext.class));
        verify(timeoutMono, times(1)).block();
    }

    @Test
    public void TestGenerateWIToken_failure() throws Exception {
        ArgumentCaptor<TokenRequestContext> requestCaptor = ArgumentCaptor.forClass(TokenRequestContext.class);

        setupWorkloadIdentityCredentialMocks();
        when(workloadIdentityCredential.getToken(requestCaptor.capture())).thenReturn(responseMono);
        mockFailedTokenResponse();

        assertThrows(AppException.class, () -> azureServicePrincipal.getWIToken(appResourceId));

        // Verify both scopes in the correct order
        TokenRequestContext capturedRequest = requestCaptor.getValue();
        assertEquals("https://management.azure.com/.default", capturedRequest.getScopes().get(0));
        assertEquals(appResourceId + "/.default", capturedRequest.getScopes().get(1));

        verify(workloadIdentityCredentialBuilder, times(1)).build();
        verify(workloadIdentityCredential, times(1)).getToken(any(TokenRequestContext.class));
        verify(timeoutMono, times(1)).block();
    }

    @Test
    public void TestGenerateIdToken_trailingSlash() throws Exception {
        String trailingSlashResourceId = "https://management.azure.com/";
        ArgumentCaptor<TokenRequestContext> requestCaptor = ArgumentCaptor.forClass(TokenRequestContext.class);

        setupClientSecretCredentialMocks();
        when(clientSecretCredential.getToken(requestCaptor.capture())).thenReturn(responseMono);
        mockSuccessfulTokenResponse(createAccessToken());

        String result = azureServicePrincipal.getIdToken(spId, spSecret, tenantId, trailingSlashResourceId);
        assertEquals(accessTokenContent, result);

        // Verify the scope does NOT have a double slash
        TokenRequestContext capturedRequest = requestCaptor.getValue();
        assertEquals("https://management.azure.com/.default", capturedRequest.getScopes().get(0));
    }

    @Test
    public void TestGenerateWIToken_trailingSlash_deduplicatesScope() throws Exception {
        String trailingSlashResourceId = "https://management.azure.com/";
        String scope = "https://management.azure.com/.default";
        ArgumentCaptor<TokenRequestContext> requestCaptor = ArgumentCaptor.forClass(TokenRequestContext.class);

        setupWorkloadIdentityCredentialMocks();
        when(workloadIdentityCredential.getToken(requestCaptor.capture())).thenReturn(responseMono);
        mockSuccessfulTokenResponse(createAccessToken());

        String result = azureServicePrincipal.getWIToken(trailingSlashResourceId, scope);
        assertEquals(accessTokenContent, result);

        // Verify only ONE scope is sent (deduplication works) and no double slash
        TokenRequestContext capturedRequest = requestCaptor.getValue();
        assertEquals(1, capturedRequest.getScopes().size());
        assertEquals("https://management.azure.com/.default", capturedRequest.getScopes().get(0));
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

    // Private helper methods to reduce duplication

    private void setupClientSecretCredentialMocks() {
        when(azureServicePrincipal.createClientSecretCredentialBuilder()).thenReturn(clientSecretCredentialBuilder);
        when(clientSecretCredentialBuilder.tenantId(tenantId)).thenReturn(clientSecretCredentialBuilder);
        when(clientSecretCredentialBuilder.clientId(spId)).thenReturn(clientSecretCredentialBuilder);
        when(clientSecretCredentialBuilder.clientSecret(spSecret)).thenReturn(clientSecretCredentialBuilder);
        when(clientSecretCredentialBuilder.build()).thenReturn(clientSecretCredential);
    }

    private void setupManagedIdentityCredentialMocks() {
        when(azureServicePrincipal.createManagedIdentityCredentialBuilder()).thenReturn(managedIdentityCredentialBuilder);
        when(managedIdentityCredentialBuilder.build()).thenReturn(managedIdentityCredential);
    }

    private void setupWorkloadIdentityCredentialMocks() {
        when(azureServicePrincipal.createworkloadIdentityClientBuilder()).thenReturn(workloadIdentityCredentialBuilder);
        when(workloadIdentityCredentialBuilder.build()).thenReturn(workloadIdentityCredential);
    }

    private void mockSuccessfulTokenResponse(AccessToken token) {
        when(responseMono.timeout(any(Duration.class))).thenReturn(timeoutMono);
        when(timeoutMono.block()).thenReturn(token);
    }

    private void mockFailedTokenResponse() {
        when(responseMono.timeout(any(Duration.class))).thenReturn(timeoutMono);
        when(timeoutMono.block()).thenReturn(null);
    }

    private AccessToken createAccessToken() {
        return new AccessToken(accessTokenContent, OffsetDateTime.now());
    }
}
