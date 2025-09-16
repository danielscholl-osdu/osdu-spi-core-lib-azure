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

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.implementation.IdentityClient;
import com.azure.identity.implementation.IdentityClientBuilder;
import com.azure.identity.WorkloadIdentityCredential;
import com.azure.identity.WorkloadIdentityCredentialBuilder;

import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.stereotype.Component;

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

    private static final int ERROR_STATUS_CODE = 500;
    private static final String ERROR_REASON = "Received empty token";
    private static final String ERROR_MESSAGE_SPN = "SPN client returned null token";
    private static final String ERROR_MESSAGE_MSI = "MSI client returned null token";
    private static final String ERROR_MESSAGE_WI = "Workload Identity client returned null token";
    private static final String MANAGEMENT_SCOPE = "https://management.azure.com/.default";


    /**
     * Comparison of Token Acquisition Methods:
     *
     * | Feature                   | getMSIToken                              | getWIToken                              | getIdToken                               |
     * |---------------------------|------------------------------------------|------------------------------------------|-----------------------------------------|
     * | Mechanism                 | Managed Identity                        | Workload Identity                        | Service Principal (SPN)                  |
     * | Credential Builder        | IdentityClientBuilder                   | WorkloadIdentityCredentialBuilder        | IdentityClientBuilder                    |
     * | Endpoint                  | IMDS (http://169.254.169.254/metadata)  | Kubernetes and Azure AD federation       | Azure AD                                 |
     * | Environment Dependency    | Azure Managed Identity-enabled resources| Kubernetes Workload Identity setup       | None (requires SPN credentials)          |
     * | Input Parameters          | None                                    | Resource ID                              | SPN ID, SPN Secret, Tenant ID, Resource  |
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
            IdentityClientBuilder identityClientBuilder = createIdentityClientBuilder();
            IdentityClient identityClientSPN = identityClientBuilder.tenantId(tenant_id)
                    .clientId(sp_id)
                    .clientSecret(sp_secret)
                    .build();

            TokenRequestContext requestContextSPN = new TokenRequestContext();
            requestContextSPN.addScopes(app_resource_id.concat("/.default"));
            AccessToken tokenSpnCreds = identityClientSPN.authenticateWithConfidentialClient(requestContextSPN).block();

            if (tokenSpnCreds != null && tokenSpnCreds.getToken() != null) {
                return tokenSpnCreds.getToken();
            }
            throw new AppException(ERROR_STATUS_CODE, ERROR_REASON, ERROR_MESSAGE_SPN);
        } catch (Exception e) {
            throw new AppException(ERROR_STATUS_CODE, ERROR_REASON,
                "Service Principal token acquisition failed: " + e.getMessage());
        }
    }

    /**
     * Gets a token using DefaultAzureCredential with default management scope.
     * @return The authentication token string
     */
    public String getMSIToken() {
        try {
            IdentityClientBuilder identityClientBuilder = createIdentityClientBuilder();
            IdentityClient identityClientMSI = identityClientBuilder.build();

            TokenRequestContext requestContextMSI = new TokenRequestContext();
            requestContextMSI.addScopes(MANAGEMENT_SCOPE);
            AccessToken tokenMsi = identityClientMSI.authenticateToIMDSEndpoint(requestContextMSI).block();

            if (tokenMsi != null && tokenMsi.getToken() != null) {
                return tokenMsi.getToken();
            }
            throw new AppException(ERROR_STATUS_CODE, ERROR_REASON, ERROR_MESSAGE_MSI);
        } catch (Exception e) {
            throw new AppException(ERROR_STATUS_CODE, ERROR_REASON,
                "MSI token acquisition failed: " + e.getMessage());
        }
    }

    /**
     * Method to generate token using Workload Identity.
     * @param app_resource_id The resource ID of the target application
     * @return AUTHENTICATION TOKEN
     */
    public String getWIToken(final String app_resource_id) {
        try {
            WorkloadIdentityCredentialBuilder workloadIdentityClientBuilder = createworkloadIdentityClientBuilder();
            WorkloadIdentityCredential credential = workloadIdentityClientBuilder.build();

            TokenRequestContext requestContext = new TokenRequestContext();
            requestContext.addScopes(MANAGEMENT_SCOPE);
            requestContext.addScopes(app_resource_id.concat("/.default"));
            AccessToken token = credential.getToken(requestContext).block();

            if (token != null && token.getToken() != null) {
                return token.getToken();
            }
            throw new AppException(ERROR_STATUS_CODE, ERROR_REASON, ERROR_MESSAGE_WI);
        } catch (Exception e) {
            throw new AppException(ERROR_STATUS_CODE, ERROR_REASON,
                "Workload Identity token acquisition failed: " + e.getMessage());
        }
    }

    /**
     * Creates a new IdentityClientBuilder instance.
     * Protected method to allow mocking in tests.
     *
     * @return A new instance of IdentityClientBuilder
     */
    protected IdentityClientBuilder createIdentityClientBuilder() {
        return new IdentityClientBuilder();
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
}
