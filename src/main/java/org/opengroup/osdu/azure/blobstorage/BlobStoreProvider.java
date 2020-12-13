package org.opengroup.osdu.azure.blobstorage;

import com.azure.identity.DefaultAzureCredential;
import org.opengroup.osdu.azure.cache.BlobServiceClientCache;
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
     * @param blobServiceClientCache Blob service client cache to use.
     * @return instance of {@link BlobServiceClientFactoryImpl}
     */
    @Bean
    public IBlobServiceClientFactory buildBlobClientFactory(final DefaultAzureCredential defaultAzureCredential,
                                                            final PartitionServiceClient partitionServiceClient,
                                                            final BlobServiceClientCache blobServiceClientCache) {
        return new BlobServiceClientFactoryImpl(defaultAzureCredential, partitionServiceClient, blobServiceClientCache);
    }

    /**
     * Create instance of {@link BlobStore}.
     * @param blobServiceClientFactory Factory which provides a BlobClient.
     * @param logger logger to use for logging.
     * @return instance of {@link BlobStore}
     */
    @Bean
    public BlobStore buildBlobStore(final IBlobServiceClientFactory blobServiceClientFactory, final ILogger logger) {
        return new BlobStore(blobServiceClientFactory, logger);
    }
}
