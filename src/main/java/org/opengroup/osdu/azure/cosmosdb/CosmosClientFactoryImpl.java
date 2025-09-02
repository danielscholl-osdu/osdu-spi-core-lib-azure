package org.opengroup.osdu.azure.cosmosdb;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.DirectConnectionConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.PostConstruct;

import com.azure.cosmos.ThrottlingRetryOptions;
import com.azure.identity.DefaultAzureCredential;
import com.azure.security.keyvault.secrets.SecretClient;
import jakarta.annotation.PreDestroy;
import org.opengroup.osdu.azure.KeyVaultFacade;
import org.opengroup.osdu.azure.cosmosdb.system.config.SystemCosmosConfig;
import org.opengroup.osdu.azure.di.MSIConfiguration;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.azure.di.CosmosRetryConfiguration;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.common.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Implementation for ICosmosClientFactory.
 */
@Component
@Lazy
public class CosmosClientFactoryImpl implements ICosmosClientFactory {
    private static final String LOGGER_NAME = CosmosClientFactoryImpl.class.getName();
    private static final String SYSTEM_COSMOS_CACHE_KEY = "system_cosmos";
    private static final String DATA_PARTITION_ID = "dataPartitionId";

    @Lazy
    @Autowired
    private PartitionServiceClient partitionService;

    @Autowired
    private SecretClient secretClient;

    @Autowired
    private SystemCosmosConfig systemCosmosConfig;

    private Map<String, CosmosClient> cosmosClientMap;
    private Map<String, CosmosAsyncClient> cosmosAsyncClientMap;

    @Autowired
    private CosmosRetryConfiguration cosmosRetryConfiguration;

    @Autowired
    private MSIConfiguration msiConfiguration;

    @Autowired
    private DefaultAzureCredential defaultAzureCredential;

    @Autowired
    private ICosmosClientBuilderFactory cosmosClientBuilderFactory;

    @Value("${cosmos.maxConnectionsPerEndpoint}")
    private Integer maxConnectionPerEndpoint;

    /**
     * Initializes the private variables as required.
     */
    @PostConstruct
    public void initialize() {
        cosmosClientMap = new ConcurrentHashMap<>();
        cosmosAsyncClientMap = new ConcurrentHashMap<>();
    }

    /**
     * @param dataPartitionId Data Partition Id
     * @return Cosmos Client instance
     */
    @Override
    public CosmosClient getClient(final String dataPartitionId) {
        Validators.checkNotNullAndNotEmpty(dataPartitionId, DATA_PARTITION_ID);
        String cacheKey = String.format("%s-cosmosClient", dataPartitionId);

        return this.cosmosClientMap.computeIfAbsent(cacheKey, cosmosClient -> createCosmosClient(dataPartitionId));
    }

    /**
     * @param dataPartitionId Data Partition Id
     * @return Cosmos Async Client instance
     */
    @Override
    public CosmosAsyncClient getAsyncClient(final String dataPartitionId) {
        Validators.checkNotNullAndNotEmpty(dataPartitionId, DATA_PARTITION_ID);
        String cacheKey = String.format("%s-cosmosAsyncClient", dataPartitionId);

        return this.cosmosAsyncClientMap.computeIfAbsent(cacheKey, cosmosClient -> createCosmosAsyncClient(dataPartitionId));
    }

    /**
     * @return Cosmos client instance for system resources.
     */
    @Override
    public CosmosClient getSystemClient() {

        return this.cosmosClientMap.computeIfAbsent(
                SYSTEM_COSMOS_CACHE_KEY, cosmosClient -> createSystemCosmosClient()
        );
    }

    /**
     *
     * @param dataPartitionId Data Partition Id
     * @return Cosmos Client Instance
     */
    private CosmosClient createCosmosClient(final String dataPartitionId) {
        PartitionInfoAzure pi = this.partitionService.getPartition(dataPartitionId);

        ThrottlingRetryOptions throttlingRetryOptions = cosmosRetryConfiguration.getThrottlingRetryOptions();
        CosmosClientBuilder cosmosClientBuilder;

        if (msiConfiguration.getIsEnabled()) {
            cosmosClientBuilder = cosmosClientBuilderFactory.getCosmosClientBuilder()
                    .endpoint(pi.getCosmosEndpoint())
                    .credential(defaultAzureCredential)
                    .throttlingRetryOptions(throttlingRetryOptions);
        } else {
            cosmosClientBuilder = cosmosClientBuilderFactory.getCosmosClientBuilder()
                    .endpoint(pi.getCosmosEndpoint())
                    .key(pi.getCosmosPrimaryKey())
                    .throttlingRetryOptions(throttlingRetryOptions);
        }
        setDirectMode(cosmosClientBuilder);

        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME)
                .debug("Created CosmosClient for dataPartition {}.", dataPartitionId);
        return cosmosClientBuilder.buildClient();
    }

    /**
     *
     * @param dataPartitionId Data Partition Id
     * @return Cosmos Async Client Instance
     */
    private CosmosAsyncClient createCosmosAsyncClient(final String dataPartitionId) {
        PartitionInfoAzure pi = this.partitionService.getPartition(dataPartitionId);

        ThrottlingRetryOptions throttlingRetryOptions = cosmosRetryConfiguration.getThrottlingRetryOptions();
        CosmosClientBuilder cosmosAsyncClientBuilder;

        if (msiConfiguration.getIsEnabled()) {
            cosmosAsyncClientBuilder = cosmosClientBuilderFactory.getCosmosClientBuilder()
                    .endpoint(pi.getCosmosEndpoint())
                    .credential(defaultAzureCredential)
                    .throttlingRetryOptions(throttlingRetryOptions);
        } else {
            cosmosAsyncClientBuilder = cosmosClientBuilderFactory.getCosmosClientBuilder()
                    .endpoint(pi.getCosmosEndpoint())
                    .key(pi.getCosmosPrimaryKey())
                    .throttlingRetryOptions(throttlingRetryOptions);
        }
        setDirectMode(cosmosAsyncClientBuilder);
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME)
                .debug("Created CosmosAsyncClient for dataPartition {}.", dataPartitionId);
        return cosmosAsyncClientBuilder.buildAsyncClient();
    }

    /**
     * Method to create the cosmos client for system resources.
     * @return cosmos client.
     */
    private CosmosClient createSystemCosmosClient() {

        CosmosClientBuilder cosmosClientBuilder;
        if (msiConfiguration.getIsEnabled()) {
            cosmosClientBuilder = cosmosClientBuilderFactory.getCosmosClientBuilder()
                    .endpoint(getSecret(systemCosmosConfig.getCosmosDBAccountKeyName()))
                    .credential(defaultAzureCredential);
        } else {
            cosmosClientBuilder = cosmosClientBuilderFactory.getCosmosClientBuilder()
                    .endpoint(getSecret(systemCosmosConfig.getCosmosDBAccountKeyName()))
                    .key(getSecret(systemCosmosConfig.getCosmosPrimaryKeyName()));
        }
        setDirectMode(cosmosClientBuilder);
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME)
                .debug("Created CosmosClient for system resources");

        return cosmosClientBuilder.buildClient();
    }

    /**
     * @param keyName Name of the key to be read from key vault.
     * @return secret value
     */
    private String getSecret(final String keyName) {
        return KeyVaultFacade.getSecretWithValidation(secretClient, keyName);
    }

    /**
     * @return DirectConnectionConfig object with defined max connection per endpoint if maxConnectionPerEndpoint not null
     */
    private DirectConnectionConfig getDirectConnectionConfig() {
        return maxConnectionPerEndpoint == null ? null
                : DirectConnectionConfig.getDefaultConfig().setMaxConnectionsPerEndpoint(maxConnectionPerEndpoint);
    }

    /**
     * This method set connectionConfig for CosmosClientBuilder if connectionConfig is not null.
     * @param builder CosmosClientBuilder to build CosmosClient or AsyncCosmosClient
     */
    private void setDirectMode(final CosmosClientBuilder builder) {
        DirectConnectionConfig connectionConfig = getDirectConnectionConfig();
        if (connectionConfig != null) {
            builder.directMode(connectionConfig);
        }
    }

    /**
     * This method close all cosmos clients before the bean destruction.
     */
    @PreDestroy
    public void closeAllClients() {
        if (cosmosClientMap != null) {
            for (Map.Entry<String, CosmosClient> entry: cosmosClientMap.entrySet()) {
                if (entry.getValue() != null) {
                    try {
                        entry.getValue().close();
                    } catch (Exception e) {
                        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME)
                                .warn("Failed to close CosmosClient for partition: " + entry.getKey(), e);
                    }
                }
            }
        }
        if (cosmosAsyncClientMap != null) {
            for (Map.Entry<String, CosmosAsyncClient> entry: cosmosAsyncClientMap.entrySet()) {
                if (entry.getValue() != null) {
                    try {
                        entry.getValue().close();
                    } catch (Exception e) {
                        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME)
                                .warn("Failed to close CosmosAsyncClient for partition: " + entry.getKey(), e);
                    }
                }
            }
        }
    }
}
