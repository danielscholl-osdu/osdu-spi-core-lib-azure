package org.opengroup.osdu.azure.cosmosdb;

import com.azure.cosmos.CosmosClientBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Implementation for ICosmosClientBuilderFactory.
 */
@Component
@Lazy
public class CosmosClientBuilderFactory implements ICosmosClientBuilderFactory {

    /**
     * This method needs to create CosmosClientBuilder objects.
     * @return new CosmosClientBuilder
     */
    @Override
    public CosmosClientBuilder getCosmosClientBuilder() {
        return new CosmosClientBuilder();
    }
}
