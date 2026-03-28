/*
 * Copyright © Microsoft Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opengroup.osdu.azure.blobstorage;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.identity.DefaultAzureCredential;
import org.opengroup.osdu.azure.logging.DependencyLogger;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.core.common.logging.ILogger;

@ExtendWith(MockitoExtension.class)
public class BlobStoreProviderTest {

    @Mock
    private DefaultAzureCredential credential;

    @Mock
    private PartitionServiceClient partitionServiceClient;

    @Mock
    private ILogger logger;

    @Mock
    private DependencyLogger depLogger;

    @Test
    public void should_build_blob_client_factory() {
        BlobStoreProvider provider = new BlobStoreProvider();

        IBlobServiceClientFactory factory = provider.buildBlobClientFactory(credential, partitionServiceClient);

        assertNotNull(factory);
        assertTrue(factory instanceof BlobServiceClientFactoryImpl);
    }

    @Test
    public void should_build_blob_store() {
        BlobStoreProvider provider = new BlobStoreProvider();
        IBlobServiceClientFactory factory = provider.buildBlobClientFactory(credential, partitionServiceClient);

        BlobStore store = provider.buildBlobStore(factory, logger, depLogger);

        assertNotNull(store);
    }
}
