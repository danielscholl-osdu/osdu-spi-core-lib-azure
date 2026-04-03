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

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.azure.storage.file.datalake.DataLakeServiceClient;

/**
 * Foo.
 */
public interface IDataLakeClientFactory {
    /**
     *
     * @param dataPartitionId dataPartitionId
     * @param pathName pathName
     * @param containerName containerName
     * @return DataLakeDirectoryClient
     */
    DataLakeDirectoryClient getDataLakeDirectoryClient(
            String dataPartitionId,
            String pathName,
            String containerName);

    /**
     *
     * @param dataPartitionId dataPartitionId
     * @return Get DataLakeServiceClient.
     */
    DataLakeServiceClient getDataLakeServiceClient(
             String dataPartitionId);

    /**
     *
     * @param dataPartitionId dataPartitionId
     * @param fileSystemName fileSystemName
     * @return Get DataLakeServiceClient.
     */
    DataLakeServiceClient getDataLakeServiceClient(
            String dataPartitionId,
            String fileSystemName);

    /**
     * @param dataPartitionId dataPartitionId
     * @return BlobServiceClient for the hierarchical storage account.
     */
    BlobServiceClient getBlobServiceClient(
            String dataPartitionId);
}
