package org.opengroup.osdu.azure.cryptography;

import org.opengroup.osdu.azure.cache.CryptographyClientCache;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.common.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.azure.identity.DefaultAzureCredential;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;

/**
 * Implementation for CryptographyClientFactory.
 */
@Component
@ConditionalOnProperty(value = "azure.cryptography.enabled", havingValue = "true", matchIfMissing = true)
public class CryptographyClientFactoryImpl implements CryptographyClientFactory {

  @Autowired
  private PartitionServiceClient partitionService;
  @Autowired
  private CryptographyClientCache clientCache;
  @Autowired
  private DefaultAzureCredential defaultAzureCredential;

  /**
   * @param partitionId partition id
   * @return CryptographyClient
   */
  @Override
  public CryptographyClient getClient(final String partitionId) {
    Validators.checkNotNullAndNotEmpty(partitionId, "partitionId");

    String cacheKey = partitionId + "-cryptographyClient";
    if (clientCache.containsKey(cacheKey)) {
      return clientCache.get(cacheKey);
    }

    PartitionInfoAzure pi = partitionService.getPartition(partitionId);

    CryptographyClient cryptographyClient = new CryptographyClientBuilder()
        .keyIdentifier(pi.getCryptographyEncryptionKeyIdentifier())
        .credential(defaultAzureCredential)
        .buildClient();
    clientCache.put(cacheKey, cryptographyClient);
    return cryptographyClient;
  }
}
