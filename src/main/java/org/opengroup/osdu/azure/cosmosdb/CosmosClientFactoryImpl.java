package org.opengroup.osdu.azure.cosmosdb;

import java.time.Duration;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.DirectConnectionConfig;

import com.azure.cosmos.CosmosDiagnosticsThresholds;
import com.azure.cosmos.ThrottlingRetryOptions;
import com.azure.cosmos.models.CosmosClientTelemetryConfig;
import com.azure.cosmos.models.CosmosMicrometerMetricsOptions;
import com.azure.identity.DefaultAzureCredential;
import com.azure.security.keyvault.secrets.SecretClient;
import org.opengroup.osdu.azure.KeyVaultFacade;
import org.opengroup.osdu.azure.cosmosdb.cache.async.CosmosAsyncClientCache;
import org.opengroup.osdu.azure.cosmosdb.cache.sync.CosmosClientCache;
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

    @Autowired
    private CosmosClientCache cosmosClientCache;
    @Autowired
    private CosmosAsyncClientCache cosmosAsyncClientCache;

    @Autowired
    private CosmosRetryConfiguration cosmosRetryConfiguration;

    @Autowired
    private MSIConfiguration msiConfiguration;

    @Autowired
    private DefaultAzureCredential defaultAzureCredential;

    @Autowired
    private ICosmosClientBuilderFactory cosmosClientBuilderFactory;

    @Value("${cosmos.maxConnectionsPerEndpoint:130}")
    private Integer maxConnectionPerEndpoint;

    // Telemetry thresholds to retain only operationally useful Cosmos metrics while preventing
    // Prometheus cardinality explosions from transport-level histograms.
    @Value("${azure.cosmos.metrics.requestChargeThreshold:10}")
    private float requestChargeThreshold;
    @Value("${azure.cosmos.metrics.pointOperationLatencyThresholdMs:1000}")
    private long pointOperationLatencyThresholdMs;
    @Value("${azure.cosmos.metrics.nonPointOperationLatencyThresholdMs:3000}")
    private long nonPointOperationLatencyThresholdMs;
    private static final float DEFAULT_REQUEST_CHARGE_THRESHOLD = 10.0f;
    private static final long DEFAULT_POINT_OPERATION_LATENCY_THRESHOLD_MS = 1000L;
    private static final long DEFAULT_NON_POINT_OPERATION_LATENCY_THRESHOLD_MS = 3000L;

    /**
     * @param dataPartitionId Data Partition Id
     * @return Cosmos Client instance
     */
    @Override
    public CosmosClient getClient(final String dataPartitionId) {
        Validators.checkNotNullAndNotEmpty(dataPartitionId, DATA_PARTITION_ID);
        String cacheKey = String.format("%s-cosmosClient", dataPartitionId);

        return this.cosmosClientCache.computeIfAbsent(cacheKey, cosmosClient -> createCosmosClient(dataPartitionId));
    }

    /**
     * @param dataPartitionId Data Partition Id
     * @return Cosmos Async Client instance
     */
    @Override
    public CosmosAsyncClient getAsyncClient(final String dataPartitionId) {
        Validators.checkNotNullAndNotEmpty(dataPartitionId, DATA_PARTITION_ID);
        String cacheKey = String.format("%s-cosmosAsyncClient", dataPartitionId);

        return this.cosmosAsyncClientCache.computeIfAbsent(cacheKey, cosmosClient -> createCosmosAsyncClient(dataPartitionId));
    }

    /**
     * @return Cosmos client instance for system resources.
     */
    @Override
    public CosmosClient getSystemClient() {

        return this.cosmosClientCache.computeIfAbsent(
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
        setClientTelemetry(cosmosClientBuilder);

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
        setClientTelemetry(cosmosAsyncClientBuilder);

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
        setClientTelemetry(cosmosClientBuilder);

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
     * Configure Cosmos SDK telemetry to retain operationally useful metrics
     * (requests, latency, RU consumption, response codes) while reducing
     * cardinality from transport-level RNTBD histograms.
     *
     * @param builder CosmosClientBuilder to configure
     */
    private void setClientTelemetry(final CosmosClientBuilder builder) {
        if (requestChargeThreshold <= 0) {
            requestChargeThreshold = DEFAULT_REQUEST_CHARGE_THRESHOLD;
        }
        if (pointOperationLatencyThresholdMs <= 0) {
            pointOperationLatencyThresholdMs = DEFAULT_POINT_OPERATION_LATENCY_THRESHOLD_MS;
        }
        if (nonPointOperationLatencyThresholdMs <= 0) {
            nonPointOperationLatencyThresholdMs = DEFAULT_NON_POINT_OPERATION_LATENCY_THRESHOLD_MS;
        }

        // Disable histograms to avoid cardinality explosions while keeping aggregate request/RU metrics.
        // Detailed transport diagnostics are only emitted for operations crossing the configured thresholds.
        CosmosMicrometerMetricsOptions metricsOptions =
            new CosmosMicrometerMetricsOptions()
                .enableHistogramsByDefault(false)
                .applyDiagnosticThresholdsForTransportLevelMeters(true);

        // Only requests above the RU or latency thresholds produce detailed transport diagnostics.
        // point ops: read-by-id, upsert, delete
        // non-point ops: queries, scans, cross-partition work
        CosmosDiagnosticsThresholds thresholds =
            new CosmosDiagnosticsThresholds()
                .setRequestChargeThreshold(requestChargeThreshold)
                .setPointOperationLatencyThreshold(Duration.ofMillis(pointOperationLatencyThresholdMs))
                .setNonPointOperationLatencyThreshold(Duration.ofMillis(nonPointOperationLatencyThresholdMs));

        CosmosClientTelemetryConfig telemetryConfig =
            new CosmosClientTelemetryConfig()
                .metricsOptions(metricsOptions)
                .diagnosticsThresholds(thresholds);

         CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("Applying Cosmos telemetry configuration: histograms disabled, transport metrics threshold-filtered.");

        builder.clientTelemetryConfig(telemetryConfig);
    }
}
