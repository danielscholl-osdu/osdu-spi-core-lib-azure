package org.opengroup.osdu.azure.cosmosdb.cache.async;

import com.azure.cosmos.CosmosAsyncClient;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Implementation for RemovalListener interface for CosmosAsyncClient, required for the CosmosAsyncClient cache.
 */
@Component
@Slf4j
public class CosmosAsyncClientRemovalListener implements RemovalListener<String, CosmosAsyncClient> {

    /**
     * Implementation for the interface's method. Closes all cosmos async clients before it will be removed from cache.
     * @param notification
     */
    @Override
    public void onRemoval(final RemovalNotification<String, CosmosAsyncClient> notification) {
        CosmosAsyncClient cosmosAsyncClient = notification.getValue();
        if (cosmosAsyncClient != null) {
            try {
                cosmosAsyncClient.close();
            } catch (Exception e) {
                log.error("Error while closing cosmos async client {}", e.getMessage(), e);
            }
        }
    }

}