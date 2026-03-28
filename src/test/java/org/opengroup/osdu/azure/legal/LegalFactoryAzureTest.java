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
package org.opengroup.osdu.azure.legal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.di.LegalAPIConfiguration;
import org.opengroup.osdu.core.common.http.IHttpClient;
import org.opengroup.osdu.core.common.http.json.HttpResponseBodyMapper;
import org.opengroup.osdu.core.common.legal.ILegalProvider;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;

@ExtendWith(MockitoExtension.class)
public class LegalFactoryAzureTest {

    @Mock
    private HttpResponseBodyMapper mapper;

    @Mock
    private IHttpClient httpClient;

    @Mock
    private LegalAPIConfiguration legalAPIConfiguration;

    @Mock
    private DpsHeaders dpsHeaders;

    private LegalFactoryAzure factory;

    @BeforeEach
    void setUp() {
        when(legalAPIConfiguration.getLegalTagApi()).thenReturn("http://legal-api/api/legal/v1");
        when(legalAPIConfiguration.getLegalApiKey()).thenReturn("test-api-key");
        factory = new LegalFactoryAzure(mapper, httpClient, legalAPIConfiguration);
    }

    @Test
    public void should_create_legal_service() {
        ILegalProvider provider = factory.create(dpsHeaders);

        assertNotNull(provider);
    }

    @Test
    public void should_throw_when_headers_null() {
        assertThrows(NullPointerException.class, () -> factory.create(null));
    }
}
