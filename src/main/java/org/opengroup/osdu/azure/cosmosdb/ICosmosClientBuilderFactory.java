package org.opengroup.osdu.azure.cosmosdb;

import com.azure.cosmos.CosmosClientBuilder;

/**
 * Interface for Cosmos Client Builder Factory to return appropriate cosmos client builder.
 */
public interface ICosmosClientBuilderFactory {

    /**
     * This method needs to create CosmosClientBuilder objects.
     * @return new CosmosClientBuilder
     */
    CosmosClientBuilder getCosmosClientBuilder();
}
