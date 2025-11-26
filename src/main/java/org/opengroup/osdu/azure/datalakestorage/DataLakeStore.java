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

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.common.sas.SasProtocol;
import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.azure.storage.file.datalake.models.DataLakeStorageException;
import com.azure.storage.file.datalake.models.PathItem;
import com.azure.storage.file.datalake.models.UserDelegationKey;
import com.azure.storage.file.datalake.sas.DataLakeServiceSasSignatureValues;
import com.azure.storage.file.datalake.sas.FileSystemSasPermission;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.azure.logging.DependencyPayload;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A simpler interface to interact with Azure DataLake storage Gen2.
 */
public class DataLakeStore {

    private IDataLakeClientFactory dataLakeClientFactory;
    private static final Logger LOGGER = LoggerFactory.getLogger(DataLakeStore.class);


    /**
     * @param factory factory
     */
    public DataLakeStore(final IDataLakeClientFactory factory) {
        this.dataLakeClientFactory = factory;
    }

    /**
     * create directory in a fileSystem.
     *
     * @param dataPartitionId dataPartitionId
     * @param containerName containerName
     * @param directoryName directoryName
     */
    public void createDirectory(
            final String dataPartitionId,
            final String containerName,
            final String directoryName) {
        DataLakeDirectoryClient dataLakeDirectoryClient = createDataLakeDirectoryClient(dataPartitionId,
                directoryName, containerName);
        dataLakeDirectoryClient.create();
    }

    /**
     *  Generates pre-signed url to a DataLake Directory using the user delegation key.
     *
     * @param dataPartitionId dataPartitionId
     * @param containerName containerName
     * @param directoryName fileName
     * @param expiryTime expiryTime
     * @param permissions permissions
     * @return string
     */
    public String generatePreSignedURL(final String dataPartitionId, final String containerName, final String directoryName,
                                       final OffsetDateTime expiryTime, final FileSystemSasPermission permissions) {
        DataLakeDirectoryClient dataLakeClient = createDataLakeDirectoryClient(dataPartitionId, directoryName, containerName);
        return String.format("%s?%s", dataLakeClient.getDirectoryUrl(), generateSASToken(dataLakeClient, expiryTime, permissions));
    }

    /**
     *  Generates pre-signed url to a DataLake Directory using the user delegation key.
     *
     * @param dataPartitionId dataPartitionId
     * @param containerName containerName
     * @param directoryName fileName
     * @param expiryTime expiryTime
     * @param permissions permissions
     * @return string
     */
    public String generatePreSignedURLWithUserDelegationSas(final String dataPartitionId, final String containerName, final String directoryName,
                                       final OffsetDateTime expiryTime, final FileSystemSasPermission permissions) {
        DataLakeDirectoryClient dataLakeClient = createDataLakeDirectoryClient(dataPartitionId, directoryName, containerName);
        return String.format("%s?%s", dataLakeClient.getDirectoryUrl(), generateSASTokenWithUserDelegationSas(dataPartitionId, containerName, dataLakeClient, expiryTime, permissions));
    }

    /**
     * Generate SaS token to interact with Azure DataLake Gen 2.
     *
     * @param client client
     * @param expiryTime expiryTime
     * @param permissions permissions
     * @return string
     */
    private String generateSASToken(final DataLakeDirectoryClient client, final OffsetDateTime expiryTime,
            final FileSystemSasPermission permissions) {
        DataLakeServiceSasSignatureValues sign = new DataLakeServiceSasSignatureValues(expiryTime, permissions)
                .setStartTime(OffsetDateTime.now())
                .setProtocol(SasProtocol.HTTPS_ONLY);

        final long start = System.currentTimeMillis();
        String sasToken = client.generateSas(sign);
        final long timeTaken = System.currentTimeMillis() - start;
        logDependency("GENERATE_SAS_TOKEN_DATALAKE", client.getDirectoryName(), client.getDirectoryUrl(), timeTaken, String.valueOf(HttpStatus.SC_OK), true);
        return sasToken;
    }

    /**
     * Generate SaS token to interact with Azure DataLake Gen 2.
     *
     * @param dataPartitionId dataPartitionId
     * @param fileSystemName fileSystemName
     * @param client client
     * @param expiryTime expiryTime
     * @param permissions permissions
     * @return string
     */
    private String generateSASTokenWithUserDelegationSas(final String dataPartitionId, final String fileSystemName, final DataLakeDirectoryClient client, final OffsetDateTime expiryTime,
                                    final FileSystemSasPermission permissions) {
        DataLakeServiceSasSignatureValues sign = new DataLakeServiceSasSignatureValues(expiryTime, permissions)
                .setStartTime(OffsetDateTime.now())
                .setProtocol(SasProtocol.HTTPS_ONLY);

        OffsetDateTime startTime = OffsetDateTime.now();
        DataLakeServiceClient dataLakeServiceClient = dataLakeClientFactory.getDataLakeServiceClient(dataPartitionId, fileSystemName);

        UserDelegationKey userDelegationKey = dataLakeServiceClient.getUserDelegationKey(startTime, expiryTime);
        final long start = System.currentTimeMillis();
        String sasToken = client.generateUserDelegationSas(sign, userDelegationKey);

        final long timeTaken = System.currentTimeMillis() - start;
        logDependency("GENERATE_SAS_TOKEN_DATALAKE", client.getDirectoryName(), client.getDirectoryUrl(), timeTaken, String.valueOf(HttpStatus.SC_OK), true);
        return sasToken;
    }

    /**
     * Create DataLakeDirectoryClient.
     *
     * @param dataPartitionId dataPartitionId
     * @param directoryName directoryName
     * @param containerName containerName
     * @return string
     */
    private DataLakeDirectoryClient createDataLakeDirectoryClient(
            final String dataPartitionId,
            final String directoryName,
            final String containerName) {
        try {
            return dataLakeClientFactory.getDataLakeDirectoryClient(dataPartitionId, directoryName, containerName);
        } catch (AppException ex) {
            throw handleDataLakeStoreException(ex.getError().getCode(), "Error creating Hierarchical dataLake container client.", ex);
        } catch (Exception ex) {
            throw handleDataLakeStoreException(500, "Error creating Hierarchical dataLake container client.", ex);
        }
    }

    /**
     * move files from one fileSystem to another fileSystem.
     * @param dataPartitionId dataPartitionId
     * @param sourceContainerName sourceContainerName
     * @param directoryName directoryName
     * @param destinationContainerName destinationContainerName
     * @return DataLakeDirectoryClient Direc
     */
    public DataLakeDirectoryClient moveDirectory(
            final String dataPartitionId,
            final String sourceContainerName,
            final String directoryName,
            final String destinationContainerName) {
        DataLakeDirectoryClient dataLakeDirectoryClient = createDataLakeDirectoryClient(dataPartitionId, directoryName, sourceContainerName);
        return dataLakeDirectoryClient.rename(destinationContainerName, directoryName);
    }

    /**
     * Get the File names from the given directory.
     * @param dataPartitionId dataPartitionId
     * @param containerName containerName
     * @param directoryName directoryName
     * @return the list of file names
     */
    public List<String> getFileNamesFromDirectory(final String dataPartitionId, final String containerName, final String directoryName) {
        DataLakeDirectoryClient dataLakeDirectoryClient = createDataLakeDirectoryClient(dataPartitionId, directoryName, containerName);
        try {
            PagedIterable<PathItem> pathItems = dataLakeDirectoryClient.listPaths(true, false, null, null);
            return extractFileNameFromPath(pathItems);
        } catch (DataLakeStorageException ex) {
            throw handleDataLakeStoreException(ex.getStatusCode(), "Error getting files from directory.", ex);
        } catch (Exception ex) {
            throw handleDataLakeStoreException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Generic error occurred.", ex);
        }
    }

    /**
     * Extract the file name alone from the full path of PathItem.
     * @param pathItems PathItems
     * @return the list of file names
     */
    private  List<String> extractFileNameFromPath(final PagedIterable<PathItem> pathItems) {
        List<String> fileNames = new ArrayList<>();
        for (PathItem pathItem : pathItems) {
            if (!pathItem.isDirectory()) {
                String[] fullPath = pathItem.getName().split("/");
                fileNames.add(fullPath[1]);
            }
        }
        return fileNames;
    }

    /**
     * Log dependency.
     *
     * @param name name
     * @param data data
     * @param target target
     * @param timeTakenInMs timeTakenInMs
     * @param resultCode resultCode
     * @param success success
     */
    private void logDependency(final String name, final String data, final String target,
                               final long timeTakenInMs, final String resultCode, final boolean success) {
        DependencyPayload payload = new DependencyPayload(name, data, Duration.ofMillis(timeTakenInMs), resultCode, success);
        payload.setType("DataLakeStore");
        payload.setTarget(target);
        CoreLoggerFactory.getInstance().getLogger(DataLakeStore.class.getName()).logDependency(payload);
    }

    /**
     * Logs and returns instance of AppException.
     *
     * @param status status
     * @param errorMessage errorMessage
     * @param ex ex
     * @return Exception
     */
    private AppException handleDataLakeStoreException(final int status, final String errorMessage, final Exception ex) {
        LOGGER.error(errorMessage, ex);
        return new AppException(status, errorMessage, ex.getMessage(), ex);
    }
}
