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

import javax.inject.Provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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

    void mockTenant(String tenantName) {
        TenantInfo tenant = new TenantInfo();
        tenant.setName(tenantName);
        ITenantInfoService tenantService = mock(ITenantInfoService.class);

        doReturn(tenantService).when(tenantProvider).get();
        doReturn(tenant).when(tenantService).getTenantInfo();
    }

    @Test
    void get_willCacheBasedOnTenantName(){
        doReturn(mock(ClusterSettings.class)).when(esRepo).getElasticClusterSettings(any());

        mockTenant("t1");
        esSettingsService.getElasticClusterInformation();
        esSettingsService.getElasticClusterInformation();

        mockTenant("t2");
        esSettingsService.getElasticClusterInformation();
        esSettingsService.getElasticClusterInformation();

        verify(esRepo, times(2)).getElasticClusterSettings(any());
    }
}
