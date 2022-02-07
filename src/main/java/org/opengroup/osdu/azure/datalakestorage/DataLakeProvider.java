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
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This is configuration bean to provide DataLakeStore component.
 */
@Configuration
@ConditionalOnProperty(value = "azure.datalakestorage.enabled", havingValue = "true", matchIfMissing = true)
public class DataLakeProvider {
    /**
     * Creates instance of {@link IDataLakeClientFactory}.
     * @param defaultAzureCredential Azure credentials to use.
     * @param partitionServiceClient Partition service client to use.
     * @return instance of {@link DataLakeClientFactoryImpl}
     */
    @Bean
    public IDataLakeClientFactory buildDataLakeClientFactory(final DefaultAzureCredential defaultAzureCredential,
                                                            final PartitionServiceClient partitionServiceClient) {
        return new DataLakeClientFactoryImpl(defaultAzureCredential, partitionServiceClient);
    }

    /**
     * Create instance of {@link DataLakeStore}.
     * @param dataLakeClientFactory Factory which provides a BlobClient.
     * @return instance of {@link DataLakeStore}
     */
    @Bean
    public DataLakeStore buildDataLakeStore(final IDataLakeClientFactory dataLakeClientFactory) {
        return new DataLakeStore(dataLakeClientFactory);
    }
}
