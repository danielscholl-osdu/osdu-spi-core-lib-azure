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

package org.opengroup.osdu.azure.dependencies;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import org.opengroup.osdu.azure.KeyVaultFacade;
import org.opengroup.osdu.azure.di.AzureActiveDirectoryConfiguration;
import org.opengroup.osdu.common.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

import jakarta.inject.Named;

import static org.opengroup.osdu.azure.util.AuthUtils.getClientSecret;
/**
 * Bootstraps Azure dependencies for OSDU.
 */
@Configuration
public class AzureOSDUConfig {

    @Autowired
    private AzureActiveDirectoryConfiguration aadConfiguration;

    /**
     * Azure Services that require a credential for access can leverage this
     * {@link DefaultAzureCredential}. This specific implementation will first attempt
     * to authenticate using the following environment
     * variables:
     * AZURE_CLIENT_ID - service principal's app id
     * AZURE_TENANT_ID - id of the principal's Azure Active Directory tenant
     * AZURE_CLIENT_SECRET - one of the service principal's client secrets
     * <p>
     * If this strategy fails, the credential will fallback to using a Managed
     * Identity, if possible, and fail otherwise. This logic is implemented
     * in {@link com.azure.identity.DefaultAzureCredential}.
     * <p>
     * NOTE: The @Primary annotation is added to resolve bean conflicts with Spring Cloud Azure 5.x+
     * auto-configuration, which also provides a DefaultAzureCredential bean. This ensures backward
     * compatibility with services that haven't upgraded to Spring Cloud Azure 5.x yet.
     *
     * @return A configured credential suitable for authenticating with Azure
     * Services
     */
    @Bean
    @Primary
    @Lazy
    public DefaultAzureCredential azureCredential() {
        return new DefaultAzureCredentialBuilder().build();
    }

    /**
     * @param credential  A credential that can be used to authenticate with
     *                    KeyVault
     * @param keyVaultURL The URL of the KeyVault to configure
     * @return A configured KeyVault secret client
     */
    @Bean
    @Lazy
    public SecretClient keyVaultSecretsClient(
            final DefaultAzureCredential credential,
            final @Named("KEY_VAULT_URL") String keyVaultURL) {
        Validators.checkNotNull(credential, "Credential cannot be null");
        Validators.checkNotNullAndNotEmpty(keyVaultURL, "KV URL");
        return new SecretClientBuilder()
                .credential(credential)
                .vaultUrl(keyVaultURL)
                .buildClient();
    }

    /**
     * Azure Active Directory configuration bean.
     *
     * @return A configured KeyVault secret client
     * */
    @Bean
    @Named("AAD_OBO_API")
    public String aadClientID() {
        return aadConfiguration.getAadClientId();
    }

    /**
     * Service principal's app id configuration bean.
     *
     * @param sc KeyVault client
     * @return client id
     * */
    @Bean
    @Named("APP_DEV_SP_USERNAME")
    public String appDevSpUsername(final SecretClient sc) {
        if (aadConfiguration.getClientId() != null && !aadConfiguration.getClientId().isEmpty()) {
            return aadConfiguration.getClientId();
        }
        return KeyVaultFacade.getSecretWithValidation(sc, "app-dev-sp-username");
    }

    /**
     * Service Principle secrete configuration bean.
     *
     * @param sc KeyVault client
     * @return client secrete
     * */
    @Bean
    @Named("APP_DEV_SP_PASSWORD")
    public String appDevSpPassword(final SecretClient sc) {
        return getClientSecret(aadConfiguration, sc);
    }


    /**
     * Azure Active Directory principle Tenant Id configuration bean.
     *
     * @param sc KeyVault client
     * @return tenant id
     * */
    @Bean
    @Named("APP_DEV_SP_TENANT_ID")
    public String appDevSpTenantId(final SecretClient sc) {
        if (aadConfiguration.getTenantId() != null && !aadConfiguration.getTenantId().isEmpty()) {
            return aadConfiguration.getTenantId();
        }
        return KeyVaultFacade.getSecretWithValidation(sc, "app-dev-sp-tenant-id");
    }
}
