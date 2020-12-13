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

package org.opengroup.osdu.azure.blobstorage;

import com.azure.identity.DefaultAzureCredential;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.opengroup.osdu.azure.cache.BlobServiceClientCache;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.common.Validators;

/**
 * Implementation for IBlobServiceClientFactory.
 */
public class BlobServiceClientFactoryImpl implements IBlobServiceClientFactory {
    private DefaultAzureCredential defaultAzureCredential;
    private PartitionServiceClient partitionService;
    private BlobServiceClientCache clientCache;

    /**
     * Constructor to initialize instance of {@link BlobServiceClientFactoryImpl}.
     * @param credentials Azure Credentials to use
     * @param partitionServiceClient Partition service client to use
     * @param blobServiceClientCache Blob service client cache to use
     */
    public BlobServiceClientFactoryImpl(final DefaultAzureCredential credentials,
                                        final PartitionServiceClient partitionServiceClient,
                                        final BlobServiceClientCache blobServiceClientCache) {
        this.defaultAzureCredential = credentials;
        this.partitionService = partitionServiceClient;
        this.clientCache = blobServiceClientCache;
    }

    /**
     * @param dataPartitionId data partition id.
     * @return BlobServiceClient corresponding to the given data partition id.
     */
    @Override
    public BlobServiceClient getBlobServiceClient(final String dataPartitionId) {
        Validators.checkNotNull(defaultAzureCredential, "Credential");
        Validators.checkNotNullAndNotEmpty(dataPartitionId, "dataPartitionId");

        String cacheKey = String.format("%s-blobServiceClient", dataPartitionId);
        if (this.clientCache.containsKey(cacheKey)) {
            return this.clientCache.get(cacheKey);
        }

        PartitionInfoAzure pi = this.partitionService.getPartition(dataPartitionId);
        String endpoint = String.format("https://%s.blob.core.windows.net", pi.getStorageAccountName());

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .endpoint(endpoint)
                .credential(defaultAzureCredential)
                .buildClient();

        this.clientCache.put(cacheKey, blobServiceClient);

        return blobServiceClient;
    }
}
