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
import com.azure.identity.ChainedTokenCredential;
import com.azure.identity.ChainedTokenCredentialBuilder;
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

    @Mock
    ChainedTokenCredentialBuilder chainedTokenCredentialBuilder;

    @Mock
    ChainedTokenCredential chainedTokenCredential;

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

        String scope = "https://management.azure.com/.default";
        String result = azureServicePrincipal.getWIToken(scope);
        assertEquals(accessTokenContent, result);

        // A client-credentials token can target only one resource.
        TokenRequestContext capturedRequest = requestCaptor.getValue();
        assertEquals(1, capturedRequest.getScopes().size());
        assertEquals(scope, capturedRequest.getScopes().get(0));

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

        String scope = "https://management.azure.com/.default";
        assertThrows(AppException.class, () -> azureServicePrincipal.getWIToken(scope));

        TokenRequestContext capturedRequest = requestCaptor.getValue();
        assertEquals(1, capturedRequest.getScopes().size());
        assertEquals(scope, capturedRequest.getScopes().get(0));

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
    public void TestGenerateWIToken_customScope() throws Exception {
        String scope = "https://cosmos.azure.com/.default";
        ArgumentCaptor<TokenRequestContext> requestCaptor = ArgumentCaptor.forClass(TokenRequestContext.class);

        setupWorkloadIdentityCredentialMocks();
        when(workloadIdentityCredential.getToken(requestCaptor.capture())).thenReturn(responseMono);
        mockSuccessfulTokenResponse(createAccessToken());

        String result = azureServicePrincipal.getWIToken(scope);
        assertEquals(accessTokenContent, result);

        TokenRequestContext capturedRequest = requestCaptor.getValue();
        assertEquals(1, capturedRequest.getScopes().size());
        assertEquals(scope, capturedRequest.getScopes().get(0));
    }

    // --- getLocalDevToken tests ---

    @Test
    public void TestGenerateLocalDevToken() throws Exception {
        setupChainedTokenCredentialMocks();
        when(chainedTokenCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(createAccessToken()));

        String result = azureServicePrincipal.getLocalDevToken("https://management.azure.com/.default");
        assertEquals(accessTokenContent, result);

        verify(chainedTokenCredentialBuilder, times(1)).build();
        verify(chainedTokenCredential, times(1)).getToken(any(TokenRequestContext.class));
    }

    @Test
    public void TestGenerateLocalDevToken_failure_nullToken() throws Exception {
        setupChainedTokenCredentialMocks();
        when(chainedTokenCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.empty());

        assertThrows(AppException.class, () -> azureServicePrincipal.getLocalDevToken("https://management.azure.com/.default"));

        verify(chainedTokenCredentialBuilder, times(1)).build();
    }

    @Test
    public void TestGenerateLocalDevToken_failure_exception() throws Exception {
        setupChainedTokenCredentialMocks();
        when(chainedTokenCredential.getToken(any(TokenRequestContext.class)))
            .thenReturn(Mono.error(new RuntimeException("Azure CLI not logged in")));

        AppException ex = assertThrows(AppException.class, () ->
            azureServicePrincipal.getLocalDevToken("https://management.azure.com/.default"));

        assertTrue(ex.getMessage().contains("Local development token acquisition failed"));
    }

    // --- Timeout tests for SPN and WI ---

    @Test
    public void TestGenerateIdToken_timeout_throwsAppException() throws Exception {
        setupClientSecretCredentialMocks();
        when(clientSecretCredential.getToken(any(TokenRequestContext.class)))
            .thenReturn(Mono.error(new RuntimeException(new java.util.concurrent.TimeoutException("Azure AD timeout after 30s"))));

        AppException ex = assertThrows(AppException.class, () ->
            azureServicePrincipal.getIdToken(spId, spSecret, tenantId, appResourceId));

        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("timed out"),
            "Expected AppException to mention timeout but got: " + ex.getMessage());
        // SPN timeout should NOT mention IMDS throttling
        assertTrue(!ex.getMessage().contains("IMDS"),
            "SPN timeout should not mention IMDS but got: " + ex.getMessage());
    }

    @Test
    public void TestGenerateWIToken_timeout_throwsAppException() throws Exception {
        setupWorkloadIdentityCredentialMocks();
        when(workloadIdentityCredential.getToken(any(TokenRequestContext.class)))
            .thenReturn(Mono.error(new RuntimeException(new java.util.concurrent.TimeoutException("Azure AD timeout after 30s"))));

        AppException ex = assertThrows(AppException.class, () ->
            azureServicePrincipal.getWIToken("https://management.azure.com/.default"));

        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("timed out"),
            "Expected AppException to mention timeout but got: " + ex.getMessage());
    }

    @Test
    public void TestGenerateIdToken_genericException_throwsAppException() throws Exception {
        setupClientSecretCredentialMocks();
        when(clientSecretCredential.getToken(any(TokenRequestContext.class)))
            .thenReturn(Mono.error(new RuntimeException("Network unreachable")));

        AppException ex = assertThrows(AppException.class, () ->
            azureServicePrincipal.getIdToken(spId, spSecret, tenantId, appResourceId));

        assertTrue(ex.getMessage().contains("failed"),
            "Expected failure message but got: " + ex.getMessage());
    }

    @Test
    public void TestGenerateMsiToken_customScope() throws Exception {
        String customScope = "https://redis.azure.com/.default";
        ArgumentCaptor<TokenRequestContext> requestCaptor = ArgumentCaptor.forClass(TokenRequestContext.class);
        AccessToken token = new AccessToken(accessTokenContent, OffsetDateTime.now().plusHours(1));

        setupManagedIdentityCredentialMocks();
        when(managedIdentityCredential.getToken(requestCaptor.capture())).thenReturn(Mono.just(token));

        String result = azureServicePrincipal.getMSIToken(customScope);
        assertEquals(accessTokenContent, result);

        TokenRequestContext capturedRequest = requestCaptor.getValue();
        assertEquals(customScope, capturedRequest.getScopes().get(0));
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

    private void setupChainedTokenCredentialMocks() {
        when(azureServicePrincipal.createLocalChainedTokenCredentialBuilder()).thenReturn(chainedTokenCredentialBuilder);
        when(chainedTokenCredentialBuilder.build()).thenReturn(chainedTokenCredential);
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
