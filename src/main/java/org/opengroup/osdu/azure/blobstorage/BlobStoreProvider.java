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
import org.opengroup.osdu.azure.logging.DependencyLogger;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.core.common.logging.ILogger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This is configuration bean to provide BlobStore component.
 */
@Configuration
@ConditionalOnProperty(value = "azure.blobStore.required", havingValue = "true", matchIfMissing = false)
public class BlobStoreProvider {
    /**
     * Creates instance of {@link IBlobServiceClientFactory}.
     * @param defaultAzureCredential Azure credentials to use.
     * @param partitionServiceClient Partition service client to use.
     * @return instance of {@link BlobServiceClientFactoryImpl}
     */
    @Bean
    public IBlobServiceClientFactory buildBlobClientFactory(final DefaultAzureCredential defaultAzureCredential,
                                                            final PartitionServiceClient partitionServiceClient) {
        return new BlobServiceClientFactoryImpl(defaultAzureCredential, partitionServiceClient);
    }

    /**
     * Create instance of {@link BlobStore}.
     * @param blobServiceClientFactory Factory which provides a BlobClient.
     * @param logger logger to use for logging.
     * @param depLogger dependency logger to use for dependency logging.
     * @return instance of {@link BlobStore}
     */
    @Bean
    public BlobStore buildBlobStore(final IBlobServiceClientFactory blobServiceClientFactory, final ILogger logger, final DependencyLogger depLogger) {
        return new BlobStore(blobServiceClientFactory, logger, depLogger);
    }
}
