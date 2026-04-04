package org.opengroup.osdu.azure.graph;

import com.azure.identity.DefaultAzureCredential;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GraphServiceClientFactoryTest {
    @Mock
    private Map<String, GraphServiceClient> graphServiceClientMap;
    @Mock
    private DefaultAzureCredential azureCredential;

    @InjectMocks
    private GraphServiceClientFactory sut;

    @Test
    void getGraphServiceClient_successfullyReturnsClient_ifValidationsOk() {
        sut.getGraphServiceClient("data-partition-id");
    }

    @Test
    void getGraphServiceClient_throwsException_ifEmptyDataPartitionIdIsProvided() {
        assertThrows(IllegalArgumentException.class, () -> {
            sut.getGraphServiceClient("");
        });
    }

    @Test
    void getGraphServiceClient_throwsException_ifNullDataPartitionIdIsProvided() {
        assertThrows(NullPointerException.class, () -> {
            sut.getGraphServiceClient(null);
        });
    }

    @Test
    void initialize_createsEmptyMap() {
        sut.initialize();
        // After initialize, getting a client should create one via computeIfAbsent on a real map
        GraphServiceClient client = sut.getGraphServiceClient("test-partition");
        assertNotNull(client);
    }
}