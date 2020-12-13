package org.opengroup.osdu.azure.servicebus;

import com.microsoft.azure.servicebus.SubscriptionClient;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.cache.SubscriptionClientCache;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.core.common.partition.Property;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
public class SubscriptionClientFactoryImplTest {

    @InjectMocks
    private SubscriptionClientFactoryImpl subscriptionClientFactory;
    @Mock
    private PartitionServiceClient partitionServiceClient;
    @Mock
    private SubscriptionClientCache clientCache;

    private static final String PARTITION_ID = "dataPartitionId";
    private static final String TOPIC_NAME = "testTopic";
    private static final String SUBSCRIPTION_NAME = "testSubscription";
    private static final String SB_ENTITY_PATH = "testTopic/subscriptions/testSubscription";
    private static final String SB_CONNECTION_STRING = "Endpoint=sb://test-bus.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=testKey";
    @BeforeEach
    void init() {
        initMocks(this);
    }

    @Test
    public void should_throwException_given_nullSubscriptionName() {
        try {
            this.subscriptionClientFactory.getClient(PARTITION_ID, TOPIC_NAME, null);
        } catch (NullPointerException ex) {
            assertEquals("subscriptionName cannot be null!", ex.getMessage());
        } catch (Exception ex) {
            fail("Should not get any other exception. Received " + ex.getClass());
        }
    }

    @Test
    public void should_throwException_given_emptySubscriptionName() {
        try {
            this.subscriptionClientFactory.getClient(PARTITION_ID, TOPIC_NAME, "");
        } catch (IllegalArgumentException ex) {
            assertEquals("subscriptionName cannot be empty!", ex.getMessage());
        } catch (Exception ex) {
            fail("Should not get any other exception. Received " + ex.getClass());
        }
    }

    @Test
    public void should_throwException_given_nullTopicName() {
        try {
            this.subscriptionClientFactory.getClient(PARTITION_ID, null, SUBSCRIPTION_NAME);
        } catch (NullPointerException ex) {
            assertEquals("topicName cannot be null!", ex.getMessage());
        } catch (Exception ex) {
            fail("Should not get any other exception. Received " + ex.getClass());
        }
    }

    @Test
    public void should_throwException_given_emptyTopicName() {
        try {
            this.subscriptionClientFactory.getClient(PARTITION_ID, "", SUBSCRIPTION_NAME);
        } catch (IllegalArgumentException ex) {
            assertEquals("topicName cannot be empty!", ex.getMessage());
        } catch (Exception ex) {
            fail("Should not get any other exception. Received " + ex.getClass());
        }
    }


    @Test
    public void should_return_cachedClient_when_cachedEarlier() throws ServiceBusException, InterruptedException {
        SubscriptionClient subscriptionClient = mock(SubscriptionClient.class);
        final String cacheKey = String.format("%s-%s", PARTITION_ID, SB_ENTITY_PATH);
        when(this.clientCache.containsKey(cacheKey)).thenReturn(true);
        when(this.clientCache.get(cacheKey)).thenReturn(subscriptionClient);

        this.subscriptionClientFactory.getClient(PARTITION_ID, TOPIC_NAME, SUBSCRIPTION_NAME);
        verify(this.partitionServiceClient, never()).getPartition(PARTITION_ID);
    }

    @Test
    public void should_return_client_when_partition_valid() throws ServiceBusException, InterruptedException {
        when(this.partitionServiceClient.getPartition(PARTITION_ID)).thenReturn(
                PartitionInfoAzure.builder().sbConnectionConfig(Property.builder().value(SB_CONNECTION_STRING).build())
                        .build());
        assertNotNull(this.subscriptionClientFactory.getClient(PARTITION_ID, TOPIC_NAME, SUBSCRIPTION_NAME));
    }

}
