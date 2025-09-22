// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.azure.serviceBusManager;

import com.microsoft.azure.servicebus.ClientSettings;
import com.microsoft.azure.servicebus.management.ManagementClient;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.security.ManagedIdentityTokenProvider;
import org.opengroup.osdu.azure.dependencies.DefaultAzureServiceBusCredential;
import org.opengroup.osdu.azure.di.MSIConfiguration;
import org.opengroup.osdu.azure.di.WorkloadIdentityConfiguration;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.common.Validators;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation for Service Bus ManagementClient Factory to return appropriate ServiceBusManagementClient based on the data partition id.
 */
@Component
@ConditionalOnProperty(value = "azure.serviceBus.manager.enabled", havingValue = "true", matchIfMissing = true)
public class ManagementClientFactoryImpl implements IManagementClientFactory {
    private static final String MANAGEMENT_CLIENT_ERROR_MESSAGE = "Unable to create management client";
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementClientFactoryImpl.class);

    @Autowired
    private PartitionServiceClient partitionService;

    private Map<String, ManagementClient> managementClientMap;

    @Autowired
    private MSIConfiguration msiConfiguration;

    @Autowired
    private WorkloadIdentityConfiguration workloadIdentityConfiguration;

    /**
     * Initializes the private variables as required.
     */
    @PostConstruct
    public void initialize() {
        managementClientMap = new ConcurrentHashMap<>();
    }

    /**
     * @param partitionId partition id
     * @return ServiceBusManagementClient
     */

    public ManagementClient getManager(final String partitionId) {
        Validators.checkNotNullAndNotEmpty(partitionId, "partitionId");
        String cacheKey = partitionId + "-serviceBusManagementClient";
        if (this.managementClientMap.containsKey(cacheKey)) {
            return (ManagementClient) this.managementClientMap.get(cacheKey);
        }

        return this.managementClientMap.computeIfAbsent(cacheKey, managementClient -> createManagementClient(partitionId));
    }

    /**
     * @param partitionId Data Partition Id
     * @return Management Client Instance using appropriate token provider
     * Auth selection order:
     * 1. If MSI is enabled:
     *    1. Try Workload Identity first (modern K8s method)
     *    2. Fallback to Pod Identity (legacy K8s method)
     * 2. Otherwise, use service bus connection string
     */
    private ManagementClient createManagementClient(final String partitionId) {
        try {
            PartitionInfoAzure pi = this.partitionService.getPartition(partitionId);

            ManagementClient managementClient;

            if (msiConfiguration.getIsEnabled()) {
                String serviceBusNamespace = pi.getSbNamespace();
                URI namespaceEndpointURI = new URI(String.format("sb://%s.servicebus.windows.net/", serviceBusNamespace));

                ClientSettings clientSettings = workloadIdentityConfiguration.getIsEnabled()
                        ? new ClientSettings(new DefaultAzureServiceBusCredential())
                        : new ClientSettings(new ManagedIdentityTokenProvider());

                managementClient = new ManagementClient(namespaceEndpointURI, clientSettings);
            } else {
                String serviceBusConnectionString = pi.getSbConnection();
                ConnectionStringBuilder connectionStringBuilder = new ConnectionStringBuilder(serviceBusConnectionString);
                managementClient = new ManagementClient(connectionStringBuilder);
            }

            LOGGER.debug("Management client creation successful for partition Id : " + partitionId);
            return managementClient;
        } catch (Exception e) {
            LOGGER.error("Management client creation failed for partition Id : " + partitionId);
            throw new AppException(500, "Internal Server Error", MANAGEMENT_CLIENT_ERROR_MESSAGE, e);
        }
    }
}

