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
import com.azure.storage.common.policy.RequestRetryOptions;
import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.azure.storage.file.datalake.DataLakeFileSystemClient;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.di.BlobStoreRetryConfiguration;
import org.opengroup.osdu.azure.di.MSIConfiguration;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DataLakeClientFactoryImplTest {

    private static final String PARTITION_ID = "dataPartitionId";
    private static final String DIRECTORY_NAME = "directoryName";
    private static final String FILE_SYSTEM_NAME = "fileSystemName";
    private static final String ACCOUNT_NAME = "accountName";
    private static final String ACCOUNT_KEY = "accountKey";
    private static final String ACCOUNT_ENDPOINT = "https://opendes.dfs.core.windows.net";

    @Mock
    private DataLakeServiceClient mockDataLakeServiceClient;

    @Mock
    private DataLakeFileSystemClient mockDataLakeFileSystemClient;

    @Mock
    private DataLakeDirectoryClient mockDataLakeDirectoryClient;

    @Mock
    private PartitionServiceClient mockPartitionServiceClient;

    @Mock
    private PartitionInfoAzure mockPartitionInfoAzure;

    @Mock
    private Map<String, DataLakeServiceClient> dataLakeContainerClientMap;

    @Mock
    private BlobStoreRetryConfiguration mockBlobStoreRetryConfiguration;

    @Mock
    private RequestRetryOptions mockRequestRetryOptions;

    @Mock
    private MSIConfiguration mockMsiConfiguration;

    @Mock
    private DefaultAzureCredential mockDefaultAzureCredential;

    @InjectMocks
    private DataLakeClientFactoryImpl dataLakeClientFactoryImpl;


    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        lenient().when(mockDataLakeServiceClient.getFileSystemClient(FILE_SYSTEM_NAME))
                .thenReturn(mockDataLakeFileSystemClient);
        lenient().when(mockDataLakeFileSystemClient.getDirectoryClient(DIRECTORY_NAME))
                .thenReturn(mockDataLakeDirectoryClient);
        lenient().when(mockPartitionServiceClient.getPartition(PARTITION_ID))
                .thenReturn(mockPartitionInfoAzure);
        lenient().when(mockPartitionInfoAzure.getHierarchicalStorageAccountName())
                .thenReturn(ACCOUNT_NAME);
        lenient().when(mockPartitionInfoAzure.getHierarchicalStorageAccountKey())
                .thenReturn(ACCOUNT_KEY);
        lenient().when(mockPartitionInfoAzure.getHierarchicalStorageAccountBlobEndpoint())
                .thenReturn(ACCOUNT_ENDPOINT);
    }

    @Test
    public void getDataLakeDirectoryClient_Success() {
        when(mockBlobStoreRetryConfiguration.getRequestRetryOptions())
                .thenReturn(mockRequestRetryOptions);

        DataLakeDirectoryClient dataLakeDirectoryClient = dataLakeClientFactoryImpl.getDataLakeDirectoryClient(
                PARTITION_ID, DIRECTORY_NAME, FILE_SYSTEM_NAME);

        assertNotNull(dataLakeDirectoryClient);
        String cacheKey = String.format("%s-%s", PARTITION_ID, FILE_SYSTEM_NAME);
        verify(dataLakeContainerClientMap, times(0)).get(cacheKey);
    }

    @Test
    public void getDataLakeDirectoryClient_MsiEnabled() {
        when(mockBlobStoreRetryConfiguration.getRequestRetryOptions())
                .thenReturn(mockRequestRetryOptions);
        when(mockMsiConfiguration.getIsEnabled()).thenReturn(true);

        DataLakeDirectoryClient dataLakeDirectoryClient = dataLakeClientFactoryImpl.getDataLakeDirectoryClient(
                PARTITION_ID, DIRECTORY_NAME, FILE_SYSTEM_NAME);

        assertNotNull(dataLakeDirectoryClient);
        String cacheKey = String.format("%s-%s", PARTITION_ID, FILE_SYSTEM_NAME);
        verify(dataLakeContainerClientMap, times(0)).get(cacheKey);
    }

    @Test
    public void getDataLakeDirectoryClient_should_throwException_given_nullDataPartitionId() {
        try {
            dataLakeClientFactoryImpl.getDataLakeDirectoryClient(null, DIRECTORY_NAME, FILE_SYSTEM_NAME);
        } catch (NullPointerException ex) {
            assertEquals("dataPartitionId cannot be null!", ex.getMessage());
        } catch (Exception ex) {
            fail("Should not get any other exception. Received " + ex.getClass());
        }
    }

    @Test
    public void getDataLakeDirectoryClient_should_throwException_given_emptyDataPartitionId() {
        try {
            dataLakeClientFactoryImpl.getDataLakeDirectoryClient("", DIRECTORY_NAME, FILE_SYSTEM_NAME);
        } catch (IllegalArgumentException ex) {
            assertEquals("dataPartitionId cannot be empty!", ex.getMessage());
        } catch (Exception ex) {
            fail("Should not get any other exception. Received " + ex.getClass());
        }
    }

    @Test
    public void getDataLakeDirectoryClient_should_return_cachedContainer_when_cachedEarlier() {
        String cacheKey = String.format("%s-%s", PARTITION_ID, FILE_SYSTEM_NAME);

        when(this.dataLakeContainerClientMap.containsKey(cacheKey)).thenReturn(true);
        when(this.dataLakeContainerClientMap.get(cacheKey)).thenReturn(mockDataLakeServiceClient);

        DataLakeDirectoryClient dataLakeDirectoryClient = dataLakeClientFactoryImpl.getDataLakeDirectoryClient(
                PARTITION_ID, DIRECTORY_NAME, FILE_SYSTEM_NAME);

        assertNotNull(dataLakeDirectoryClient);
       verify(this.dataLakeContainerClientMap).containsKey(cacheKey);
       verify(this.dataLakeContainerClientMap).get(cacheKey);
    }

    @Test
    public void getDataLakeServiceClient_should_return_cachedContainer_when_cachedEarlier() {
        String cacheKey = String.format("%s-%s", PARTITION_ID, FILE_SYSTEM_NAME);

        when(this.dataLakeContainerClientMap.containsKey(cacheKey)).thenReturn(true);
        when(this.dataLakeContainerClientMap.get(cacheKey)).thenReturn(mockDataLakeServiceClient);

        DataLakeServiceClient dataLakeServiceClient = dataLakeClientFactoryImpl.getDataLakeServiceClient(
                PARTITION_ID, FILE_SYSTEM_NAME);

        assertNotNull(dataLakeServiceClient);
        verify(this.dataLakeContainerClientMap).containsKey(cacheKey);
        verify(this.dataLakeContainerClientMap).get(cacheKey);
    }

    @Test
    public void getDataLakeServiceClient_MsiEnabled() {
        when(mockBlobStoreRetryConfiguration.getRequestRetryOptions())
                .thenReturn(mockRequestRetryOptions);
        when(mockMsiConfiguration.getIsEnabled()).thenReturn(true);

        DataLakeServiceClient dataLakeServiceClient = dataLakeClientFactoryImpl.getDataLakeServiceClient(
                PARTITION_ID, FILE_SYSTEM_NAME);

        assertNotNull(dataLakeServiceClient);
        String cacheKey = String.format("%s-%s", PARTITION_ID, FILE_SYSTEM_NAME);
        verify(dataLakeContainerClientMap, times(0)).get(cacheKey);
    }

    @Test
    public void getDataLakeServiceClient_Success() {
        when(mockBlobStoreRetryConfiguration.getRequestRetryOptions())
                .thenReturn(mockRequestRetryOptions);

        DataLakeServiceClient dataLakeServiceClient = dataLakeClientFactoryImpl.getDataLakeServiceClient(
                PARTITION_ID, FILE_SYSTEM_NAME);

        assertNotNull(dataLakeServiceClient);
        String cacheKey = String.format("%s-%s", PARTITION_ID, FILE_SYSTEM_NAME);
        verify(dataLakeContainerClientMap, times(0)).get(cacheKey);
    }

    @Test
    public void getBlobServiceClient_Success() {
        when(mockBlobStoreRetryConfiguration.getRequestRetryOptions())
                .thenReturn(mockRequestRetryOptions);

        BlobServiceClient blobServiceClient = dataLakeClientFactoryImpl.getBlobServiceClient(PARTITION_ID);

        assertNotNull(blobServiceClient);
    }

    @Test
    public void getBlobServiceClient_MsiEnabled() {
        when(mockBlobStoreRetryConfiguration.getRequestRetryOptions())
                .thenReturn(mockRequestRetryOptions);
        when(mockMsiConfiguration.getIsEnabled()).thenReturn(true);

        BlobServiceClient blobServiceClient = dataLakeClientFactoryImpl.getBlobServiceClient(PARTITION_ID);

        assertNotNull(blobServiceClient);
    }

    @Test
    public void getBlobServiceClient_should_return_cached_when_calledTwice() {
        when(mockBlobStoreRetryConfiguration.getRequestRetryOptions())
                .thenReturn(mockRequestRetryOptions);

        BlobServiceClient first = dataLakeClientFactoryImpl.getBlobServiceClient(PARTITION_ID);
        BlobServiceClient second = dataLakeClientFactoryImpl.getBlobServiceClient(PARTITION_ID);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
        // Partition service should only be called once since the second call uses the cache
        verify(mockPartitionServiceClient, times(1)).getPartition(PARTITION_ID);
    }
}
