package org.opengroup.osdu.azure.cosmosdb;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.ThrottlingRetryOptions;
import com.azure.identity.DefaultAzureCredential;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.cosmosdb.system.config.SystemCosmosConfig;
import org.opengroup.osdu.azure.di.CosmosRetryConfiguration;
import org.opengroup.osdu.azure.di.MSIConfiguration;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.only;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
public class CosmosClientFactoryImplTest {

    @Mock
    private Map<String, CosmosClient> cosmosClientMap;
    @Mock
    private PartitionServiceClient partitionService;
    @Mock
    private MSIConfiguration msiConfiguration;
    @Mock
    private CosmosClient cosmosClient;
    @Mock
    private CosmosAsyncClient cosmosAsyncClient;
    @Mock
    private PartitionInfoAzure partitionInfo;
    @Mock
    private CosmosRetryConfiguration retryConfig;
    @Mock
    private ThrottlingRetryOptions throttlingRetryOptions;
    @Mock
    private DefaultAzureCredential defaultAzureCredential;
    @Mock
    private ICosmosClientBuilderFactory cosmosClientBuilderFactory;
    @Mock
    private CosmosClientBuilder cosmosClientBuilder;
    @Mock
    private SystemCosmosConfig systemCosmosConfig;
    @Mock
    private SecretClient secretClient;
    @Mock
    private KeyVaultSecret cosmosDbAccountSecret;
    @Mock
    private KeyVaultSecret cosmosPrimaryKeySecret;
    @InjectMocks
    private CosmosClientFactoryImpl sut;

    private static final String FIELD_NAME_COSMOS_CLIENT_MAP = "cosmosClientMap";
    private static final String FIELD_NAME_COSMOS_ASYNC_CLIENT_MAP = "cosmosAsyncClientMap";

    private static final String FIELD_NAME_MAX_CONNECTION_PER_ENDPOINT = "maxConnectionPerEndpoint";
    private static final Integer MAX_CONNECTION_PER_ENDPOINT = 10;
    private static final String PARTITION_ID = "dataPartitionId";
    private static final String PARTITION_ID_ANOTHER = "anotherDataPartitionId";

    private static final String COSMOS_ENDPOINT_URL = "cosmosEndpointUrl";
    private static final String COSMOS_PRIMARY_KEY = "primaryKey";
    private static final String COSMOS_PRIMARY_VALUE = "primaryValue";
    private static final String COSMOS_DB_ACCOUNT_KEY = "cosmosDbAccountKey";
    private static final String COSMOS_DB_ACCOUNT_VALUE = "cosmosDbAccountValue";

    @BeforeEach
    void init() {
        CoreLoggerFactory.resetFactory();
        initMocks(this);
        sut.initialize();
        setFieldValue(FIELD_NAME_MAX_CONNECTION_PER_ENDPOINT, MAX_CONNECTION_PER_ENDPOINT);
    }

    @Test
    public void getClient_shouldReturnCosmosClient_whenMsiConfigurationDisabled() {
        when(partitionInfo.getCosmosEndpoint()).thenReturn(COSMOS_ENDPOINT_URL);
        when(partitionInfo.getCosmosPrimaryKey()).thenReturn(COSMOS_PRIMARY_KEY);
        when(partitionService.getPartition(PARTITION_ID)).thenReturn(partitionInfo);

        when(msiConfiguration.getIsEnabled()).thenReturn(false);
        when(retryConfig.getThrottlingRetryOptions()).thenReturn(throttlingRetryOptions);
        when(cosmosClientBuilderFactory.getCosmosClientBuilder()).thenReturn(cosmosClientBuilder);

        when(cosmosClientBuilder.endpoint(COSMOS_ENDPOINT_URL)).thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder.key(COSMOS_PRIMARY_KEY)).thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder.throttlingRetryOptions(throttlingRetryOptions)).thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder
                .directMode(argThat(connectionConfig -> MAX_CONNECTION_PER_ENDPOINT.equals(connectionConfig.getMaxConnectionsPerEndpoint()))))
                .thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder.buildClient()).thenReturn(cosmosClient);

        assertNotNull(sut.getClient(PARTITION_ID));
    }

    @Test
    public void getClient_shouldReturnCosmosClient_whenMsiConfigurationEnabled() {
        when(partitionInfo.getCosmosEndpoint()).thenReturn(COSMOS_ENDPOINT_URL);
        when(partitionService.getPartition(PARTITION_ID)).thenReturn(partitionInfo);

        when(msiConfiguration.getIsEnabled()).thenReturn(true);
        when(cosmosClientBuilderFactory.getCosmosClientBuilder()).thenReturn(cosmosClientBuilder);
        when(retryConfig.getThrottlingRetryOptions()).thenReturn(throttlingRetryOptions);

        when(cosmosClientBuilder.endpoint(COSMOS_ENDPOINT_URL)).thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder.credential(defaultAzureCredential)).thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder.throttlingRetryOptions(throttlingRetryOptions)).thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder
                .directMode(argThat(connectionConfig -> MAX_CONNECTION_PER_ENDPOINT.equals(connectionConfig.getMaxConnectionsPerEndpoint()))))
                .thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder.buildClient()).thenReturn(cosmosClient);

        assertNotNull(sut.getClient(PARTITION_ID));
    }

    @Test
    public void getClient_shouldReturnCosmosClient_whenMaxConnectionsPerEndpointIsNull() {
        setFieldValue("maxConnectionPerEndpoint", null);
        when(partitionInfo.getCosmosEndpoint()).thenReturn(COSMOS_ENDPOINT_URL);
        when(partitionInfo.getCosmosPrimaryKey()).thenReturn(COSMOS_PRIMARY_KEY);
        when(partitionService.getPartition(PARTITION_ID)).thenReturn(partitionInfo);

        when(msiConfiguration.getIsEnabled()).thenReturn(false);
        when(retryConfig.getThrottlingRetryOptions()).thenReturn(throttlingRetryOptions);
        when(cosmosClientBuilderFactory.getCosmosClientBuilder()).thenReturn(cosmosClientBuilder);

        when(cosmosClientBuilder.endpoint(COSMOS_ENDPOINT_URL)).thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder.key(COSMOS_PRIMARY_KEY)).thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder.throttlingRetryOptions(throttlingRetryOptions)).thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder.buildClient()).thenReturn(cosmosClient);

        assertNotNull(sut.getClient(PARTITION_ID));
    }

    @Test
    public void getClient_shouldReturnAsyncCosmosClient_whenMsiConfigurationDisabled() {
        when(partitionInfo.getCosmosEndpoint()).thenReturn(COSMOS_ENDPOINT_URL);
        when(partitionInfo.getCosmosPrimaryKey()).thenReturn(COSMOS_PRIMARY_KEY);
        when(partitionService.getPartition(PARTITION_ID)).thenReturn(partitionInfo);

        when(msiConfiguration.getIsEnabled()).thenReturn(false);
        when(retryConfig.getThrottlingRetryOptions()).thenReturn(throttlingRetryOptions);
        when(cosmosClientBuilderFactory.getCosmosClientBuilder()).thenReturn(cosmosClientBuilder);

        when(cosmosClientBuilder.endpoint(COSMOS_ENDPOINT_URL)).thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder.key(COSMOS_PRIMARY_KEY)).thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder.throttlingRetryOptions(throttlingRetryOptions)).thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder
                .directMode(argThat(connectionConfig -> MAX_CONNECTION_PER_ENDPOINT.equals(connectionConfig.getMaxConnectionsPerEndpoint()))))
                .thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder.buildAsyncClient()).thenReturn(cosmosAsyncClient);

        assertNotNull(sut.getAsyncClient(PARTITION_ID));
    }

    @Test
    public void getClient_shouldReturnCosmosAsyncClient_whenMsiConfigurationEnabled() {
        when(partitionInfo.getCosmosEndpoint()).thenReturn(COSMOS_ENDPOINT_URL);
        when(partitionService.getPartition(PARTITION_ID)).thenReturn(partitionInfo);

        when(msiConfiguration.getIsEnabled()).thenReturn(true);
        when(cosmosClientBuilderFactory.getCosmosClientBuilder()).thenReturn(cosmosClientBuilder);
        when(retryConfig.getThrottlingRetryOptions()).thenReturn(throttlingRetryOptions);

        when(cosmosClientBuilder.endpoint(COSMOS_ENDPOINT_URL)).thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder.credential(defaultAzureCredential)).thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder.throttlingRetryOptions(throttlingRetryOptions)).thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder
                .directMode(argThat(connectionConfig -> MAX_CONNECTION_PER_ENDPOINT.equals(connectionConfig.getMaxConnectionsPerEndpoint()))))
                .thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder.buildAsyncClient()).thenReturn(cosmosAsyncClient);

        assertNotNull(sut.getAsyncClient(PARTITION_ID));
    }

    @Test
    public void getClient_shouldReturnSystemCosmosClient_whenMsiConfigurationDisabled() {
        when(systemCosmosConfig.getCosmosDBAccountKeyName()).thenReturn(COSMOS_DB_ACCOUNT_KEY);
        when(systemCosmosConfig.getCosmosPrimaryKeyName()).thenReturn(COSMOS_PRIMARY_KEY);

        when(secretClient.getSecret(COSMOS_DB_ACCOUNT_KEY)).thenReturn(cosmosDbAccountSecret);
        when(secretClient.getSecret(COSMOS_PRIMARY_KEY)).thenReturn(cosmosPrimaryKeySecret);
        when(cosmosDbAccountSecret.getValue()).thenReturn(COSMOS_DB_ACCOUNT_VALUE);
        when(cosmosPrimaryKeySecret.getValue()).thenReturn(COSMOS_PRIMARY_VALUE);

        when(msiConfiguration.getIsEnabled()).thenReturn(false);
        when(cosmosClientBuilderFactory.getCosmosClientBuilder()).thenReturn(cosmosClientBuilder);

        when(cosmosClientBuilder.endpoint(COSMOS_DB_ACCOUNT_VALUE)).thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder.key(COSMOS_PRIMARY_VALUE)).thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder
                .directMode(argThat(connectionConfig -> MAX_CONNECTION_PER_ENDPOINT.equals(connectionConfig.getMaxConnectionsPerEndpoint()))))
                .thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder.buildClient()).thenReturn(cosmosClient);

        assertNotNull(sut.getSystemClient());
    }

    @Test
    public void getClient_shouldReturnSystemCosmosClient_whenMsiConfigurationEnabled() {
        when(systemCosmosConfig.getCosmosDBAccountKeyName()).thenReturn(COSMOS_DB_ACCOUNT_KEY);

        when(secretClient.getSecret(COSMOS_DB_ACCOUNT_KEY)).thenReturn(cosmosDbAccountSecret);
        when(cosmosDbAccountSecret.getValue()).thenReturn(COSMOS_DB_ACCOUNT_VALUE);

        when(msiConfiguration.getIsEnabled()).thenReturn(true);
        when(cosmosClientBuilderFactory.getCosmosClientBuilder()).thenReturn(cosmosClientBuilder);

        when(cosmosClientBuilder.endpoint(COSMOS_DB_ACCOUNT_VALUE)).thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder.credential(defaultAzureCredential)).thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder
                .directMode(argThat(connectionConfig -> MAX_CONNECTION_PER_ENDPOINT.equals(connectionConfig.getMaxConnectionsPerEndpoint()))))
                .thenReturn(cosmosClientBuilder);
        when(cosmosClientBuilder.buildClient()).thenReturn(cosmosClient);

        assertNotNull(sut.getSystemClient());
    }

    @Test
    public void should_throwException_given_nullDataPartitionId() {
        try {
            this.sut.getClient(null);
        } catch (NullPointerException ex) {
            assertEquals("dataPartitionId cannot be null!", ex.getMessage());
        } catch (Exception ex) {
            fail("Should not get any other exception. Received " + ex.getClass());
        }
    }

    @Test
    public void should_throwException_given_emptyDataPartitionId() {
        try {
            this.sut.getClient("");
        } catch (IllegalArgumentException ex) {
            assertEquals("dataPartitionId cannot be empty!", ex.getMessage());
        } catch (Exception ex) {
            fail("Should not get any other exception. Received " + ex.getClass());
        }
    }

    @Test
    public void shouldCloseAllClients() {
        var syncCache = new HashMap<String, CosmosClient>() {{
            put(PARTITION_ID, cosmosClient);
            put(PARTITION_ID_ANOTHER, null);
        }};
        var asyncCache = new HashMap<String, CosmosAsyncClient>() {{
            put(PARTITION_ID, cosmosAsyncClient);
            put(PARTITION_ID_ANOTHER, null);
        }};
        setFieldValue(FIELD_NAME_COSMOS_CLIENT_MAP, syncCache);
        setFieldValue(FIELD_NAME_COSMOS_ASYNC_CLIENT_MAP, asyncCache);

        sut.closeAllClients();

        verify(cosmosClient, only()).close();
        verify(cosmosAsyncClient, only()).close();
    }

    private void setFieldValue(String fieldName, Object fieldValue) {
        Field field = ReflectionUtils.findField(CosmosClientFactoryImpl.class, fieldName);
        field.setAccessible(true);
        ReflectionUtils.setField(field, sut, fieldValue);
    }
}
