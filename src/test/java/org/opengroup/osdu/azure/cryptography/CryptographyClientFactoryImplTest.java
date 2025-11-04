package org.opengroup.osdu.azure.cryptography;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.cache.CryptographyClientCache;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.core.common.partition.Property;

import com.azure.identity.DefaultAzureCredential;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;

@ExtendWith(MockitoExtension.class)
class CryptographyClientFactoryImplTest {

  private static final String VALID_DATA_PARTITION_ID = "validDataPartitionId";
  public static final String IDENTIFIER = "https://test-vault.vault.azure.net/keys/test-key/key-version";

  @Mock
  private PartitionServiceClient partitionService;
  @Mock
  private CryptographyClientCache clientCache;
  @Mock
  private DefaultAzureCredential defaultAzureCredential;
  @InjectMocks
  private CryptographyClientFactoryImpl factory;

  @Test
  void should_throwException_given_nullDataPartitionId() {

    NullPointerException nullPointerException = Assertions.assertThrows(NullPointerException.class,
        () -> this.factory.getClient(null));
    assertEquals("partitionId cannot be null!", nullPointerException.getMessage());
  }

  @Test
  void should_throwException_given_emptyDataPartitionId() {

    IllegalArgumentException illegalArgumentException = Assertions.assertThrows(IllegalArgumentException.class,
        () -> this.factory.getClient(""));
    assertEquals("partitionId cannot be empty!", illegalArgumentException.getMessage());
  }

  @Test
  void should_return_validClient_given_validPartitionId() {
    // Setup
    when(this.partitionService.getPartition(VALID_DATA_PARTITION_ID)).thenReturn(
        PartitionInfoAzure.builder()
            .idConfig(Property.builder().value(VALID_DATA_PARTITION_ID).build())
            .cryptographyEncryptionKeyIdentifierConfig(Property.builder().value(IDENTIFIER).build()).build());

    when(this.clientCache.containsKey(any())).thenReturn(false);

    // Act
    CryptographyClient eventGridClient = this.factory.getClient(VALID_DATA_PARTITION_ID);

    // Assert
    assertNotNull(eventGridClient);
    verify(this.clientCache, times(1)).put(any(), any());
  }
}