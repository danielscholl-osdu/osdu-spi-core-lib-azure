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

package org.opengroup.osdu.azure.cosmosdb;

import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.implementation.ConflictException;
import com.azure.cosmos.implementation.NotFoundException;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.FeedResponse;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedFlux;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.google.common.base.Strings;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.azure.logging.DependencyLogger;
import org.opengroup.osdu.azure.logging.DependencyLoggingOptions;
import org.opengroup.osdu.azure.query.CosmosStorePageRequest;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static org.opengroup.osdu.azure.cosmosdb.CosmosStoreBulkOperations.REQUEST_TOO_LONG_ERROR_MESSAGE;
import static org.opengroup.osdu.azure.cosmosdb.CosmosStoreBulkOperations.REQUEST_TOO_LONG_ERROR_REASON;
import static org.opengroup.osdu.azure.cosmosdb.CosmosStoreBulkOperations.TOO_MANY_REQUEST_ERROR_MESSAGE;
import static org.opengroup.osdu.azure.cosmosdb.CosmosStoreBulkOperations.TOO_MANY_REQUEST_ERROR_REASON;
import static org.opengroup.osdu.azure.logging.DependencyType.COSMOS_STORE;

/**
 * A simpler interface for interacting with CosmosDB.
 * Usage Examples:
 * <pre>
 * {@code
 *      @Inject
 *      private CosmosContainer container;
 *
 *      @Inject
 *      private CosmosStore cosmosStore;
 *
 *      void findItemExample() {
 *          Optional<MyObject> myItem = cosmosStore.findItem("dataPartitionId", "cosmosDb", "collection", "id", "partition-key", MyObject.class);
 *          myItem.isPresent(); // true if found, false otherwise
 *      }
 *
 *      void findAllItemsExample() {
 *          List<MyObject> objects = cosmosStore.findAllItems("dataPartitionId", "cosmosDb", "collection", MyObject.class);
 *      }
 *
 *      void queryItemsExample() {
 *          SqlQuerySpec query = new SqlQuerySpec()
 *                 .setQueryText("SELECT * FROM c WHERE c.isFoo = @isFoo")
 *                 .setParameters(new SqlParameterList(new SqlParameter("@isFoo", true)));
 *         FeedOptions options = new FeedOptions().setEnableCrossPartitionQuery(true);
 *
 *         List<MyObject> objects = cosmosStore.queryItems("dataPartitionId", "cosmosDb", "collection", query, options, MyObject.class);
 *      }
 *
 *      void createItemExample() {
 *          cosmosStore.createItem("dataPartitionId", "cosmosDb", "collection", "some-data");
 *      }
 * }
 * </pre>
 */

@Component
@Lazy
public class CosmosStore {

    private static final String LOGGER_NAME = CosmosStore.class.getName();
    private static final int PREFERRED_PAGE_SIZE = 1000;
    protected static final String REQUEST_TIMEOUT_ERROR_REASON = "Request Timeout";
    protected static final String REQUEST_TIMEOUT_ERROR_MESSAGE = "Request timeout while using CosmosDB. Consider increasing timeout settings or checking network connectivity.";

    @Autowired
    private ICosmosClientFactory cosmosClientFactory;
    @Autowired
    private DependencyLogger dependencyLogger;

    /**
     * @param dataPartitionId Data partition id
     * @param cosmosDBName    Database name
     * @param collection      Collection name
     * @param id              ID of item
     * @param partitionKey    Partition key of item
     * @param clazz           Class to serialize results into
     * @param <T>             Type to return
     * @return The item
     */
    public <T> Optional<T> findItem(
            final String dataPartitionId,
            final String cosmosDBName,
            final String collection,
            final String id,
            final String partitionKey,
            final Class<T> clazz) {
        CosmosContainer container = getCosmosContainer(dataPartitionId, cosmosDBName, collection);
        return findItemInternal(cosmosDBName, collection, container, id, partitionKey, clazz);
    }

    /**
     * @param cosmosDBName Database name
     * @param collection   Collection name
     * @param id           ID of item
     * @param partitionKey Partition key of item
     * @param clazz        Class to serialize results into
     * @param <T>          Type to return
     * @return The item
     */
    public <T> Optional<T> findItem(
            final String cosmosDBName,
            final String collection,
            final String id,
            final String partitionKey,
            final Class<T> clazz) {
        CosmosContainer container = getSystemCosmosContainer(cosmosDBName, collection);
        return findItemInternal(cosmosDBName, collection, container, id, partitionKey, clazz);
    }

    /**
     * @param dataPartitionId Data partition id
     * @param cosmosDBName    Database name
     * @param collection      Collection name
     * @param id              ID of item
     * @param partitionKey    Partition key of item
     * @param <T>             Type of item
     */
    public <T> void deleteItem(
            final String dataPartitionId,
            final String cosmosDBName,
            final String collection,
            final String id,
            final String partitionKey) {
        CosmosContainer container = getCosmosContainer(dataPartitionId, cosmosDBName, collection);
        deleteItemInternal(cosmosDBName, collection, container, id, partitionKey);
    }

    /**
     * @param cosmosDBName Database name
     * @param collection   Collection name
     * @param id           ID of item
     * @param partitionKey Partition key of item
     * @param <T>          Type of item
     */
    public <T> void deleteItem(
            final String cosmosDBName,
            final String collection,
            final String id,
            final String partitionKey) {
        CosmosContainer container = getSystemCosmosContainer(cosmosDBName, collection);
        deleteItemInternal(cosmosDBName, collection, container, id, partitionKey);
    }

    /**
     * @param dataPartitionId Data partition id
     * @param cosmosDBName    Database name
     * @param collection      Collection name
     * @param partitionKey    Partition key of item
     * @param item            Data object to store
     * @param <T>             Type of item
     */
    public <T> void upsertItem(
            final String dataPartitionId,
            final String cosmosDBName,
            final String collection,
            final String partitionKey,
            final T item) {
        CosmosContainer cosmosContainer = getCosmosContainer(dataPartitionId, cosmosDBName, collection);
        upsertItemInternal(cosmosDBName, collection, cosmosContainer, partitionKey, item);
    }

    /**
     * @param cosmosDBName Database name
     * @param collection   Collection name
     * @param partitionKey Partition key of item
     * @param item         Data object to store
     * @param <T>          Type of item
     */
    public <T> void upsertItem(
            final String cosmosDBName,
            final String collection,
            final String partitionKey,
            final T item) {
        CosmosContainer cosmosContainer = getSystemCosmosContainer(cosmosDBName, collection);
        upsertItemInternal(cosmosDBName, collection, cosmosContainer, partitionKey, item);
    }

    /**
     * @param dataPartitionId Data partition id
     * @param cosmosDBName    Database name
     * @param collection      Collection name
     * @param id              ID of item
     * @param partitionKey    Partition key of item
     * @param item            Data object to store
     * @param <T>             Type of item
     */
    public <T> void replaceItem(
            final String dataPartitionId,
            final String cosmosDBName,
            final String collection,
            final String id,
            final String partitionKey,
            final T item) {
        final long start = System.currentTimeMillis();
        int statusCode = HttpStatus.SC_OK;
        double requestCharge = 0.0;
        try {
            CosmosContainer cosmosContainer = getCosmosContainer(dataPartitionId, cosmosDBName, collection);
            PartitionKey key = new PartitionKey(partitionKey);
            CosmosItemRequestOptions options = new CosmosItemRequestOptions();
            CosmosItemResponse<T> response = cosmosContainer.replaceItem(item, id, key, options);
            requestCharge = response.getRequestCharge();
            CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug(String.format("REPLACE_ITEM with id=%s and partition_key=%s", id, partitionKey));
        } catch (NotFoundException e) {
            statusCode = e.getStatusCode();
            String errorMessage = "Item was unexpectedly not found";
            CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).warn(errorMessage, e);
            throw new AppException(404, errorMessage, e.getMessage(), e);
        } catch (CosmosException e) {
            statusCode = e.getStatusCode();
            throw handleCosmosException(e, "Unexpectedly failed to replace item into CosmosDB");
        } finally {
            final long timeTaken = System.currentTimeMillis() - start;
            final String dependencyTarget = getDependencyTarget(dataPartitionId, cosmosDBName, collection);
            final String dependencyData = String.format("id=%s partition_key=%s", id, partitionKey);
            final DependencyLoggingOptions options = DependencyLoggingOptions.builder()
                    .type(COSMOS_STORE)
                    .name("REPLACE_ITEM")
                    .data(dependencyData)
                    .target(dependencyTarget)
                    .timeTakenInMs(timeTaken)
                    .requestCharge(requestCharge)
                    .resultCode(statusCode)
                    .success(statusCode == HttpStatus.SC_OK)
                    .build();
            dependencyLogger.logDependency(options);
        }
    }

    /**
     * @param dataPartitionId Data partition id
     * @param cosmosDBName    Database name
     * @param collection      Collection name
     * @param partitionKey    Partition key of item
     * @param item            Data object to store
     * @param <T>             Type of item
     */
    public <T> void createItem(
            final String dataPartitionId,
            final String cosmosDBName,
            final String collection,
            final String partitionKey,
            final T item) {
        CosmosContainer cosmosContainer = getCosmosContainer(dataPartitionId, cosmosDBName, collection);
        createItemInternal(cosmosDBName, collection, cosmosContainer, partitionKey, item);
    }

    /**
     * @param cosmosDBName Database name
     * @param collection   Collection name
     * @param partitionKey Partition key of item
     * @param item         Data object to store
     * @param <T>          Type of item
     */
    public <T> void createItem(
            final String cosmosDBName,
            final String collection,
            final String partitionKey,
            final T item) {
        CosmosContainer cosmosContainer = getSystemCosmosContainer(cosmosDBName, collection);
        createItemInternal(cosmosDBName, collection, cosmosContainer, partitionKey, item);
    }

    // Find All and Queries

    /**
     * @param dataPartitionId dataPartitionId
     * @param cosmosDBName    Database name
     * @param collection      Collection name
     * @param clazz           Class type of response
     * @param <T>             Type
     * @return List<T> List of items found
     */
    public <T> List<T> findAllItems(
            final String dataPartitionId,
            final String cosmosDBName,
            final String collection,
            final Class<T> clazz) {
        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
        return queryItems(dataPartitionId, cosmosDBName, collection, new SqlQuerySpec("SELECT * FROM c"), options, clazz);
    }

    /**
     * @param dataPartitionId Data partition id
     * @param cosmosDBName    Database name
     * @param collection      Collection name
     * @param query           {@link SqlQuerySpec} to execute
     * @param options         Options
     * @param clazz           Class type of response
     * @param <T>             Type
     * @return List<T> List of items found on specific page in container
     */
    public <T> List<T> queryItems(
            final String dataPartitionId,
            final String cosmosDBName,
            final String collection,
            final SqlQuerySpec query,
            final CosmosQueryRequestOptions options,
            final Class<T> clazz) {
        CosmosContainer cosmosContainer = getCosmosContainer(dataPartitionId, cosmosDBName, collection);
        return queryItemsInternal(cosmosDBName, collection, cosmosContainer, query, options, clazz);
    }

    /**
     * @param cosmosDBName Database name
     * @param collection   Collection name
     * @param query        {@link SqlQuerySpec} to execute
     * @param options      Options
     * @param clazz        Class type of response
     * @param <T>          Type
     * @return List<T> List of items found on specific page in container
     */
    public <T> List<T> queryItems(
            final String cosmosDBName,
            final String collection,
            final SqlQuerySpec query,
            final CosmosQueryRequestOptions options,
            final Class<T> clazz) {
        CosmosContainer cosmosContainer = getSystemCosmosContainer(cosmosDBName, collection);
        return queryItemsInternal(cosmosDBName, collection, cosmosContainer, query, options, clazz);
    }

    /**
     * @param dataPartitionId   Data partition id
     * @param cosmosDBName      Database
     * @param collection        Collection
     * @param clazz             Class type of response
     * @param pageSize          Page size
     * @param continuationToken Continuation token
     * @param <T>               Type of items
     * @return Page<T> Page of items
     */
    public <T> Page<T> findAllItemsPage(
            final String dataPartitionId,
            final String cosmosDBName,
            final String collection,
            final Class<T> clazz,
            final int pageSize,
            final String continuationToken) {
        return queryItemsPage(dataPartitionId, cosmosDBName, collection, new SqlQuerySpec("SELECT * FROM c"), clazz, pageSize, continuationToken);
    }

    /**
     * @param dataPartitionId   Data partition id
     * @param cosmosDBName      Database name
     * @param collection        Collection name
     * @param query             {@link SqlQuerySpec} to execute
     * @param clazz             Class type
     * @param pageSize          Page size
     * @param continuationToken Continuation token
     * @param <T>               Type
     * @return Page<T> Page of itemns found
     */
    public <T> Page<T> queryItemsPage(
            final String dataPartitionId,
            final String cosmosDBName,
            final String collection,
            final SqlQuerySpec query,
            final Class<T> clazz,
            final int pageSize,
            final String continuationToken) {
        return queryItemsPage(dataPartitionId, cosmosDBName, collection, query, clazz, pageSize, continuationToken, new CosmosQueryRequestOptions());
    }

    /**
     * @param dataPartitionId   Data partition id
     * @param cosmosDBName      Database name
     * @param collection        Collection name
     * @param query             {@link SqlQuerySpec} to execute
     * @param clazz             Class type
     * @param pageSize          Page size
     * @param continuationToken Continuation token
     * @param queryOptions      Query options
     * @param <T>               Type
     * @return Page<T> Page of itemns found
     */
    public <T> Page<T> queryItemsPage(
            final String dataPartitionId,
            final String cosmosDBName,
            final String collection,
            final SqlQuerySpec query,
            final Class<T> clazz,
            final int pageSize,
            final String continuationToken,
            final CosmosQueryRequestOptions queryOptions) {

        int currentPageNumber = 1;
        int iterationNumber = 1;
        int documentNumber = 0;
        double requestCharge = 0.0;

        String internalcontinuationToken = continuationToken;
        List<T> results = new ArrayList<>();
        CosmosContainer container = getCosmosContainer(dataPartitionId, cosmosDBName, collection);

        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug("Receiving a set of query response pages.");
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug("Continuation Token: " + internalcontinuationToken + "\n");

        final long start = System.currentTimeMillis();
        Iterable<FeedResponse<T>> feedResponseIterator =
                container.queryItems(query, queryOptions, clazz).iterableByPage(internalcontinuationToken, pageSize);

        Iterator<FeedResponse<T>> iterator = feedResponseIterator.iterator();
        if (iterator.hasNext()) {
            FeedResponse<T> page = feedResponseIterator.iterator().next();
            requestCharge = page.getRequestCharge();
            CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug(String.format("Current page number: %d", currentPageNumber));
            // Access all of the documents in this result page
            for (T item : page.getResults()) {
                documentNumber++;
                results.add(item);
            }

            // Page count so far
            CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug(String.format("Total documents received so far: %d", documentNumber));

            // Along with page results, get a continuation token
            // which enables the client to "pick up where it left off"
            // in accessing query response pages.
            internalcontinuationToken = page.getContinuationToken();
            currentPageNumber++;
            iterationNumber++;
        }

        final long timeTaken = System.currentTimeMillis() - start;
        final String dependencyTarget = getDependencyTarget(dataPartitionId, cosmosDBName, collection);
        final String dependencyData = String.format("query=%s", query.getQueryText());
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug("Done. Retrieved {} results", results.size());
        final DependencyLoggingOptions options = DependencyLoggingOptions.builder()
                .type(COSMOS_STORE)
                .name("QUERY_ITEMS_PAGE")
                .data(dependencyData)
                .target(dependencyTarget)
                .timeTakenInMs(timeTaken)
                .requestCharge(requestCharge)
                .resultCode(HttpStatus.SC_OK)
                .success(true)
                .build();
        dependencyLogger.logDependency(options);

        CosmosStorePageRequest pageRequest = CosmosStorePageRequest.of(currentPageNumber, pageSize, internalcontinuationToken);
        return new PageImpl(results, pageRequest, documentNumber);
    }

    /**
     * @param dataPartitionId   Data partition id
     * @param cosmosDBName      Database name
     * @param collection        Collection name
     * @param query             {@link SqlQuerySpec} to execute
     * @param clazz             Class type
     * @param pageSize          Page size
     * @param continuationToken Continuation token
     * @param <T>               Type
     * @return Page<T> Page of itemns found
     */
    public <T> Page<T> queryItemsPageAsync(
            final String dataPartitionId,
            final String cosmosDBName,
            final String collection,
            final SqlQuerySpec query,
            final Class<T> clazz,
            final int pageSize,
            final String continuationToken) {

        int currentPageNumber = 1;
        int documentNumber = 0;
        double requestCharge = 0.0;

        String internalContinuationToken = continuationToken;
        CosmosAsyncContainer cosmosAsyncContainer = cosmosClientFactory.getAsyncClient(dataPartitionId).getDatabase(cosmosDBName).getContainer(collection);

        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug("Receiving a set of query response pages.");
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug("Continuation Token: " + internalContinuationToken + "\n");

        CosmosQueryRequestOptions queryOptions = new CosmosQueryRequestOptions();
        queryOptions.setMaxBufferedItemCount(pageSize);

        final long start = System.currentTimeMillis();

        CosmosPagedFlux<T> cosmosPagedFlux = cosmosAsyncContainer.queryItems(query, queryOptions, clazz);
        FeedResponse<T> page = cosmosPagedFlux.byPage(pageSize).blockFirst();
        List<T> results = new ArrayList<>();

        if (page != null) {
            results.addAll(page.getResults());
            internalContinuationToken = page.getContinuationToken();
            requestCharge = page.getRequestCharge();
        }

        final long timeTaken = System.currentTimeMillis() - start;
        final String dependencyTarget = getDependencyTarget(dataPartitionId, cosmosDBName, collection);
        final String dependencyData = String.format("query=%s", query.getQueryText());
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug("Done. Retrieved {} results", results.size());
        final DependencyLoggingOptions options = DependencyLoggingOptions.builder()
                .type(COSMOS_STORE)
                .name("QUERY_ITEMS_PAGE_ASYNC")
                .data(dependencyData)
                .target(dependencyTarget)
                .timeTakenInMs(timeTaken)
                .requestCharge(requestCharge)
                .resultCode(HttpStatus.SC_OK)
                .success(true)
                .build();
        dependencyLogger.logDependency(options);

        CosmosStorePageRequest pageRequest = CosmosStorePageRequest.of(currentPageNumber, pageSize, internalContinuationToken);
        return new PageImpl<>(results, pageRequest, documentNumber);
    }

    /**
     * @param dataPartitionId   Data partition id
     * @param cosmosDBName      Database name
     * @param collection        Collection name
     * @param query             {@link SqlQuerySpec} to execute
     * @param partitionKey      Partition key of item
     * @param clazz             Class type
     * @param pageSize          Page size
     * @param continuationToken Continuation token
     * @param <T>               Type
     * @return Page<T> Page of items found
     */
    public <T> Page<T> queryItemsPage(
            final String dataPartitionId,
            final String cosmosDBName,
            final String collection,
            final SqlQuerySpec query,
            final String partitionKey,
            final Class<T> clazz,
            final int pageSize,
            final String continuationToken) {

        int currentPageNumber = 1;
        int currentPageSize = pageSize;
        int documentNumber = 0;
        double requestCharge = 0.0;

        String internalContinuationToken = continuationToken;
        List<T> results = new ArrayList<>();
        CosmosContainer container = getCosmosContainer(dataPartitionId, cosmosDBName, collection);

        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug("Receiving a set of query response pages.");
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug("Continuation Token: " + internalContinuationToken + "\n");

        CosmosQueryRequestOptions queryOptions = new CosmosQueryRequestOptions();
        queryOptions.setPartitionKey(new PartitionKey(partitionKey));
        final long start = System.currentTimeMillis();

        do {
            Iterable<FeedResponse<T>> feedResponseIterator =
                    container.queryItems(query, queryOptions, clazz).iterableByPage(internalContinuationToken, currentPageSize);

            for (FeedResponse<T> page : feedResponseIterator) {
                requestCharge += page.getRequestCharge();
                CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug(String.format("Current page number: %d", currentPageNumber));
                // Access all the documents in this result page
                for (T item : page.getResults()) {
                    documentNumber++;
                    results.add(item);
                }

                // Page count so far
                CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug(String.format("Total documents received so far: %d", documentNumber));

                // Along with page results, get a continuation token
                // which enables the client to "pick up where it left off"
                // in accessing query response pages.
                internalContinuationToken = page.getContinuationToken();
                currentPageNumber++;
                break;
            }
            currentPageSize = currentPageSize - results.size();
        } while (!Strings.isNullOrEmpty(internalContinuationToken) && currentPageSize > 0);

        final long timeTaken = System.currentTimeMillis() - start;
        final String dependencyTarget = getDependencyTarget(dataPartitionId, cosmosDBName, collection);
        final String dependencyData = String.format("query=%s", query.getQueryText());
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug("Done. Retrieved {} results", results.size());
        final DependencyLoggingOptions options = DependencyLoggingOptions.builder()
                .type(COSMOS_STORE)
                .name("QUERY_ITEMS_PAGE")
                .data(dependencyData)
                .target(dependencyTarget)
                .timeTakenInMs(timeTaken)
                .requestCharge(requestCharge)
                .resultCode(HttpStatus.SC_OK)
                .success(true)
                .build();
        dependencyLogger.logDependency(options);

        CosmosStorePageRequest pageRequest = CosmosStorePageRequest.of(currentPageNumber, pageSize, internalContinuationToken);
        return new PageImpl(results, pageRequest, documentNumber);
    }

    /**
     * @param cosmosDBName Database name
     * @param collection   Collection name
     * @param container    Cosmos container
     * @param id           ID of item
     * @param partitionKey Partition key of item
     * @param clazz        Class to serialize results into
     * @param <T>          Type to return
     * @return The item
     */
    private <T> Optional<T> findItemInternal(
            final String cosmosDBName,
            final String collection,
            final CosmosContainer container,
            final String id,
            final String partitionKey,
            final Class<T> clazz) {
        final long start = System.currentTimeMillis();
        double requestCharge = 0.0;
        int statusCode = HttpStatus.SC_OK;
        try {
            CosmosItemRequestOptions options = new CosmosItemRequestOptions();
            PartitionKey key = new PartitionKey(partitionKey);
            CosmosItemResponse<T> cosmosItemResponse = container.readItem(id, key, options, clazz);
            requestCharge = cosmosItemResponse.getRequestCharge();
            T item = cosmosItemResponse.getItem();
            CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug(String.format("READ_ITEM with id=%s and partition_key=%s", id, partitionKey));
            return Optional.ofNullable((T) item);
        } catch (NotFoundException e) {
            statusCode = HttpStatus.SC_NOT_FOUND;
            CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).warn(String.format("Unable to find item with id=%s and partition_key=%s", id, partitionKey), e);
            return Optional.empty();
        } catch (CosmosException e) {
            statusCode = e.getStatusCode();
            throw handleCosmosException(e, "Unexpectedly failed to read item from CosmosDB");
        } finally {
            final long timeTaken = System.currentTimeMillis() - start;
            final String dependencyTarget = DependencyLogger.getCosmosDependencyTarget(cosmosDBName, collection);
            final String dependencyData = String.format("id=%s partition_key=%s", id, partitionKey);
            final DependencyLoggingOptions options = DependencyLoggingOptions.builder()
                    .type(COSMOS_STORE)
                    .name("READ_ITEM")
                    .data(dependencyData)
                    .target(dependencyTarget)
                    .timeTakenInMs(timeTaken)
                    .requestCharge(requestCharge)
                    .resultCode(statusCode)
                    .success(statusCode == HttpStatus.SC_OK)
                    .build();
            dependencyLogger.logDependency(options);
        }
    }

    /**
     * @param cosmosDBName Database name
     * @param collection   Collection name
     * @param container    Cosmos container
     * @param id           ID of item
     * @param partitionKey Partition key of item
     * @param <T>          Type of item
     */
    private <T> void deleteItemInternal(
            final String cosmosDBName,
            final String collection,
            final CosmosContainer container,
            final String id,
            final String partitionKey) {
        final long start = System.currentTimeMillis();
        int statusCode = HttpStatus.SC_OK;
        double requestCharge = 0.0;
        try {
            PartitionKey key = new PartitionKey(partitionKey);
            CosmosItemRequestOptions options = new CosmosItemRequestOptions();
            CosmosItemResponse<Object> response = container.deleteItem(id, key, options);
            requestCharge = response.getRequestCharge();
            CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug(String.format("DELETE_ITEM with id=%s and partition_key=%s", id, partitionKey));
        } catch (NotFoundException e) {
            statusCode = HttpStatus.SC_NOT_FOUND;
            String errorMessage = "Item was unexpectedly not found";
            CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).warn(errorMessage, e);
            throw new AppException(404, errorMessage, e.getMessage(), e);
        } catch (CosmosException e) {
            statusCode = e.getStatusCode();
            throw handleCosmosException(e, "Unexpectedly failed to delete item from CosmosDB");
        } finally {
            final long timeTaken = System.currentTimeMillis() - start;
            final String dependencyTarget = DependencyLogger.getCosmosDependencyTarget(cosmosDBName, collection);
            final String dependencyData = String.format("id=%s partition_key=%s", id, partitionKey);
            final DependencyLoggingOptions options = DependencyLoggingOptions.builder()
                    .type(COSMOS_STORE)
                    .name("DELETE_ITEM")
                    .data(dependencyData)
                    .target(dependencyTarget)
                    .timeTakenInMs(timeTaken)
                    .requestCharge(requestCharge)
                    .resultCode(statusCode)
                    .success(statusCode == HttpStatus.SC_OK)
                    .build();
            dependencyLogger.logDependency(options);
        }
    }

    /**
     * @param cosmosDBName Database name
     * @param collection   Collection name
     * @param container    Cosmos container.
     * @param partitionKey Partition key of item
     * @param item         Data object to store
     * @param <T>          Type of item
     */
    private <T> void upsertItemInternal(
            final String cosmosDBName,
            final String collection,
            final CosmosContainer container,
            final String partitionKey,
            final T item) {
        final long start = System.currentTimeMillis();
        int statusCode = HttpStatus.SC_OK;
        double requestCharge = 0.0;
        try {
            PartitionKey key = new PartitionKey(partitionKey);
            CosmosItemRequestOptions options = new CosmosItemRequestOptions();
            CosmosItemResponse<T> response = container.upsertItem(item, key, options);
            requestCharge = response.getRequestCharge();
            CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug(String.format("UPSERT_ITEM with partition_key=%s", partitionKey));
        } catch (CosmosException e) {
            statusCode = e.getStatusCode();
            throw handleCosmosException(e, "Unexpectedly failed to put item into CosmosDB");
        } finally {
            final long timeTaken = System.currentTimeMillis() - start;
            final String dependencyTarget = DependencyLogger.getCosmosDependencyTarget(cosmosDBName, collection);
            final String dependencyData = String.format("partition_key=%s", partitionKey);
            final DependencyLoggingOptions options = DependencyLoggingOptions.builder()
                    .type(COSMOS_STORE)
                    .name("UPSERT_ITEM")
                    .data(dependencyData)
                    .target(dependencyTarget)
                    .timeTakenInMs(timeTaken)
                    .requestCharge(requestCharge)
                    .resultCode(statusCode)
                    .success(statusCode == HttpStatus.SC_OK)
                    .build();
            dependencyLogger.logDependency(options);
        }
    }

    /**
     * @param cosmosDBName Database name
     * @param collection   Collection name
     * @param container    Cosmos container
     * @param partitionKey Partition key of item
     * @param item         Data object to store
     * @param <T>          Type of item
     */
    private <T> void createItemInternal(
            final String cosmosDBName,
            final String collection,
            final CosmosContainer container,
            final String partitionKey,
            final T item) {
        final long start = System.currentTimeMillis();
        int statusCode = HttpStatus.SC_OK;
        double requestCharge = 0.0;
        try {
            PartitionKey key = new PartitionKey(partitionKey);
            CosmosItemRequestOptions options = new CosmosItemRequestOptions();
            CosmosItemResponse<T> response = container.createItem(item, key, options);
            requestCharge = response.getRequestCharge();
            CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug(String.format("CREATE_ITEM with partition_key=%s", partitionKey));
        } catch (ConflictException e) {
            statusCode = e.getStatusCode();
            String errorMessage = "Resource with specified id or name already exists.";
            CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).warn(errorMessage, e);
            throw new AppException(409, errorMessage, e.getMessage(), e);
        } catch (CosmosException e) {
            statusCode = e.getStatusCode();
            throw handleCosmosException(e, "Unexpectedly failed to insert item into CosmosDB");
        } finally {
            final long timeTaken = System.currentTimeMillis() - start;
            final String dependencyTarget = DependencyLogger.getCosmosDependencyTarget(cosmosDBName, collection);
            final String dependencyData = String.format("partition_key=%s", partitionKey);
            final DependencyLoggingOptions options = DependencyLoggingOptions.builder()
                    .type(COSMOS_STORE)
                    .name("CREATE_ITEM")
                    .data(dependencyData)
                    .target(dependencyTarget)
                    .timeTakenInMs(timeTaken)
                    .requestCharge(requestCharge)
                    .resultCode(statusCode)
                    .success(statusCode == HttpStatus.SC_OK)
                    .build();
            dependencyLogger.logDependency(options);
        }
    }

    /**
     * @param cosmosDBName Database name
     * @param collection   Collection name
     * @param container    Cosmos container
     * @param query        {@link SqlQuerySpec} to execute
     * @param options      Options
     * @param clazz        Class type of response
     * @param <T>          Type
     * @return List<T> List of items found on specific page in container
     */
    private <T> List<T> queryItemsInternal(
            final String cosmosDBName,
            final String collection,
            final CosmosContainer container,
            final SqlQuerySpec query,
            final CosmosQueryRequestOptions options,
            final Class<T> clazz) {
        List<T> results = new ArrayList<>();
        final double[] requestCharge = {0.0};
        final long start = System.currentTimeMillis();
        CosmosPagedIterable<T> paginatedResponse = container.queryItems(query, options, clazz);
        paginatedResponse.iterableByPage(PREFERRED_PAGE_SIZE).forEach(cosmosItemPropertiesFeedResponse -> {
            CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug("Got a page of query result with {} items(s)",
                    cosmosItemPropertiesFeedResponse.getResults().size());
            results.addAll(cosmosItemPropertiesFeedResponse.getResults());
            requestCharge[0] += cosmosItemPropertiesFeedResponse.getRequestCharge();
        });
        final long timeTaken = System.currentTimeMillis() - start;
        final String dependencyTarget = DependencyLogger.getCosmosDependencyTarget(cosmosDBName, collection);
        final String dependencyData = String.format("query=%s", query.getQueryText());
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug("Done. Retrieved {} results", results.size());
        final DependencyLoggingOptions loggingOptions = DependencyLoggingOptions.builder()
                .type(COSMOS_STORE)
                .name("QUERY_ITEMS")
                .data(dependencyData)
                .target(dependencyTarget)
                .timeTakenInMs(timeTaken)
                .requestCharge(requestCharge[0])
                .resultCode(HttpStatus.SC_OK)
                .success(true)
                .build();
        dependencyLogger.logDependency(loggingOptions);
        return results;
    }

    /**
     * @param dataPartitionId Data partition id
     * @param cosmosDBName    Database name
     * @param collection      Collection name
     * @return Cosmos container
     */
    private CosmosContainer getCosmosContainer(
            final String dataPartitionId,
            final String cosmosDBName,
            final String collection) {
        try {
            return cosmosClientFactory.getClient(dataPartitionId)
                    .getDatabase(cosmosDBName)
                    .getContainer(collection);
        } catch (AppException ae) {
            throw this.handleCosmosStoreException(ae.getError().getCode(), "Error creating creating Cosmos Client", ae);
        } catch (Exception e) {
            throw this.handleCosmosStoreException(500, "Error creating creating Cosmos Client", e);
        }
    }

    /**
     * @param cosmosDBName Database name
     * @param collection   Collection name
     * @return Cosmos container
     */
    private CosmosContainer getSystemCosmosContainer(
            final String cosmosDBName,
            final String collection) {
        try {
            return cosmosClientFactory.getSystemClient()
                    .getDatabase(cosmosDBName)
                    .getContainer(collection);
        } catch (AppException ae) {
            throw this.handleCosmosStoreException(ae.getError().getCode(), "Error creating creating Cosmos Client", ae);
        } catch (Exception e) {
            throw this.handleCosmosStoreException(500, "Error creating creating Cosmos Client", e);
        }
    }

    /**
     * Return a string composed of partition ID, database name and collection.
     *
     * @param partitionId  the data partition ID
     * @param databaseName the Cosmos database name
     * @param collection   the Cosmos collection name
     * @return the dependency target string
     */
    private String getDependencyTarget(final String partitionId, final String databaseName, final String collection) {
        return String.format("%s:%s/%s", partitionId, databaseName, collection);
    }

    /**
     * Logs and returns instance of AppException.
     *
     * @param status       Response status code
     * @param errorMessage Error message
     * @param e            Original exception
     * @return Instance of AppException
     */
    private AppException handleCosmosStoreException(final int status, final String errorMessage, final Exception e) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).warn(errorMessage, e);
        return new AppException(status, errorMessage, e.getMessage(), e);
    }

    /**
     * Handles CosmosException by checking for specific error codes and throwing appropriate AppException.
     *
     * @param e              The CosmosException to handle
     * @param errorMessage   The error message to use for unexpected errors
     * @return Never returns, always throws AppException
     * @throws AppException Always thrown with appropriate status code
     */
    private AppException handleCosmosException(final CosmosException e, final String errorMessage) {
        int statusCode = e.getStatusCode();
        if (statusCode == HttpStatus.SC_REQUEST_TOO_LONG) {
            throw new AppException(HttpStatus.SC_REQUEST_TOO_LONG, REQUEST_TOO_LONG_ERROR_REASON, REQUEST_TOO_LONG_ERROR_MESSAGE, e);
        }
        if (statusCode == HttpStatus.SC_TOO_MANY_REQUESTS) {
            throw new AppException(HttpStatus.SC_TOO_MANY_REQUESTS, TOO_MANY_REQUEST_ERROR_REASON, TOO_MANY_REQUEST_ERROR_MESSAGE, e);
        }
        if (statusCode == HttpStatus.SC_REQUEST_TIMEOUT) {
            throw new AppException(HttpStatus.SC_REQUEST_TIMEOUT, REQUEST_TIMEOUT_ERROR_REASON, REQUEST_TIMEOUT_ERROR_MESSAGE, e);
        }
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).warn(errorMessage, e);
        throw new AppException(500, errorMessage, e.getMessage(), e);
    }
}
