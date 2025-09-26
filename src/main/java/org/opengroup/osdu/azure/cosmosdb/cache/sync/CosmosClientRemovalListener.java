package org.opengroup.osdu.azure.cosmosdb.cache.sync;

import com.azure.cosmos.CosmosClient;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Implementation for RemovalListener interface for CosmosClient, required for the CosmosClient cache.
 */
@Component
@Slf4j
public class CosmosClientRemovalListener implements RemovalListener<String, CosmosClient> {

    /**
     * Implementation for the interface's method. Closes all cosmos clients before it will be removed from cache.
     * @param notification
     */
    @Override
    public void onRemoval(final RemovalNotification<String, CosmosClient> notification) {
        CosmosClient cosmosClient = notification.getValue();
        if (cosmosClient != null) {
            try {
                cosmosClient.close();
            } catch (Exception e) {
                log.error("Error while closing cosmos client {}", e.getMessage(), e);
            }
        }
    }

}