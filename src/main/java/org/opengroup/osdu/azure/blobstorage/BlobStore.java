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

import com.azure.core.util.polling.SyncPoller;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobCopyInfo;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.opengroup.osdu.core.common.logging.ILogger;
import org.opengroup.osdu.core.common.model.http.AppException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;

/**
 * A simpler interface to interact with Azure blob storage.
 * Usage examples:
 * <pre>
 * {@code
 *      @Autowired
 *      private BlobStore blobStore;
 *
 *      String readFromStorageContainerExample()
 *      {
 *          String content = blobStorage.readFromStorageContainer("dataPartitionId", "filePath", "containerName");
 *          if (content != null)
 *                 return content;
 *      }
 *
 *      void writeToStorageContainerExample()
 *      {
 *          blobStorage.writeToStorageContainer("dataPartitionId", "filePath", "content", "containerName");
 *      }
 *
 *      void deleteFromStorageContainerExample()
 *      {
 *          Boolean success = blobStorage.deleteFromStorageContainer("dataPartitionId", "filePath", "containerName");
 *      }
 *
 *      void getSasTokenExample()
 *      {
 *          int expiryDays = 7;
 *          OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(expiryDays);
 *          BlobSasPermission permissions = (new BlobSasPermission()).setReadPermission(true).setCreatePermission(true);
 *          String sasToken = blobStorage.getSasToken("dataPartitionId", "filePath", "containerName", expiryTime, permissions);
 *      }
 *
 *      void copyFileExample()
 *      {
 *          BlobCopyInfo copyInfo = blobStore.copyFile("dataPartitionId", "filePath", "containerName", "sourceFilePath");
 *          System.out.println("copy info " + copyInfo.getCopyStatus());
 *      }
 * }
 * </pre>
 */
public class BlobStore {
    private IBlobServiceClientFactory blobServiceClientFactory;
    private ILogger logger;

    /**
     * Constructor to create BlobStore.
     * @param factory Factory that provides blob client.
     * @param loggerInstance logger instance to be used for logging.
     */
    public BlobStore(final IBlobServiceClientFactory factory, final ILogger loggerInstance) {
        this.blobServiceClientFactory = factory;
        this.logger = loggerInstance;
    }

    private static final String LOG_PREFIX = "azure-core-lib";

    /**
     * @param filePath        Path of file to be read.
     * @param dataPartitionId Data partition id
     * @param containerName   Name of the storage container
     * @return the content of file with provided file path.
     */
    public String readFromStorageContainer(
            final String dataPartitionId,
            final String filePath,
            final String containerName) {
        BlobContainerClient blobContainerClient = getBlobContainerClient(dataPartitionId, containerName);
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient();
        try (ByteArrayOutputStream downloadStream = new ByteArrayOutputStream()) {
            blockBlobClient.download(downloadStream);
            return downloadStream.toString(StandardCharsets.UTF_8.name());
        } catch (BlobStorageException ex) {
            if (ex.getErrorCode().equals(BlobErrorCode.BLOB_NOT_FOUND)) {
                throw handleBlobStoreException(404, "Specified blob was not found", ex);
            }
            throw handleBlobStoreException(500, "Failed to read specified blob", ex);
        } catch (UnsupportedEncodingException ex) {
            throw handleBlobStoreException(400, String.format("Encoding was not correct for item with name=%s", filePath), ex);
        } catch (IOException ex) {

            throw handleBlobStoreException(500, String.format("Malformed document for item with name=%s", filePath), ex);
        }
    }

    /**
     * @param filePath        Path of file to be deleted.
     * @param dataPartitionId Data partition id
     * @param containerName   Name of the storage container
     * @return boolean indicating whether the deletion of given file was successful or not.
     */
    public boolean deleteFromStorageContainer(
            final String dataPartitionId,
            final String filePath,
            final String containerName) {
        BlobContainerClient blobContainerClient = getBlobContainerClient(dataPartitionId, containerName);
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient();
        try {
            blockBlobClient.delete();
            return true;
        } catch (BlobStorageException ex) {
            if (ex.getErrorCode().equals(BlobErrorCode.BLOB_NOT_FOUND)) {
                throw handleBlobStoreException(404, "Specified blob was not found", ex);
            }
            throw handleBlobStoreException(500, "Failed to delete blob", ex);
        }
    }

    /**
     * @param filePath        Path of file to be written at.
     * @param content         Content to be written in the file.
     * @param dataPartitionId Data partition id
     * @param containerName   Name of the storage container
     */
    public void writeToStorageContainer(
            final String dataPartitionId,
            final String filePath,
            final String content,
            final String containerName) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        int bytesSize = bytes.length;
        BlobContainerClient blobContainerClient = getBlobContainerClient(dataPartitionId, containerName);
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient();
        try (ByteArrayInputStream dataStream = new ByteArrayInputStream(bytes)) {
            blockBlobClient.upload(dataStream, bytesSize, true);
        } catch (BlobStorageException ex) {
            throw handleBlobStoreException(500, "Failed to upload file content.", ex);
        } catch (IOException ex) {
            throw handleBlobStoreException(500, String.format("Malformed document for item with name=%s", filePath), ex);
        }
    }

    /**
     * @param dataPartitionId Data partition id
     * @param filePath        Path of file (blob) for which SAS token needs to be generated
     * @param containerName   Name of the storage container
     * @param expiryTime      Time after which the token expires
     * @param permissions     Permissions for the given blob
     * @return SAS token for the given blob
     */
    public String getSasToken(final String dataPartitionId, final String filePath, final String containerName, final OffsetDateTime expiryTime, final BlobSasPermission permissions) {
        BlobContainerClient blobContainerClient = getBlobContainerClient(dataPartitionId, containerName);
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient();
        return generateSASToken(blockBlobClient, expiryTime, permissions);
    }

    /**
     * @param dataPartitionId Data partition id
     * @param filePath        Path of file (blob) for which SAS token needs to be generated
     * @param containerName   Name of the storage container
     * @param expiryTime      Time after which the token expires
     * @param permissions     Permissions for the given blob
     * @return Generates Pre-Signed URL for a given blob.
     */
    public String generatePreSignedURL(final String dataPartitionId, final String filePath, final String containerName, final OffsetDateTime expiryTime, final BlobSasPermission permissions) {
        BlobContainerClient blobContainerClient = getBlobContainerClient(dataPartitionId, containerName);
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient();
        return blockBlobClient.getBlobUrl() + "?" + generateSASToken(blockBlobClient, expiryTime, permissions);
    }

    /**
     * Generates pre-signed url to a blob container.
     * @param dataPartitionId data partition id
     * @param containerName Name of the storage container
     * @param expiryTime Time after which the token expires
     * @param permissions permissions for the given container
     * @return Generates pre-signed url for a given container
     */
    public String generatePreSignedURL(final String dataPartitionId, final String containerName, final OffsetDateTime expiryTime, final BlobContainerSasPermission permissions) {
        BlobContainerClient blobContainerClient = getBlobContainerClient(dataPartitionId, containerName);
        return blobContainerClient.getBlobContainerUrl() + "?" + generateSASToken(blobContainerClient, expiryTime, permissions);
    }

    /**
     * Method is used to copy a file specified at Source URL to the provided destination.
     * @param dataPartitionId Data partition id
     * @param filePath        Path of file (blob) to which the file has to be copied
     * @param containerName   Name of the storage container
     * @param sourceUrl       URL of the file from where the file contents have to be copied
     * @return Blob Copy Final Result.
     */
    public BlobCopyInfo copyFile(final String dataPartitionId, final String filePath, final String containerName,
                         final String sourceUrl) {
        BlobContainerClient blobContainerClient = getBlobContainerClient(dataPartitionId, containerName);
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient();

        SyncPoller<BlobCopyInfo, Void> result = blockBlobClient.beginCopy(sourceUrl, Duration.ofSeconds(1));
        return result.waitForCompletion().getValue();
    }
    /**
     *
     * @param blockBlobClient Blob client
     * @param expiryTime Time after which SAS Token expires
     * @param permissions Permissions for the given blob
     * @return Generates SAS Token.
     */
    private String generateSASToken(final BlockBlobClient blockBlobClient, final OffsetDateTime expiryTime, final BlobSasPermission permissions) {
        BlobServiceSasSignatureValues blobServiceSasSignatureValues = new BlobServiceSasSignatureValues(expiryTime, permissions);
        return blockBlobClient.generateSas(blobServiceSasSignatureValues);
    }

    /**
     * @param client Container client
     * @param expiryTime Time after which SAS Token expires
     * @param permissions Permissions for the given container
     * @return Generates SAS Token.
     */
    private String generateSASToken(final BlobContainerClient client, final OffsetDateTime expiryTime, final BlobContainerSasPermission permissions) {
        BlobServiceSasSignatureValues blobServiceSasSignatureValues = new BlobServiceSasSignatureValues(expiryTime, permissions);
        return client.generateSas(blobServiceSasSignatureValues);
    }

    /**
     * @param dataPartitionId Data partition id.
     * @param containerName   Name of storage container.
     * @return blob container client corresponding to the dataPartitionId.
     */
    private BlobContainerClient getBlobContainerClient(final String dataPartitionId, final String containerName) {
        try {
            BlobServiceClient serviceClient = blobServiceClientFactory.getBlobServiceClient(dataPartitionId);
            return serviceClient.getBlobContainerClient(containerName);
        } catch (AppException ex) {
            throw handleBlobStoreException(ex.getError().getCode(), "Error creating creating blob container client.", ex);
        } catch (Exception ex) {
            throw handleBlobStoreException(500, "Error creating creating blob container client.", ex);
        }
    }

    /**
     * Logs and returns instance of AppException.
     *
     * @param status       Response status code
     * @param errorMessage Error message
     * @param ex           Original exception
     * @return Instance of AppException
     */
    private AppException handleBlobStoreException(final int status, final String errorMessage, final Exception ex) {
        logger.warning(LOG_PREFIX, errorMessage, Collections.<String, String>emptyMap());
        return new AppException(status, errorMessage, ex.getMessage(), ex);
    }
}
