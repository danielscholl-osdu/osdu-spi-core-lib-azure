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

package org.opengroup.osdu.azure.datalakestorage;

import com.azure.identity.DefaultAzureCredential;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.common.policy.RequestRetryOptions;
import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.azure.storage.file.datalake.DataLakeFileSystemClient;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.azure.storage.file.datalake.DataLakeServiceClientBuilder;
import org.opengroup.osdu.azure.di.BlobStoreRetryConfiguration;
import org.opengroup.osdu.azure.di.MSIConfiguration;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.common.Validators;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Implementation for IDataLakeClientFactory.
 */
public final class DataLakeClientFactoryImpl implements IDataLakeClientFactory {

    private Map<String, DataLakeServiceClient> dataLakeContainerClientMap;
    private Map<String, BlobServiceClient> blobServiceClientMap;

    @Autowired
    private DefaultAzureCredential defaultAzureCredential;

    @Autowired
    private PartitionServiceClient partitionService;

    @Autowired
    private BlobStoreRetryConfiguration blobStoreRetryConfiguration;

    @Autowired
    private MSIConfiguration msiConfiguration;

    /**
     * Constructor to initialize instance of {@link DataLakeClientFactoryImpl}.
     * @param credentials Azure Credentials to use
     * @param partitionServiceClient Partition service client to use
     */
    public DataLakeClientFactoryImpl(final DefaultAzureCredential credentials,
                                     final PartitionServiceClient partitionServiceClient) {
        this.defaultAzureCredential = credentials;
        this.partitionService = partitionServiceClient;
        dataLakeContainerClientMap = new ConcurrentHashMap<>();
        blobServiceClientMap = new ConcurrentHashMap<>();
    }
    /**
     * create DataLakeDirectoryClient.
     * @param dataPartitionId
     * @param directoryName
     * @param containerName
     * @return DataLakeServiceClient
     */
    @Override
    public DataLakeDirectoryClient getDataLakeDirectoryClient(
            final String dataPartitionId,
            final String directoryName,
            final String containerName) {
        DataLakeFileSystemClient fileSystemClient = getFileSystemClient(dataPartitionId, containerName);
        return fileSystemClient.getDirectoryClient(directoryName);
    }

    /**
     *
     * @param dataPartitionId dataPartitionId
     * @param fileSystemName   file System Name
     * @return DataLakeFileSystemClient
     */
    private DataLakeFileSystemClient getFileSystemClient(
            final String dataPartitionId,
            final String fileSystemName) {
        Validators.checkNotNullAndNotEmpty(dataPartitionId, "dataPartitionId");
        Validators.checkNotNullAndNotEmpty(fileSystemName, "fileSystemName");

        String cacheKey = String.format("%s-%s", dataPartitionId, fileSystemName);
        DataLakeServiceClient dataLakeServiceClient;
        if (this.dataLakeContainerClientMap.containsKey(cacheKey)) {
            dataLakeServiceClient = this.dataLakeContainerClientMap.get(cacheKey);
        } else {
            dataLakeServiceClient = getDataLakeServiceClient(dataPartitionId);
            this.dataLakeContainerClientMap.put(cacheKey, dataLakeServiceClient);
        }
        return dataLakeServiceClient.getFileSystemClient(fileSystemName);
    }

    /**
     *
     * @param dataPartitionId dataPartitionId
     * @return DataLakeServiceClient
     */
    @Override
    public DataLakeServiceClient getDataLakeServiceClient(
            final String dataPartitionId) {
        PartitionInfoAzure pi = this.partitionService.getPartition(dataPartitionId);

        String endpoint = pi.getHierarchicalStorageAccountBlobEndpoint();

        DataLakeServiceClientBuilder dataLakeServiceClientBuilder = getDataLakeServiceClientBuilder(endpoint);
        if (msiConfiguration.getIsEnabled()) {
            return dataLakeServiceClientBuilder.credential(defaultAzureCredential)
                    .buildClient();
        } else {
            StorageSharedKeyCredential storageSharedKeyCredential = new StorageSharedKeyCredential(
                    pi.getHierarchicalStorageAccountName(),
                    pi.getHierarchicalStorageAccountKey()
            );

            return dataLakeServiceClientBuilder
                    .credential(storageSharedKeyCredential)
                    .buildClient();
        }
    }

    /**
     *
     * @param dataPartitionId dataPartitionId
     * @param fileSystemName fileSystemName
     * @return DataLakeServiceClient
     */
    @Override
    public DataLakeServiceClient getDataLakeServiceClient(
            final String dataPartitionId,
            final String fileSystemName) {
        String cacheKey = String.format("%s-%s", dataPartitionId, fileSystemName);
        DataLakeServiceClient dataLakeServiceClient;
        if (this.dataLakeContainerClientMap.containsKey(cacheKey)) {
            dataLakeServiceClient = this.dataLakeContainerClientMap.get(cacheKey);
        } else {
            dataLakeServiceClient = getDataLakeServiceClient(dataPartitionId);
            this.dataLakeContainerClientMap.put(cacheKey, dataLakeServiceClient);
        }
        return dataLakeServiceClient;
    }

    /**
     * Returns a cached {@link BlobServiceClient} for the hierarchical storage account
     * associated with the given data partition. The client uses the {@code .blob.core.windows.net}
     * endpoint (converted from the DFS endpoint) so that Blob REST API operations such as
     * {@code Copy Blob} work correctly.
     *
     * @param dataPartitionId dataPartitionId
     * @return BlobServiceClient for the hierarchical storage account.
     */
    @Override
    public BlobServiceClient getBlobServiceClient(final String dataPartitionId) {
        String cacheKey = String.format("%s-HSABlobServiceClient", dataPartitionId);
        return this.blobServiceClientMap.computeIfAbsent(cacheKey, k -> createBlobServiceClient(dataPartitionId));
    }

    /**
     * Creates a new {@link BlobServiceClient} for the hierarchical storage account.
     * The DFS endpoint ({@code .dfs.core.windows.net}) is converted to the blob endpoint
     * ({@code .blob.core.windows.net}) because the Blob REST API (used by {@code beginCopy},
     * {@code copyFromUrl}, etc.) requires the blob endpoint.
     *
     * @param dataPartitionId dataPartitionId
     * @return BlobServiceClient targeting the blob endpoint of the hierarchical storage account.
     */
    private BlobServiceClient createBlobServiceClient(final String dataPartitionId) {
        PartitionInfoAzure pi = this.partitionService.getPartition(dataPartitionId);
        String endpoint = String.format("https://%s.blob.core.windows.net", pi.getHierarchicalStorageAccountName());
        RequestRetryOptions requestRetryOptions = blobStoreRetryConfiguration.getRequestRetryOptions();
        BlobServiceClientBuilder builder = new BlobServiceClientBuilder()
                .endpoint(endpoint)
                .retryOptions(requestRetryOptions);

        if (msiConfiguration.getIsEnabled()) {
            // Managed Identity authentication
            return builder.credential(defaultAzureCredential).buildClient();
        } else {
            // Shared Key authentication
            StorageSharedKeyCredential storageSharedKeyCredential = new StorageSharedKeyCredential(
                    pi.getHierarchicalStorageAccountName(),
                    pi.getHierarchicalStorageAccountKey()
            );
            return builder.credential(storageSharedKeyCredential).buildClient();
        }
    }

    /**
     * @param endpoint Azure DataLake endpoint
     * @return DataLakeServiceClientBuilder
     */
    private DataLakeServiceClientBuilder getDataLakeServiceClientBuilder(final String endpoint) {
        RequestRetryOptions requestRetryOptions = blobStoreRetryConfiguration.getRequestRetryOptions();
        return new DataLakeServiceClientBuilder()
                .endpoint(endpoint)
                .retryOptions(requestRetryOptions);
    }
}
