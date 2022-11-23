package org.opengroup.osdu.azure.cosmosdb;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosBulkExecutionOptions;
import com.azure.cosmos.models.CosmosBulkItemResponse;
import com.azure.cosmos.models.CosmosBulkOperations;
import com.azure.cosmos.models.CosmosItemOperation;
import com.azure.cosmos.models.PartitionKey;
import com.google.gson.Gson;
import com.microsoft.azure.documentdb.DocumentClientException;
import com.microsoft.azure.documentdb.bulkexecutor.BulkImportResponse;
import com.microsoft.azure.documentdb.bulkexecutor.DocumentBulkExecutor;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.azure.logging.DependencyLogger;
import org.opengroup.osdu.azure.logging.DependencyLoggingOptions;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static org.opengroup.osdu.azure.logging.DependencyType.COSMOS_STORE;

/**
 * Class to perform bulk Cosmos operations using DocumentBulkExecutor or CosmosClient.
 */
@Component
@Lazy
public class CosmosStoreBulkOperations {

    private static final Logger LOGGER = LoggerFactory.getLogger(CosmosStoreBulkOperations.class.getName());

    @Autowired
    private DependencyLogger dependencyLogger;

    @Autowired
    private ICosmosBulkExecutorFactory bulkExecutorFactory;

    @Autowired
    private ICosmosClientFactory cosmosClientFactory;

    /**
     * Bulk upserts item into cosmos collection using DocumentBulkExecutor.
     *
     * @param dataPartitionId                 name of data partition.
     * @param cosmosDBName                    name of Cosmos db.
     * @param collectionName                  name of collection in Cosmos.
     * @param documents                       collection of JSON serializable documents.
     * @param isUpsert                        flag denoting if the isUpsert flag should be set to true.
     * @param disableAutomaticIdGeneration    flag denoting if automatic id generation should be disabled in Cosmos.
     * @param maxConcurrencyPerPartitionRange The maximum degree of concurrency per partition key range. The default value is 20.
     * @param <T>                             Type of object being bulk inserted.
     * @return BulkImportResponse object with the results of the operation.
     */
    public final <T> BulkImportResponse bulkInsert(final String dataPartitionId,
                                                   final String cosmosDBName,
                                                   final String collectionName,
                                                   final Collection<T> documents,
                                                   final boolean isUpsert,
                                                   final boolean disableAutomaticIdGeneration,
                                                   final int maxConcurrencyPerPartitionRange) {
        Collection<String> serializedDocuments = new ArrayList<>();
        Gson gson = new Gson();
        final long start = System.currentTimeMillis();
        int statusCode = HttpStatus.SC_OK;
        double requestCharge = 0.0;

        // Serialize documents to json strings
        for (T item : documents) {
            String serializedDocument = gson.toJson(item);
            serializedDocuments.add(serializedDocument);
        }

        try {
            DocumentBulkExecutor executor = bulkExecutorFactory.getClient(dataPartitionId, cosmosDBName, collectionName);
            BulkImportResponse response = executor.importAll(serializedDocuments, isUpsert, disableAutomaticIdGeneration, maxConcurrencyPerPartitionRange);
            requestCharge = response.getTotalRequestUnitsConsumed();

            if (response.getNumberOfDocumentsImported() != documents.size()) {
                LOGGER.warn("Failed to import all documents using DocumentBulkExecutor! Attempted to import " + documents.size() + " documents but only imported " + response.getNumberOfDocumentsImported());
            }
            return response;
        } catch (DocumentClientException e) {
            statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            String errorMessage = "Unexpectedly failed to bulk insert documents";
            LOGGER.warn(errorMessage, e);
            throw new AppException(statusCode, errorMessage, e.getMessage(), e);
        } finally {
            final long timeTaken = System.currentTimeMillis() - start;
            final String dependencyTarget = DependencyLogger.getCosmosDependencyTarget(cosmosDBName, collectionName);
            final String dependencyData = String.format("collectionName=%s", collectionName);
            final DependencyLoggingOptions loggingOptions = DependencyLoggingOptions.builder()
                    .type(COSMOS_STORE)
                    .name("UPSERT_ITEMS")
                    .data(dependencyData)
                    .target(dependencyTarget)
                    .timeTakenInMs(timeTaken)
                    .requestCharge(requestCharge)
                    .resultCode(statusCode)
                    .success(statusCode == HttpStatus.SC_OK)
                    .build();
            dependencyLogger.logDependency(loggingOptions);
        }
    }

    /**
     * Bulk upserts item into cosmos collection using CosmosClient.
     * Partition Keys must be provided in the same order as records.
     * ith Record's partition Key will be at ith position in the List.
     *
     * @param dataPartitionId                 name of data partition.
     * @param cosmosDBName                    name of Cosmos db.
     * @param collectionName                  name of collection in Cosmos.
     * @param docs                            collection of JSON serializable documents.
     * @param partitionKeys                   List of partition keys corresponding to "docs" provided
     * @param maxConcurrencyPerPartitionRange concurrency per partition (1-5)
     * @param <T>                             Type of object being bulk inserted.
     */
    public final <T> void bulkInsertWithCosmosClient(final String dataPartitionId,
                                                     final String cosmosDBName,
                                                     final String collectionName,
                                                     final List<T> docs,
                                                     final List<String> partitionKeys,
                                                     final int maxConcurrencyPerPartitionRange) {
        final long start = System.currentTimeMillis();
        final int[] statusCode = {HttpStatus.SC_OK};
        final int[] clientStatusCode = {HttpStatus.SC_OK};
        final double[] requestCharge = {0.0};
        try {
            List<String> exceptions = new ArrayList<>();

            CosmosClient cosmosClient = cosmosClientFactory.getClient(dataPartitionId);
            CosmosContainer container = cosmosClient.getDatabase(cosmosDBName).getContainer(collectionName);

            List<CosmosItemOperation> cosmosItemOperations = new ArrayList<>();
            for (int i = 0; i < docs.size(); i++) {
                cosmosItemOperations.add(CosmosBulkOperations.getUpsertItemOperation(docs.get(i), new PartitionKey(partitionKeys.get(i))));
            }

            CosmosBulkExecutionOptions cosmosBulkExecutionOptions = new CosmosBulkExecutionOptions();
            cosmosBulkExecutionOptions.setMaxMicroBatchConcurrency(maxConcurrencyPerPartitionRange);

            container.executeBulkOperations(cosmosItemOperations, cosmosBulkExecutionOptions).forEach(cosmosBulkOperationResponse -> {
                CosmosBulkItemResponse cosmosBulkItemResponse = cosmosBulkOperationResponse.getResponse();
                CosmosItemOperation cosmosItemOperation = cosmosBulkOperationResponse.getOperation();
                requestCharge[0] += cosmosBulkItemResponse.getRequestCharge();

                if (cosmosBulkItemResponse != null && cosmosBulkOperationResponse.getResponse() != null && cosmosBulkOperationResponse.getResponse().isSuccessStatusCode()) {
                    LOGGER.info("Item : [{}], Status Code: {}, Request Charge: {}", cosmosItemOperation.getItem().toString(), cosmosBulkItemResponse.getStatusCode(), cosmosBulkItemResponse.getRequestCharge());
                } else {
                    LOGGER.error(
                            "The operation for Item : [{}] Failed. Response code : {}. , Request Charge: {}, Exception : {}",
                            cosmosItemOperation.getItem().toString(),
                            cosmosBulkItemResponse.getStatusCode(),
                            cosmosBulkItemResponse.getRequestCharge(),
                            cosmosBulkOperationResponse.getException());
                    if (cosmosBulkOperationResponse.getException() != null) {
                        exceptions.add(cosmosBulkOperationResponse.getException().toString());
                    } else {
                        exceptions.add("Error occurred while upsert operation");
                    }
                    if (cosmosBulkItemResponse.getStatusCode() >= 500) {
                        statusCode[0] = cosmosBulkItemResponse.getStatusCode();
                    } else if (cosmosBulkItemResponse.getStatusCode() >= 400) {
                        clientStatusCode[0] = cosmosBulkItemResponse.getStatusCode();
                    }
                }
            });

            if (!exceptions.isEmpty()) {
                int status = statusCode[0] != HttpStatus.SC_OK ? statusCode[0] : clientStatusCode[0];
                LOGGER.error("Failed to create documents in CosmosDB: {}", String.join(",", exceptions));
                throw new AppException(status, "Record creation has failed!", "Failed to create documents in CosmosDB", exceptions.toArray(new String[exceptions.size()]));
            }
        } catch (Exception e) {
            int status = statusCode[0] != HttpStatus.SC_OK ? statusCode[0] : clientStatusCode[0];
            String errorMessage = "Unexpectedly failed to bulk insert documents";
            LOGGER.error(errorMessage, e);
            throw new AppException(status, errorMessage, e.getMessage(), e);
        } finally {
            int status = statusCode[0] != HttpStatus.SC_OK ? statusCode[0] : clientStatusCode[0];
            final long timeTaken = System.currentTimeMillis() - start;
            final String dependencyTarget = DependencyLogger.getCosmosDependencyTarget(cosmosDBName, collectionName);
            final String dependencyData = String.format("partition_key=%s", new HashSet<>(partitionKeys));
            final DependencyLoggingOptions loggingOptions = DependencyLoggingOptions.builder()
                    .type(COSMOS_STORE)
                    .name("UPSERT_ITEMS")
                    .data(dependencyData)
                    .target(dependencyTarget)
                    .timeTakenInMs(timeTaken)
                    .requestCharge(requestCharge[0])
                    .resultCode(statusCode[0])
                    .success(status == HttpStatus.SC_OK)
                    .build();
            dependencyLogger.logDependency(loggingOptions);
        }
    }
}
