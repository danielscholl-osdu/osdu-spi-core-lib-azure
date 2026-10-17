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

package org.opengroup.osdu.azure.legal;

import org.opengroup.osdu.azure.di.LegalAPIConfiguration;
import org.opengroup.osdu.core.common.http.IHttpClient;
import org.opengroup.osdu.core.common.http.json.HttpResponseBodyMapper;
import org.opengroup.osdu.core.common.legal.ILegalFactory;
import org.opengroup.osdu.core.common.legal.ILegalProvider;
import org.opengroup.osdu.core.common.legal.LegalAPIConfig;
import org.opengroup.osdu.core.common.legal.LegalService;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Implements ILegalFactory.
 * Please use LEGAL_TAG_API nomenclature for legal api url in application.properties to use this class.
 */
@Component
@Primary
@ConditionalOnProperty(value = "azure.legal.factory.enabled", havingValue = "true", matchIfMissing = true)
public class LegalFactoryAzure implements ILegalFactory {

    private LegalAPIConfig config;
    private final HttpResponseBodyMapper mapper;
    private final IHttpClient client;
    private final LegalAPIConfiguration legalAPIConfiguration;

    /**
     * Constructor Injection for following 3 parameter.
     *
     * @param configuration LegalAPIConfig
     * @param bodyMapper    HttpResponseBodyMapper
     * @param httpClient    IHttpClient
     */
    @Autowired
    public LegalFactoryAzure(final HttpResponseBodyMapper bodyMapper, final IHttpClient httpClient, final LegalAPIConfiguration configuration) {
        this.mapper = bodyMapper;
        this.client = httpClient;
        this.legalAPIConfiguration = configuration;
        this.config = LegalAPIConfig.builder()
                .rootUrl(legalAPIConfiguration.getLegalTagApi())
                .apiKey(legalAPIConfiguration.getLegalApiKey())
                .build();
    }

    /**
     * Creates LegalService.
     *
     * @param dpsHeaders DpsHeaders
     * @return instance of LegalService
     */
    @Override
    public ILegalProvider create(final DpsHeaders dpsHeaders) {
        Objects.requireNonNull(dpsHeaders, "headers cannot be null");
        return new LegalService(this.config,
                this.client,
                dpsHeaders,
                this.mapper);
    }
}
