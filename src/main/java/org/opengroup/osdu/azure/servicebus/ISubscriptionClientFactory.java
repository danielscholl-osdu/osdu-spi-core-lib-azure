package org.opengroup.osdu.azure.servicebus;

import com.microsoft.azure.servicebus.SubscriptionClient;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;

/**
 * Interface for Subscription Client Factory to return appropriate subscription client
 * instance for each tenant based on data partition, service bus topic and subscription Name.
 */
public interface ISubscriptionClientFactory {
    /**
     * @param dataPartitionId Data Partition Id
     * @param topicName Service Bus Topic
     * @param subscriptionName Service Bus Subscription
     * @return A client configured to communicate with a Service Bus Subscription
     * @throws ServiceBusException  Exception thrown by {@link SubscriptionClient}
     * @throws InterruptedException Exception thrown by {@link SubscriptionClient}
     */
    SubscriptionClient getClient(String dataPartitionId, String topicName, String subscriptionName)
            throws ServiceBusException, InterruptedException;
}