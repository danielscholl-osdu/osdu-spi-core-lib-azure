package org.opengroup.osdu.azure.servicebus;

import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.SubscriptionClient;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.opengroup.osdu.azure.cache.SubscriptionClientCache;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.common.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Implementation for ISubscriptionClientFactory.
 */
@Component
@Lazy
public class SubscriptionClientFactoryImpl implements ISubscriptionClientFactory {

    @Autowired
    private PartitionServiceClient partitionService;

    @Autowired
    private SubscriptionClientCache clientCache;

    /**
     * @param dataPartitionId Data Partition Id
     * @param topicName Service Bus Topic Name
     * @param subscriptionName Service Bus Subscription Name
     * @return A client configured to communicate with a Service Bus Subscription
     * @throws ServiceBusException  Exception thrown by {@link SubscriptionClient}
     * @throws InterruptedException Exception thrown by {@link SubscriptionClient}
     */
    @Override
    public SubscriptionClient getClient(final String dataPartitionId, final String topicName, final String subscriptionName) throws ServiceBusException, InterruptedException {
        Validators.checkNotNullAndNotEmpty(dataPartitionId, "dataPartitionId");
        Validators.checkNotNullAndNotEmpty(topicName, "topicName");
        Validators.checkNotNullAndNotEmpty(subscriptionName, "subscriptionName");

        String entityPath = String.format("%s/subscriptions/%s", topicName, subscriptionName);
        String cacheKey = String.format("%s-%s", dataPartitionId, entityPath);
        if (this.clientCache.containsKey(cacheKey)) {
            return this.clientCache.get(cacheKey);
        }

        PartitionInfoAzure pi = this.partitionService.getPartition(dataPartitionId);
        String serviceBusConnectionString = pi.getSbConnection();
        ConnectionStringBuilder connectionStringBuilder = new ConnectionStringBuilder(
                serviceBusConnectionString,
                entityPath
        );

        SubscriptionClient subscriptionClient = new SubscriptionClient(connectionStringBuilder, ReceiveMode.PEEKLOCK);
        this.clientCache.put(cacheKey, subscriptionClient);

        return subscriptionClient;


    }
}