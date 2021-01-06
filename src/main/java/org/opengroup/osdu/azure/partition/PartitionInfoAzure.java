package org.opengroup.osdu.azure.partition;

import com.azure.security.keyvault.secrets.SecretClient;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.opengroup.osdu.azure.KeyVaultFacade;
import org.opengroup.osdu.core.common.partition.Property;

/**
 * Azure data partition variables.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PartitionInfoAzure {

    @SerializedName("id")
    private Property idConfig;

    @SerializedName("name")
    private Property nameConfig;

    @SerializedName("compliance-ruleset")
    private Property complianceRulesetConfig;

    @SerializedName("elastic-endpoint")
    private Property elasticEndpointConfig;

    @SerializedName("elastic-username")
    private Property elasticUsernameConfig;

    @SerializedName("elastic-password")
    private Property elasticPasswordConfig;

    @Builder.Default
    @SerializedName("elastic-ssl-enabled")
    private Property elasticSslEnabledConfig = Property.builder().value(true).build();

    @SerializedName("cosmos-connection")
    private Property cosmosConnectionConfig;

    @SerializedName("cosmos-endpoint")
    private Property cosmosEndpointConfig;

    @SerializedName("cosmos-primary-key")
    private Property cosmosPrimaryKeyConfig;

    @SerializedName("sb-connection")
    private Property sbConnectionConfig;

    @SerializedName("storage-account-key")
    private Property storageAccountKeyConfig;

    @SerializedName("storage-account-name")
    private Property storageAccountNameConfig;

    @SerializedName("sb-namespace")
    private Property sbNamespaceConfig;

    @SerializedName("eventgrid-recordstopic")
    private Property eventGridRecordsTopicEndpointConfig;

    @SerializedName("eventgrid-recordstopic-accesskey")
    private Property eventGridRecordsTopicAccessKeyConfig;

    private Property servicePrincipalAppIdConfig = Property.builder().value("app-dev-sp-username").sensitive(true).build();

    private SecretClient secretClient;

    /**
     * @return partition id
     */
    public String getId() {
        return String.valueOf(this.getIdConfig().getValue());
    }

    /**
     * @return partition name
     */
    public String getName() {
        return String.valueOf(this.getNameConfig().getValue());
    }

    /**
     * @return partition compliance ruleset
     */
    public String getComplianceRuleset() {
        return String.valueOf(this.getComplianceRulesetConfig().getValue());
    }

    /**
     * @return partition service principle id
     */
    public String getServicePrincipalAppId() {
        if (this.getServicePrincipalAppIdConfig().isSensitive()) {
            return getSecret(this.getServicePrincipalAppIdConfig());
        }
        return String.valueOf(this.getServicePrincipalAppIdConfig().getValue());
    }

    /**
     * @return partition elastic endpoint
     */
    public String getElasticEndpoint() {
        if (this.getElasticEndpointConfig().isSensitive()) {
            return getSecret(this.getElasticEndpointConfig());
        }
        return String.valueOf(this.getElasticEndpointConfig().getValue());
    }

    /**
     * @return partition elastic username
     */
    public String getElasticUsername() {
        if (this.getElasticUsernameConfig().isSensitive()) {
            return getSecret(this.getElasticUsernameConfig());
        }
        return String.valueOf(this.getElasticUsernameConfig().getValue());
    }

    /**
     * @return partition elastic password
     */
    public String getElasticPassword() {
        if (this.getElasticPasswordConfig().isSensitive()) {
            return getSecret(this.getElasticPasswordConfig());
        }
        return String.valueOf(this.getElasticPasswordConfig().getValue());
    }

    /**
     * @return partition elastic ssl enabled
     */
    public String getElasticSslEnabled() {
        if (this.getElasticSslEnabledConfig().isSensitive()) {
            return getSecret(this.getElasticSslEnabledConfig());
        }
        return String.valueOf(this.getElasticSslEnabledConfig().getValue());
    }

    /**
     * @return partition cosmosdb connection
     */
    public String getCosmosConnection() {
        if (this.getCosmosConnectionConfig().isSensitive()) {
            return getSecret(this.getCosmosConnectionConfig());
        }
        return String.valueOf(this.getCosmosConnectionConfig().getValue());
    }

    /**
     * @return partition cosmosdb endpoint
     */
    public String getCosmosEndpoint() {
        if (this.getCosmosEndpointConfig().isSensitive()) {
            return getSecret(this.getCosmosEndpointConfig());
        }
        return String.valueOf(this.getCosmosEndpointConfig().getValue());
    }

    /**
     * @return partition cosmosdb primary-key
     */
    public String getCosmosPrimaryKey() {
        if (this.getCosmosPrimaryKeyConfig().isSensitive()) {
            return getSecret(this.getCosmosPrimaryKeyConfig());
        }
        return String.valueOf(this.getCosmosPrimaryKeyConfig().getValue());
    }

    /**
     * @return partition service bus connection string
     */
    public String getSbConnection() {
        if (this.getSbConnectionConfig().isSensitive()) {
            return getSecret(this.getSbConnectionConfig());
        }
        return String.valueOf(this.getSbConnectionConfig().getValue());
    }

    /**
     * @return partition service bus namespace
     */
    public String getSbNamespace() {
        if (this.getSbNamespaceConfig().isSensitive()) {
            return getSecret(this.getSbNamespaceConfig());
        }
        return String.valueOf(this.getSbNamespaceConfig().getValue());
    }

    /**
     * @return partition storage account key
     */
    public String getStorageAccountKey() {
        if (this.getStorageAccountKeyConfig().isSensitive()) {
            return getSecret(this.getStorageAccountKeyConfig());
        }
        return String.valueOf(this.getStorageAccountKeyConfig().getValue());
    }

    /**
     * @return partition storage account name
     */
    public String getStorageAccountName() {
        if (this.getStorageAccountNameConfig().isSensitive()) {
            return getSecret(this.getStorageAccountNameConfig());
        }
        return String.valueOf(this.getStorageAccountNameConfig().getValue());
    }

    /**
     * @return partition event grid topic endpoint
     */
    public String getEventGridRecordsTopicEndpoint() {
        if (this.getEventGridRecordsTopicEndpointConfig().isSensitive()) {
            return getSecret(this.getEventGridRecordsTopicEndpointConfig());
        }
        return String.valueOf(this.getEventGridRecordsTopicEndpointConfig().getValue());
    }

    /**
     * @return partition event grid topic key
     */
    public String getEventGridRecordsTopicAccessKey() {
        if (this.getEventGridRecordsTopicAccessKeyConfig().isSensitive()) {
            return getSecret(this.getEventGridRecordsTopicAccessKeyConfig());
        }
        return String.valueOf(this.getEventGridRecordsTopicAccessKeyConfig().getValue());
    }

    /**
     * @param client KV secret client
     */
    public void configureSecretClient(final SecretClient client) {
        this.secretClient = client;
    }

    /**
     * @param p partition property
     * @return secret value
     */
    private String getSecret(final Property p) {
        return KeyVaultFacade.getSecretWithValidation(this.secretClient, String.valueOf(p.getValue()));
    }
}