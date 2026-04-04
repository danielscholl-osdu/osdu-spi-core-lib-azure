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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.cache.ElasticCredentialsCache;
import org.opengroup.osdu.azure.cache.ElasticCredentialsCacheImpl;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.multitenancy.ITenantInfoService;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticRepository;
import jakarta.inject.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;


@ExtendWith(MockitoExtension.class)
class ElasticClusterSettingsServiceTest {
    @Mock
    private Provider<ITenantInfoService> tenantProvider;

    @Mock
    private IElasticRepository esRepo;

    @Mock
    private JaxRsDpsLog log;

    @Spy
    private ElasticCredentialsCache cache = new ElasticCredentialsCacheImpl(10000, 10000);

    @InjectMocks
    private ElasticClusterSettingsService esSettingsService;

    void mockTenantWithRequestScope(String tenantName) {
        TenantInfo tenant = new TenantInfo();
        tenant.setName(tenantName);
        tenant.setDataPartitionId(tenantName);
        ITenantInfoService tenantService = mock(ITenantInfoService.class);

        doReturn(tenantService).when(tenantProvider).get();
        doReturn(tenant).when(tenantService).getTenantInfo();
        doReturn(tenant).when(tenantService).getTenantInfoForPartition(tenantName);
    }

    void mockTenantByPartitionId(String tenantName) {
        TenantInfo tenant = new TenantInfo();
        tenant.setName(tenantName);
        tenant.setDataPartitionId(tenantName);
        ITenantInfoService tenantService = mock(ITenantInfoService.class);

        doReturn(tenantService).when(tenantProvider).get();
        doReturn(tenant).when(tenantService).getTenantInfoForPartition(tenantName);
    }

    void mockMultiTenants(List<String> dataPartitionIDs){
        //mock the tenant service
        ITenantInfoService tenantService = mock(ITenantInfoService.class);
        doReturn(tenantService).when(tenantProvider).get();

        List<TenantInfo> tenantInfos = new ArrayList<>();
        //mock the tenants
        for (String tenant: dataPartitionIDs) {
            TenantInfo tn = new TenantInfo();
            tn.setName(tenant);
            tn.setDataPartitionId(tenant);
            tenantInfos.add(tn);
        }
        doReturn(tenantInfos).when(tenantService).getAllTenantInfos();
    }

    @Test
    void get_willCacheBasedOnTenantName() {
        doReturn(mock(ClusterSettings.class)).when(esRepo).getElasticClusterSettings(any());

        mockTenantWithRequestScope("t1");
        esSettingsService.getElasticClusterInformation();
        esSettingsService.getElasticClusterInformation();

        mockTenantWithRequestScope("t2");
        esSettingsService.getElasticClusterInformation();
        esSettingsService.getElasticClusterInformation();

        verify(esRepo, times(2)).getElasticClusterSettings(any());
    }

    @Test
    void get_AllClusterSettings(){
        doReturn(mock(ClusterSettings.class)).when(esRepo).getElasticClusterSettings(any());
        mockMultiTenants(Arrays.asList("t1", "t2", "t3"));
        esSettingsService.getAllClustersSettings();
        verify(esRepo, times(3)).getElasticClusterSettings(any());
    }

    @Test
    void getElasticClusterInformationForPartition_willReturnCorrectSettingsForPartition() {
        ClusterSettings expectedSettings = mock(ClusterSettings.class);
        doReturn(expectedSettings).when(esRepo).getElasticClusterSettings(any());

        mockTenantByPartitionId("t1");
        ClusterSettings result = esSettingsService.getElasticClusterInformationForPartition("t1");

        verify(esRepo, times(1)).getElasticClusterSettings(any());
        assertEquals(expectedSettings, result);
    }

    @Test
    void getElasticClusterInformationForPartition_willCacheBasedOnPartitionId() {
        doReturn(mock(ClusterSettings.class)).when(esRepo).getElasticClusterSettings(any());

        mockTenantByPartitionId("t1");
        esSettingsService.getElasticClusterInformationForPartition("t1");
        esSettingsService.getElasticClusterInformationForPartition("t1");

        mockTenantByPartitionId("t2");
        esSettingsService.getElasticClusterInformationForPartition("t2");
        esSettingsService.getElasticClusterInformationForPartition("t2");

        verify(esRepo, times(2)).getElasticClusterSettings(any());
    }
}
