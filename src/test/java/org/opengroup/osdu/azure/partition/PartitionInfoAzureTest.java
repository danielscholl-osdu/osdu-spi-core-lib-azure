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

package org.opengroup.osdu.azure.partition;

import com.azure.core.exception.ResourceNotFoundException;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.partition.Property;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.azure.logging.ICoreLogger;

@ExtendWith(MockitoExtension.class)
public class PartitionInfoAzureTest {
    @Mock
    private SecretClient secretClient;

    private PartitionInfoAzure partitionInfoAzure;

    @Mock
    private ICoreLogger mockLogger;

    @Mock
    private CoreLoggerFactory mockLoggerFactory;

    private static final String BLOB_ENDPOINT_SECRET = "opendes-storage-blob-endpoint";
    private static final String HIERARCHY_BLOB_ENDPOINT_SECRET = "opendes-hierarchical-storage-blob-endpoint";
    private static final String PARTITION_DNS_BLOB_ENDPOINT = "https://opendes.blob.core.windows.net";
    private static final String STORAGE_V1_BLOB_ENDPOINT = "https://opendes.blob.core.windows.net";
    private static final String HIERARCHICAL_STORAGE_V1_BLOB_ENDPOINT = "https://opendes.dfs.core.windows.net";

    @BeforeEach
    public void setup() {
        CoreLoggerFactory.resetFactory();
        CoreLoggerFactory.getInstance();

        partitionInfoAzure = new PartitionInfoAzure();
        partitionInfoAzure.configureSecretClient(secretClient);
    }

    /**
     * The test will verify scenario where the Partition service is not seeded to have new property for Blob endpoint.
     */
    @Test
    public void testGetStorageBlobEndpoint_WithPartitionInfoNotContainingRequiredField() {
        // Setup
        Property storageAccountName = new Property(false, "opendes");
        partitionInfoAzure.setStorageAccountNameConfig(storageAccountName);

        // Test and Result
        assertEquals(STORAGE_V1_BLOB_ENDPOINT, partitionInfoAzure.getStorageBlobEndpoint());
    }

    /**
     * The test will verify scenario where Partition Service is seeded to have new property.
     * But the secret does not exist in the Key vault.
     */
    @Test
    public void testGetStorageBlobEndpoint_WithKeyVaultNotHavingRequiredSecret() {
        // Setup
        Property storageBlobEndpoint = new Property(true, BLOB_ENDPOINT_SECRET);
        Property storageAccountName = new Property(false, "opendes");

        partitionInfoAzure.setStorageAccountBlobEndpointConfig(storageBlobEndpoint);
        partitionInfoAzure.setStorageAccountNameConfig(storageAccountName);

        when(secretClient.getSecret(BLOB_ENDPOINT_SECRET)).thenThrow(ResourceNotFoundException.class);

        // Test and Result
        assertEquals(STORAGE_V1_BLOB_ENDPOINT, partitionInfoAzure.getStorageBlobEndpoint());
    }

    /**
     * The test will verify scenario where Partition Service is seeded to have new Property and the required secret
     * exists in Key Vault.
     */
    @Test
    public void testGetStorageBlobEndpoint_WhenInfraAndPartitionInfoAreUpdated() {
        // Setup
        Property storageBlobEndpoint = new Property(true, BLOB_ENDPOINT_SECRET);

        partitionInfoAzure.setStorageAccountBlobEndpointConfig(storageBlobEndpoint);

        when(secretClient.getSecret(BLOB_ENDPOINT_SECRET))
                .thenReturn(new KeyVaultSecret(BLOB_ENDPOINT_SECRET, PARTITION_DNS_BLOB_ENDPOINT));

        // Test and Result
        assertEquals(PARTITION_DNS_BLOB_ENDPOINT, partitionInfoAzure.getStorageBlobEndpoint());
    }

    @Test
    public void testGetHierarchicalStorageBlobEndpoint_WithKeyVaultNotHavingRequiredSecret() {
        // Setup
        Property storageBlobEndpoint = new Property(true, HIERARCHY_BLOB_ENDPOINT_SECRET);
        Property storageAccountName = new Property(false, "opendes");

        partitionInfoAzure.setHierarchicalStorageAccountBlobEndpointConfig(storageBlobEndpoint);
        partitionInfoAzure.setHierarchicalStorageAccountNameConfig(storageAccountName);

        when(secretClient.getSecret(HIERARCHY_BLOB_ENDPOINT_SECRET)).thenThrow(ResourceNotFoundException.class);

        // Test and Result
        assertEquals(HIERARCHICAL_STORAGE_V1_BLOB_ENDPOINT, partitionInfoAzure.getHierarchicalStorageAccountBlobEndpoint());
    }

    /**
     * The test will verify scenario where Partition Service is seeded to have new Property and the required secret
     * exists in Key Vault.
     */
    @Test
    public void testGetHierarchicalStorageBlobEndpoint_WhenInfraAndPartitionInfoAreUpdated() {
        // Setup
        Property storageBlobEndpoint = new Property(true, HIERARCHY_BLOB_ENDPOINT_SECRET);

        partitionInfoAzure.setHierarchicalStorageAccountBlobEndpointConfig(storageBlobEndpoint);

        when(secretClient.getSecret(HIERARCHY_BLOB_ENDPOINT_SECRET))
                .thenReturn(new KeyVaultSecret(HIERARCHY_BLOB_ENDPOINT_SECRET, HIERARCHICAL_STORAGE_V1_BLOB_ENDPOINT));

        // Test and Result
        assertEquals(HIERARCHICAL_STORAGE_V1_BLOB_ENDPOINT, partitionInfoAzure.getHierarchicalStorageAccountBlobEndpoint());
    }
}
