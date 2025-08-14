package org.opengroup.osdu.azure.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.azure.security.keyvault.secrets.SecretClient;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.azure.di.AzureActiveDirectoryConfiguration;
import org.opengroup.osdu.azure.di.LocalDevelopmentConfiguration;
import org.opengroup.osdu.azure.di.PodIdentityConfiguration;
import org.opengroup.osdu.azure.di.WorkloadIdentityConfiguration;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.IdToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.HashMap;
import java.util.Map;

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

    private final AzureServicePrincipal azureServicePrincipal = new AzureServicePrincipal();

    private Map<String, Object> tokenCache = new HashMap<>();

    /**
     * @return service principle's authorization token
     */
    public String getAuthorizationToken() {
        String accessToken = "";
        try {
            IdToken cachedToken = (IdToken) this.tokenCache.get("token");
            if (!IdToken.refreshToken(cachedToken)) {
                return cachedToken.getTokenValue();
            }

            // Check if running in local development mode
            if (environment.acceptsProfiles(Profiles.of("local"))
                && localDevelopmentConfiguration != null
                && localDevelopmentConfiguration.getEnableLocalCredentials()) {
                // Use local development credentials (Azure CLI, VS Code, IntelliJ)
                accessToken = this.azureServicePrincipal.getLocalDevToken(aadClientId.concat("/.default"));
            } else if (workloadIdentityConfiguration.getIsEnabled()) {
                // Try Workload Identity (modern K8s method)
                accessToken = this.azureServicePrincipal.getWIToken(aadClientId);
            } else if (podIdentityConfiguration.getIsEnabled()) {
                // Try Pod Identity (legacy K8s method)
                accessToken = this.azureServicePrincipal.getMSIToken();
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
            this.tokenCache.put("token", idToken);
        } catch (JWTDecodeException e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Persistence error", "Invalid token, error decoding", e);
        } catch (Exception e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Persistence error", "Error generating token", e);
        }

        return accessToken;
    }
}
