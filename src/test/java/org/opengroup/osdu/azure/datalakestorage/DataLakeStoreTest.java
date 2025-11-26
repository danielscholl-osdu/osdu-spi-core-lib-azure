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

import com.azure.core.http.HttpResponse;
import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.azure.storage.file.datalake.models.DataLakeStorageException;
import com.azure.storage.file.datalake.models.PathInfo;
import com.azure.storage.file.datalake.models.PathItem;
import com.azure.storage.file.datalake.models.UserDelegationKey;
import com.azure.storage.file.datalake.sas.DataLakeServiceSasSignatureValues;
import com.azure.storage.file.datalake.sas.FileSystemSasPermission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.logging.CoreLogger;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DataLakeStoreTest {

    private static final String PARTITION_ID = "dataPartitionId";
    private static final String DIRECTORY_NAME = "directoryName";
    private static final String FILE_SYSTEM_NAME = "fileSystemName";
    private static final String DESTINATION_FILE_SYSTEM = "destinationSystemName";

    @Mock
    private CoreLoggerFactory coreLoggerFactory;

    @Mock
    private IDataLakeClientFactory dataLakeClientFactory;

    @Mock
    private DataLakeDirectoryClient dataLakeDirectoryClient;

    @Mock
    private PathInfo pathInfo;

    @InjectMocks
    private DataLakeStore dataLakeStore;

    @Mock
    private CoreLogger coreLogger;

    @Mock
    private DataLakeServiceClient dataLakeServiceClient;

    @Mock
    private UserDelegationKey userDelegationKey;

    @Mock
    private HttpResponse httpResponse;

    @Captor
    private ArgumentCaptor<DataLakeServiceSasSignatureValues> dataLakeServiceSasSignatureValuesCaptor;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        mockSingleton(coreLoggerFactory);
        lenient().when(coreLoggerFactory.getLogger(anyString())).thenReturn(coreLogger);
        lenient().when(dataLakeClientFactory.getDataLakeDirectoryClient(
                PARTITION_ID, DIRECTORY_NAME, FILE_SYSTEM_NAME)).thenReturn(dataLakeDirectoryClient);
    }

    @AfterEach
    public void takeDown() {
        resetSingleton();
    }

    /**
     * Workaround for inability to mock static methods like getInstance().
     *
     * @param mock CoreLoggerFactory mock instance
     */
    private void mockSingleton(CoreLoggerFactory mock) {
        try {
            Field instance = CoreLoggerFactory.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, mock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reset workaround for inability to mock static methods like getInstance().
     */
    private void resetSingleton() {
        try {
            Field instance = CoreLoggerFactory.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
            instance.setAccessible(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void createDirectory_Success() {
        when(dataLakeDirectoryClient.create()).thenReturn(pathInfo);
        dataLakeStore.createDirectory(PARTITION_ID, FILE_SYSTEM_NAME, DIRECTORY_NAME);

        verify(dataLakeClientFactory).getDataLakeDirectoryClient(PARTITION_ID, DIRECTORY_NAME, FILE_SYSTEM_NAME);
        verify(dataLakeDirectoryClient).create();
    }

    @Test
    public void createDirectory_AppException() {
        doThrow(DataLakeStorageException.class).when(dataLakeClientFactory).getDataLakeDirectoryClient(PARTITION_ID, DIRECTORY_NAME, FILE_SYSTEM_NAME);
        try {
            dataLakeStore.createDirectory(PARTITION_ID, FILE_SYSTEM_NAME, DIRECTORY_NAME);
        } catch (AppException exception) {
            assertEquals(500, exception.getError().getCode());
        } catch (Exception exception) {
            fail("should not get different error code");
        }
    }

    @Test
    public void generatePreSignedURL_ReturnsValidSasToken() {
        String containerSasToken = "containerSasToken";
        String containerUrl = "containerUrl";
        String containerPreSignedUrl = String.format("%s?%s", containerUrl, containerSasToken);

        doReturn(containerUrl).when(dataLakeDirectoryClient).getDirectoryUrl();
        doReturn(DIRECTORY_NAME).when(dataLakeDirectoryClient).getDirectoryName();
        doReturn(containerSasToken).when(dataLakeDirectoryClient).generateSas(any(DataLakeServiceSasSignatureValues.class));

        int expiryDays = 1;
        OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(expiryDays);

        FileSystemSasPermission fileSystemSasPermission =
                (new FileSystemSasPermission()).setReadPermission(true).setCreatePermission(true);

        String obtainedPreSignedUrl = dataLakeStore.generatePreSignedURL(PARTITION_ID, FILE_SYSTEM_NAME, DIRECTORY_NAME,
                expiryTime, fileSystemSasPermission);

        verify(dataLakeDirectoryClient).generateSas(dataLakeServiceSasSignatureValuesCaptor.capture());

        assertEquals(fileSystemSasPermission.toString(), dataLakeServiceSasSignatureValuesCaptor.getValue().getPermissions());
        assertEquals(expiryTime, dataLakeServiceSasSignatureValuesCaptor.getValue().getExpiryTime());
        assertEquals(containerPreSignedUrl, obtainedPreSignedUrl);
    }

    @Test
    public void generatePreSignedURLWithUserDelegationSas_ReturnsValidSasToken() {
        String containerSasToken = "containerSasToken";
        String containerUrl = "containerUrl";
        String containerPreSignedUrl = String.format("%s?%s", containerUrl, containerSasToken);
        int expiryDays = 1;
        OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(expiryDays);

        ArgumentCaptor<UserDelegationKey> userDelegationKeyArgumentCaptor = ArgumentCaptor.forClass(UserDelegationKey.class);

        doReturn(containerUrl).when(dataLakeDirectoryClient).getDirectoryUrl();
        doReturn(DIRECTORY_NAME).when(dataLakeDirectoryClient).getDirectoryName();
        doReturn(containerSasToken).when(dataLakeDirectoryClient).generateUserDelegationSas(any(DataLakeServiceSasSignatureValues.class), any(UserDelegationKey.class));
        doReturn(dataLakeServiceClient).when(dataLakeClientFactory).getDataLakeServiceClient(PARTITION_ID, FILE_SYSTEM_NAME);
        doReturn(userDelegationKey).when(dataLakeServiceClient).getUserDelegationKey(any(OffsetDateTime.class), any(OffsetDateTime.class));

        FileSystemSasPermission fileSystemSasPermission =
                (new FileSystemSasPermission()).setReadPermission(true).setCreatePermission(true);

        String obtainedPreSignedUrl = dataLakeStore.generatePreSignedURLWithUserDelegationSas(PARTITION_ID, FILE_SYSTEM_NAME, DIRECTORY_NAME,
                expiryTime, fileSystemSasPermission);

        verify(dataLakeDirectoryClient).generateUserDelegationSas(dataLakeServiceSasSignatureValuesCaptor.capture(), userDelegationKeyArgumentCaptor.capture());

        assertEquals(fileSystemSasPermission.toString(), dataLakeServiceSasSignatureValuesCaptor.getValue().getPermissions());
        assertEquals(containerPreSignedUrl, obtainedPreSignedUrl);
    }

    @Test
    public void generatePreSignedURL_ThrowException() {
        doThrow(DataLakeStorageException.class).when(dataLakeDirectoryClient).generateSas(any(DataLakeServiceSasSignatureValues.class));
        int expiryDays = 1;
        OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(expiryDays);

        FileSystemSasPermission fileSystemSasPermission =
                (new FileSystemSasPermission()).setReadPermission(true).setCreatePermission(true);

        try {
            String obtainedPreSignedUrl = dataLakeStore.generatePreSignedURL(PARTITION_ID, FILE_SYSTEM_NAME, DIRECTORY_NAME,
                    expiryTime, fileSystemSasPermission);
        } catch (DataLakeStorageException ex) {
            verify(dataLakeDirectoryClient, times(1)).generateSas(any(DataLakeServiceSasSignatureValues.class));
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void moveDirectory_success() {
        DataLakeDirectoryClient mockDirectoryClient = mock(DataLakeDirectoryClient.class);
        when(dataLakeDirectoryClient.rename(DESTINATION_FILE_SYSTEM, DIRECTORY_NAME))
                .thenReturn(mockDirectoryClient);

        DataLakeDirectoryClient directoryClient = dataLakeStore.moveDirectory(PARTITION_ID,
                FILE_SYSTEM_NAME, DIRECTORY_NAME, DESTINATION_FILE_SYSTEM);

        assertEquals(mockDirectoryClient, directoryClient);
    }

    @Test
    public void moveDirectory_throwException() {
        doThrow(DataLakeStorageException.class).when(dataLakeDirectoryClient)
                .rename(DESTINATION_FILE_SYSTEM, DIRECTORY_NAME);

        try {
            dataLakeStore.moveDirectory(PARTITION_ID,
                    FILE_SYSTEM_NAME, DIRECTORY_NAME, DESTINATION_FILE_SYSTEM);
        } catch (DataLakeStorageException ex) {
            verify(dataLakeDirectoryClient).rename(DESTINATION_FILE_SYSTEM, DIRECTORY_NAME);
        } catch (Exception ex) {
            fail("should not get different error");
        }
    }

    @Test
    public void shouldReturnFileNameListFromDirectory() {
        PathItem pathItem1 = createPathItem("file1.txt");
        PathItem pathItem2 = createPathItem("file2.txt");
        PagedIterable<PathItem> pathItems = mock(PagedIterable.class);
        Iterator<PathItem> pathItemIterator = mock(Iterator.class);

        when(pathItems.iterator()).thenReturn(pathItemIterator);
        when(pathItems.iterator().hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(pathItemIterator.next()).thenReturn(pathItem1).thenReturn(pathItem2);
        when(dataLakeDirectoryClient.listPaths(true, false, null, null))
                .thenReturn(pathItems);

        List<String> fileNamesFromDirectory = dataLakeStore.getFileNamesFromDirectory(PARTITION_ID, FILE_SYSTEM_NAME, DIRECTORY_NAME);

        assertEquals(Arrays.asList("file1.txt", "file2.txt"), fileNamesFromDirectory);
    }

    @Test
    public void shouldReturnEmptyFileNamesList_whenDirectoryIsEmpty() {

        PagedIterable<PathItem> pathItems = mock(PagedIterable.class);
        Iterator<PathItem> pathItemIterator = mock(Iterator.class);

        when(pathItems.iterator()).thenReturn(pathItemIterator);
        when(pathItems.iterator().hasNext()).thenReturn(false);
        when(dataLakeDirectoryClient.listPaths(true, false, null, null))
                .thenReturn(pathItems);

        List<String> fileNamesFromDirectory = dataLakeStore.getFileNamesFromDirectory(PARTITION_ID, FILE_SYSTEM_NAME, DIRECTORY_NAME);

        assertEquals(emptyList(), fileNamesFromDirectory);
    }

    @Test
    public void getFileNamesFromDirectory_shouldThrowNotFound_whenPathNotFound() {
        DataLakeStorageException dataLakeStorageException = new DataLakeStorageException("The specified path does not exist.", httpResponse, "PathNotFound");
        doThrow(dataLakeStorageException).when(dataLakeDirectoryClient)
                .listPaths(true,false, null, null);
        when(httpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND.value());
        String errorMessage = "Error getting files from directory.";

        AppException ex = assertThrows(AppException.class,
                () -> dataLakeStore.getFileNamesFromDirectory(PARTITION_ID, FILE_SYSTEM_NAME, DIRECTORY_NAME));

        assertEquals(HttpStatus.NOT_FOUND.value(), ex.getError().getCode());
        assertEquals(errorMessage, ex.getError().getReason());
    }

    @Test
    public void getFileNamesFromDirectory_shouldThrowInternalServerError_whenGenericExceptionThrown() {
        doThrow(RuntimeException.class).when(dataLakeDirectoryClient)
                .listPaths(true,false, null, null);
        String errorMessage = "Generic error occurred.";

        AppException ex = assertThrows(AppException.class,
                () -> dataLakeStore.getFileNamesFromDirectory(PARTITION_ID, FILE_SYSTEM_NAME, DIRECTORY_NAME));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getError().getCode());
        assertEquals(errorMessage, ex.getError().getReason());
    }

    private PathItem createPathItem(String fileName) {
        return new PathItem("test-etag", null, 1, "test-group", false, DIRECTORY_NAME+"/"+fileName, "test-owner", "read");
    }

}
