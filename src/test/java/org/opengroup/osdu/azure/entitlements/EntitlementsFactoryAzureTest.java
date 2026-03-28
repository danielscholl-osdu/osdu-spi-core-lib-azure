/*
 * Copyright © Microsoft Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opengroup.osdu.azure.entitlements;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.entitlements.EntitlementsAPIConfig;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsService;
import org.opengroup.osdu.core.common.http.IHttpClient;
import org.opengroup.osdu.core.common.http.json.HttpResponseBodyMapper;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;

@ExtendWith(MockitoExtension.class)
public class EntitlementsFactoryAzureTest {

    @Mock
    private HttpResponseBodyMapper mapper;

    @Mock
    private IHttpClient httpClient;

    @Mock
    private DpsHeaders dpsHeaders;

    private EntitlementsFactoryAzure factory;

    @BeforeEach
    void setUp() {
        EntitlementsAPIConfig config = EntitlementsAPIConfig.builder()
                .rootUrl("http://entitlements/api/entitlements/v2")
                .apiKey("test-key")
                .build();
        factory = new EntitlementsFactoryAzure(config, mapper, httpClient);
    }

    @Test
    public void should_create_entitlements_service() {
        IEntitlementsService service = factory.create(dpsHeaders);
        assertNotNull(service);
    }

    @Test
    public void should_throw_when_headers_null() {
        assertThrows(NullPointerException.class, () -> factory.create(null));
    }
}
