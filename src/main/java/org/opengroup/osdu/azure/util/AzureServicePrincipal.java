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

package org.opengroup.osdu.azure.util;

import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.stereotype.Component;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.AzureCliCredential;
import com.azure.identity.AzureCliCredentialBuilder;
import com.azure.identity.ChainedTokenCredential;
import com.azure.identity.ChainedTokenCredentialBuilder;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.IntelliJCredential;
import com.azure.identity.IntelliJCredentialBuilder;
import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.identity.WorkloadIdentityCredential;
import com.azure.identity.WorkloadIdentityCredentialBuilder;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to generate the AAD authentication tokens using WorkloadIdentityCredential.
 *
 * For production deployments, configure one of:
 * - Workload Identity (recommended for AKS)
 * - Managed Identity/Pod Identity (legacy approach for AKS)
 * - Environment Variables (for non-AKS scenarios)
 */
@Component
public final class AzureServicePrincipal {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureServicePrincipal.class);
    private static final int ERROR_STATUS_CODE = 500;
    private static final String ERROR_REASON = "Received empty token";
    private static final String ERROR_MESSAGE_SPN = "SPN client returned null token";
    private static final String ERROR_MESSAGE_MSI = "MSI client returned null token";
    private static final String ERROR_MESSAGE_WI = "Workload Identity client returned null token";
    private static final String MANAGEMENT_SCOPE = "https://management.azure.com/.default";

    /** Maximum time to wait for a token from IMDS before giving up. */
    private static final Duration IMDS_TIMEOUT = Duration.ofSeconds(10);

    /** Maximum time to wait for a token from Azure AD before giving up. */
    private static final Duration AZURE_AD_TIMEOUT = Duration.ofSeconds(30);


    /*
     * Comparison of Token Acquisition Methods:
     *
     * | Feature                   | getMSIToken                              | getWIToken                              | getIdToken                               |
     * |---------------------------|------------------------------------------|------------------------------------------|-----------------------------------------|
     * | Mechanism                 | Managed Identity                        | Workload Identity                        | Service Principal (SPN)                  |
     * | Credential Builder        | ManagedIdentityCredentialBuilder        | WorkloadIdentityCredentialBuilder        | ClientSecretCredentialBuilder            |
     * | Endpoint                  | IMDS (http://169.254.169.254/metadata)  | Kubernetes and Azure AD federation       | Azure AD                                 |
     * | Environment Dependency    | Azure Managed Identity-enabled resources| Kubernetes Workload Identity setup       | None (requires SPN credentials)          |
     * | Input Parameters          | Scope                                   | Scope                                    | SPN ID, SPN Secret, Tenant ID, Resource  |
     * | Use Case                  | Azure-native environments               | Kubernetes-native environments           | Non-Azure environments or explicit SPN usage |
     *
     * Summary:
     * - `getMSIToken`: Designed for Azure-native environments using Managed Identity
     * - `getWIToken`: Optimized for Kubernetes deployments using Workload Identity federation
     * - `getIdToken`: Suitable for non-Azure environments or when explicit SPN credentials are required
     */

    /**
     * Gets an authentication token using service principal credentials.
     * This method uses client credentials (client ID and secret) to obtain a token.
     *
     * @param sp_id The service principal (client) ID
     * @param sp_secret The service principal client secret
     * @param tenant_id The Azure AD tenant ID
     * @param app_resource_id The resource ID of the target application
     * @return The authentication token string
     * @throws AppException if token acquisition fails or returns null
     */
    public String getIdToken(final String sp_id, final String sp_secret, final String tenant_id, final String app_resource_id) {
        try {
            ClientSecretCredentialBuilder credentialBuilder = createClientSecretCredentialBuilder();
            ClientSecretCredential credential = credentialBuilder
                    .tenantId(tenant_id)
                    .clientId(sp_id)
                    .clientSecret(sp_secret)
                    .build();

            String sanitizedResourceId = sanitizeResourceId(app_resource_id);

            TokenRequestContext requestContextSPN = new TokenRequestContext();
            requestContextSPN.addScopes(sanitizedResourceId.concat("/.default"));
            AccessToken tokenSpnCreds = credential.getToken(requestContextSPN)
                    .timeout(AZURE_AD_TIMEOUT)
                    .block();

            return extractTokenOrThrow(tokenSpnCreds, ERROR_MESSAGE_SPN);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw handleTokenAcquisitionException(e, "Service Principal", AZURE_AD_TIMEOUT);
        }
    }

    /**
     * Gets a token using Managed Identity Credential with custom scope.
     * A 10-second timeout guards against IMDS throttling (e.g., many pods refreshing
     * simultaneously), which would otherwise cause indefinite blocking.
     * @param scope The scope to request
     * @return The authentication token string
     */
    public String getMSIToken(final String scope) {
        try {
            ManagedIdentityCredentialBuilder credentialBuilder = createManagedIdentityCredentialBuilder();
            ManagedIdentityCredential credential = credentialBuilder.build();

            TokenRequestContext requestContextMSI = new TokenRequestContext();
            requestContextMSI.addScopes(scope);
            AccessToken tokenMsi = credential.getToken(requestContextMSI)
                    .timeout(IMDS_TIMEOUT)
                    .block();

            return extractTokenOrThrow(tokenMsi, ERROR_MESSAGE_MSI);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw handleTokenAcquisitionException(e, "MSI", IMDS_TIMEOUT);
        }
    }

    /**
     * Gets a token using Managed Identity Credential with default management scope.
     * @return The authentication token string
     */
    public String getMSIToken() {
        return getMSIToken(MANAGEMENT_SCOPE);
    }

    /**
     * Generates a token for one scope using Workload Identity.
     * Client-credentials access tokens target one resource, so callers must
     * request separate tokens for different resource scopes.
     * @param scope The scope to request
     * @return AUTHENTICATION TOKEN
     */
    public String getWIToken(final String scope) {
        try {
            WorkloadIdentityCredentialBuilder workloadIdentityClientBuilder = createworkloadIdentityClientBuilder();
            WorkloadIdentityCredential credential = workloadIdentityClientBuilder.build();

            TokenRequestContext requestContext = new TokenRequestContext();
            requestContext.addScopes(scope);
            AccessToken token = credential.getToken(requestContext)
                    .timeout(AZURE_AD_TIMEOUT)
                    .block();

            return extractTokenOrThrow(token, ERROR_MESSAGE_WI);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw handleTokenAcquisitionException(e, "Workload Identity", AZURE_AD_TIMEOUT);
        }
    }

    /**
     * Gets an authentication token using local development credentials with custom scope.
     *
     * <p><b>Note</b>: This method calls {@code .block()} on the reactive credential chain.
     * It must not be invoked from a Reactor event-loop thread (e.g., WebFlux handlers).
     *
     * @param scope The scope to request
     * @return The authentication token string
     * @throws AppException if token acquisition fails or returns null
     */
    public String getLocalDevToken(final String scope) {
        if (scope == null || scope.trim().isEmpty()) {
            throw new AppException(ERROR_STATUS_CODE, ERROR_REASON, "Scope must not be null or empty");
        }
        try {
            ChainedTokenCredentialBuilder credentialBuilder = this.createLocalChainedTokenCredentialBuilder();

            ChainedTokenCredential chainedCredential = credentialBuilder.build();

            String trimmedScope = scope.trim();
            if (trimmedScope.contains(" ")) {
                throw new AppException(ERROR_STATUS_CODE, ERROR_REASON, "Scope must not contain internal whitespace");
            }
            TokenRequestContext requestContext = new TokenRequestContext();
            requestContext.addScopes(trimmedScope);

            AccessToken token = chainedCredential.getToken(requestContext).block(Duration.ofSeconds(30));

            if (token != null && token.getToken() != null) {
                return token.getToken();
            }
            throw new AppException(ERROR_STATUS_CODE, ERROR_REASON,
                "Local development credential returned null token");
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.warn("Local development token acquisition failed: {}", e.getMessage());
            throw new AppException(ERROR_STATUS_CODE, ERROR_REASON,
                "Local development token acquisition failed. Ensure 'az login' or IntelliJ Azure plugin is configured correctly.");
        }
    }

    /**
     * Creates a ChainedTokenCredentialBuilder for local development.
     * Protected method to allow mocking in tests.
     *
     * @return A ChainedTokenCredentialBuilder instance
     */
    protected ChainedTokenCredentialBuilder createLocalChainedTokenCredentialBuilder() {
        ChainedTokenCredentialBuilder credentialBuilder = new ChainedTokenCredentialBuilder();

        AzureCliCredential azureCliCredential = new AzureCliCredentialBuilder().build();
        credentialBuilder.addLast(azureCliCredential);

        IntelliJCredential intelliJCredential = new IntelliJCredentialBuilder().build();
        credentialBuilder.addLast(intelliJCredential);

        return credentialBuilder;
    }

    /**
     * Creates a new ManagedIdentityCredentialBuilder instance.
     * Protected method to allow mocking in tests.
     *
     * @return A new instance of ManagedIdentityCredentialBuilder
     */
    protected ManagedIdentityCredentialBuilder createManagedIdentityCredentialBuilder() {
        return new ManagedIdentityCredentialBuilder();
    }

    /**
     * Creates a new ClientSecretCredentialBuilder instance.
     * Protected method to allow mocking in tests.
     *
     * @return A new instance of ClientSecretCredentialBuilder
     */
    protected ClientSecretCredentialBuilder createClientSecretCredentialBuilder() {
        return new ClientSecretCredentialBuilder();
    }

    /**
     * Creates a new WorkloadIdentityClientBuilder instance.
     * Protected method to allow mocking in tests.
     *
     * @return A new instance of WorkloadIdentityClientBuilder
     */
    protected WorkloadIdentityCredentialBuilder createworkloadIdentityClientBuilder() {
        return new WorkloadIdentityCredentialBuilder();
    }

    /**
     * Sanitizes a resource ID by removing trailing slashes to avoid scope formatting issues.
     *
     * @param resourceId The resource ID to sanitize
     * @return The sanitized resource ID without trailing slash
     */
    private String sanitizeResourceId(final String resourceId) {
        return resourceId.endsWith("/")
            ? resourceId.substring(0, resourceId.length() - 1)
            : resourceId;
    }

    /**
     * Extracts the token string from an AccessToken or throws an AppException if the token is null.
     *
     * @param accessToken The AccessToken to extract from
     * @param errorMessage The error message to use if the token is null
     * @return The token string
     * @throws AppException if the token is null or empty
     */
    private String extractTokenOrThrow(final AccessToken accessToken, final String errorMessage) {
        if (accessToken != null && accessToken.getToken() != null) {
            return accessToken.getToken();
        }
        throw new AppException(ERROR_STATUS_CODE, ERROR_REASON, errorMessage);
    }

    /**
     * Walks the exception cause chain to find the root cause.
     *
     * @param exception The exception to analyze
     * @return The root cause of the exception
     */
    private Throwable getRootCause(final Throwable exception) {
        Throwable rootCause = exception;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    /**
     * Handles exceptions from token acquisition operations, detecting timeouts and wrapping in AppException.
     * Reactor's block() wraps checked exceptions via Exceptions.propagate(), so this walks the full
     * cause chain to reliably detect TimeoutException at any nesting depth.
     *
     * @param exception The exception to handle
     * @param credentialType The type of credential (e.g., "MSI", "Service Principal")
     * @param timeout The timeout duration that was applied
     * @return An AppException with appropriate error message
     */
    private AppException handleTokenAcquisitionException(final Exception exception, final String credentialType, final Duration timeout) {
        Throwable rootCause = getRootCause(exception);
        String exceptionMessage = exception.getMessage();
        String appExceptionMessage;

        if (rootCause instanceof TimeoutException) {
            String message = credentialType + " token acquisition timed out after " + timeout.getSeconds() + "s";
            if ("MSI".equals(credentialType)) {
                message += " — IMDS may be throttled";
            }
            appExceptionMessage = message + ": " + exceptionMessage;
        } else {
            appExceptionMessage = credentialType + " token acquisition failed: " + exceptionMessage;
        }

        return new AppException(ERROR_STATUS_CODE, ERROR_REASON, appExceptionMessage);
    }
}
