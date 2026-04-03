package org.opengroup.osdu.azure.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.azure.security.keyvault.secrets.SecretClient;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.azure.di.AzureActiveDirectoryConfiguration;
import org.opengroup.osdu.azure.di.LocalDevelopmentConfiguration;
import org.opengroup.osdu.azure.di.PodIdentityConfiguration;
import org.opengroup.osdu.azure.di.WorkloadIdentityConfiguration;
import org.opengroup.osdu.azure.cache.AzureServicePrincipleTokenServiceCache;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.IdToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.Arrays;
import java.util.List;

import static org.opengroup.osdu.azure.util.AuthUtils.getClientSecret;

/**
 * Azure Service Principle token service.
 */
@Component
@Lazy
public class AzureServicePrincipleTokenService {

    @Inject
    @Named("AAD_OBO_API")
    private String aadClientId;
    @Inject
    @Named("APP_DEV_SP_USERNAME")
    private String clientID;
    @Inject
    @Named("APP_DEV_SP_PASSWORD")
    private String clientSecret;
    @Inject
    @Named("APP_DEV_SP_TENANT_ID")
    private String tenantId;

    @Autowired
    private WorkloadIdentityConfiguration workloadIdentityConfiguration;
    @Autowired
    private PodIdentityConfiguration podIdentityConfiguration;
    @Autowired
    private AzureActiveDirectoryConfiguration aadConfiguration;
    @Autowired
    private SecretClient sc;
    @Autowired
    private Environment environment;
    @Autowired(required = false)  // Optional - only exists when "local" profile is active
    private LocalDevelopmentConfiguration localDevelopmentConfiguration;

    @Autowired
    private AzureServicePrincipal azureServicePrincipal;

    @Autowired
    private AzureServicePrincipleTokenServiceCache tokenCache;

    private static final String MANAGEMENT_SCOPE = "https://management.azure.com/.default";


    /**
     * @return service principle's authorization token
     */
    public String getAuthorizationToken() {
        return getAuthorizationToken(MANAGEMENT_SCOPE);
    }

    /**
     * Gets the authorization token for the specified scope.
     * @param scope The scope for which the token is requested
     * @return service principle's authorization token
     */
    public String getAuthorizationToken(final String scope) {
        String accessToken = "";
        try {
            String cacheKey = "token:" + scope;
            IdToken cachedToken = (IdToken) this.tokenCache.get(cacheKey);
            if (!IdToken.refreshToken(cachedToken)) {
                return cachedToken.getTokenValue();
            }

            // 1. If running local mode, try development credentials (Azure CLI, VS Code, IntelliJ)
            // 2. Try Workload Identity first (modern K8s method)
            // 3. Try Pod Identity second (legacy K8s method)
            // 4. Fallback to Service Principal with client secret
            if (environment.acceptsProfiles(Profiles.of("local"))
                && localDevelopmentConfiguration != null
                && localDevelopmentConfiguration.getEnableLocalCredentials()) {
                assertNotCloudEnvironment();
                accessToken = this.azureServicePrincipal.getLocalDevToken(aadClientId.concat("/.default"));
            } else if (workloadIdentityConfiguration.getIsEnabled()) {
                accessToken = this.azureServicePrincipal.getWIToken(aadClientId, scope);
            } else if (podIdentityConfiguration.getIsEnabled()) {
                accessToken = this.azureServicePrincipal.getMSIToken(scope);
            } else {
                // Fallback to Service Principal with client secret
                try {
                    accessToken = this.azureServicePrincipal.getIdToken(clientID, clientSecret, tenantId, aadClientId);
                } catch (AppException e) {
                    if (e.getError().getCode() != HttpStatus.SC_UNAUTHORIZED) {
                        throw e;
                    }
                    // Attempt to refresh client secret if unauthorized
                    String newClientSecret = getClientSecret(aadConfiguration, sc);
                    if (clientSecret.equals(newClientSecret)) {
                        throw e;
                    }
                    accessToken = this.azureServicePrincipal.getIdToken(clientID, newClientSecret, tenantId, aadClientId);
                    clientSecret = newClientSecret;
                }
            }

            IdToken idToken = IdToken.builder()
                    .tokenValue(accessToken)
                    .expirationTimeMillis(JWT.decode(accessToken).getExpiresAt().getTime())
                    .build();
            this.tokenCache.put(cacheKey, idToken);
        } catch (JWTDecodeException e) {
            this.tokenCache.clearAll();
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Persistence error", "Invalid token, error decoding", e);
        } catch (Exception e) {
            this.tokenCache.clearAll();
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Persistence error", "Error generating token", e);
        }

        return accessToken;
    }

    /**
     * Asserts that the application is not running in a cloud environment.
     * Called inline before every local-dev token acquisition so the check cannot be
     * bypassed by bean-loading order or component-scan configuration.
     * <p>
     * Note: CI/CD pipeline checks are handled by {@code LocalEnvironmentValidator} at
     * startup. This method focuses on cloud runtime indicators only.
     *
     * @throws IllegalStateException if a cloud environment indicator is detected
     */
    private static void assertNotCloudEnvironment() {
        List<String> cloudIndicators = Arrays.asList(
                "KUBERNETES_SERVICE_HOST",
                "WEBSITE_INSTANCE_ID",
                "IDENTITY_ENDPOINT",
                "MSI_ENDPOINT",
                "CONTAINER_APP_NAME",
                "FUNCTIONS_WORKER_RUNTIME",
                "ACI_INSTANCE_NAME"
        );
        for (String indicator : cloudIndicators) {
            if (System.getenv(indicator) != null) {
                throw new IllegalStateException(
                        "Refusing local-dev credentials: cloud environment indicator '" + indicator + "' is set. "
                        + "The 'local' profile must not be used outside of a developer workstation.");
            }
        }
    }
}
