//  Copyright © Microsoft Corporation
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.opengroup.osdu.elastic.dependencies;

import org.apache.http.HttpStatus;
import org.opengroup.osdu.azure.cache.ElasticCredentialsCache;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.indexer.IElasticSettingService;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.multitenancy.ITenantInfoService;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link IElasticSettingService} for Azure.
 */
@Component
@Lazy
public class ElasticClusterSettingsService implements IElasticSettingService {

    /**
     * Provides tenant making the request.
     */
    @Inject
    @Lazy
    private Provider<ITenantInfoService> tenantProvider;

    /**
     * The underlying store for the cluster settings.
     */
    @Inject
    @Lazy
    private IElasticRepository esRepo;

    /**
     * A cache of tenant-specific cluster settings.
     */
    @Inject
    @Lazy
    private ElasticCredentialsCache clusterSettingsCache;

    /**
     * OSDU logger.
     */
    @Inject
    @Lazy
    private JaxRsDpsLog log;

    /**
     * @return {@link ClusterSettings} that can be used to connect to Elasticsearch.
     */
    @Override
    public ClusterSettings getElasticClusterInformation() {
        TenantInfo tenantInfo = tenantProvider.get().getTenantInfo();
        return getElasticClusterInformationForPartition(tenantInfo.getDataPartitionId());
    }

    /**
     * Retrieve {@link ClusterSettings} for particular partition.
     * @param partitionId Partition ID.
     * @return {@link ClusterSettings} that can be used to connect to Elasticsearch.
     */
    @Override
    public ClusterSettings getElasticClusterInformationForPartition(final String partitionId) {
        TenantInfo tenantInfo = tenantProvider.get().getTenantInfoForPartition(partitionId);
        String cacheKey = this.clusterSettingsCache.getCacheKey(tenantInfo.getName());

        ClusterSettings cachedSettings = this.clusterSettingsCache.get(cacheKey);
        if (cachedSettings != null) {
            return cachedSettings;
        }

        this.log.warning(String.format("elastic-credential cache missed for tenant: %s", tenantInfo.getName()));
        ClusterSettings clusterSettings = esRepo.getElasticClusterSettings(tenantInfo);
        if (clusterSettings == null) {
            throw new AppException(HttpStatus.SC_NOT_FOUND, "Tenant not found", "No information about the given tenant was found");
        }

        this.clusterSettingsCache.put(cacheKey, clusterSettings);
        return clusterSettings;
    }

    /**
     * @return {@link Map} that can be used to show versions of elastic search from connected services
     */
    @Override
    public Map<String, ClusterSettings> getAllClustersSettings() {
        List<TenantInfo> tenantInfos = tenantProvider.get().getAllTenantInfos();
        Map<String, ClusterSettings> mapClusterSettings = new HashMap<>();
        for (TenantInfo tenantInfo: tenantInfos) {
            ClusterSettings clusterSettings = getClusterSettingsByTenantInfo(tenantInfo);
            if (clusterSettings != null) {
                mapClusterSettings.put(tenantInfo.getDataPartitionId(), clusterSettings);
            }
        }
        return mapClusterSettings;
    }
    /**
     * @return {@link ClusterSettings} helper function to get cluster setting for each tenant.
     * @param tenantInfo tenant for which this function is called
     */
    private ClusterSettings getClusterSettingsByTenantInfo(final TenantInfo tenantInfo) {
        String cacheKey = this.clusterSettingsCache.getCacheKey(tenantInfo.getName());

        ClusterSettings cachedSettings = this.clusterSettingsCache.get(cacheKey);
        if (cachedSettings != null) {
            return cachedSettings;
        }
        this.log.warning(String.format("elastic-credential cache missed for tenant: %s", tenantInfo.getName()));
        try {
            ClusterSettings clusterSettings = esRepo.getElasticClusterSettings(tenantInfo);
            if (clusterSettings != null) {
                this.clusterSettingsCache.put(cacheKey, clusterSettings);
                return clusterSettings;
            }
            this.log.warning(String.format("cluster setting were not found for tenant: %s", tenantInfo.getName()));
        } catch (Exception e) {
            this.log.warning(String.format("exception while fetching cluster settings: %s", e.getMessage()));
        }
        return null;
    }
}
