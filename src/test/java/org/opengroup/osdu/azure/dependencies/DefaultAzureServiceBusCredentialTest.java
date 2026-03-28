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

package org.opengroup.osdu.azure.dependencies;

import com.azure.core.credential.AccessToken;
import com.azure.identity.DefaultAzureCredential;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;


@ExtendWith(MockitoExtension.class)
class DefaultAzureServiceBusCredentialTest {
    @Mock
    private DefaultAzureCredential credentialMock;

    @InjectMocks
    private DefaultAzureServiceBusCredential sbCredential;

    @Test
    void defaultConstructor_shouldCreateInstance() {
        DefaultAzureServiceBusCredential credential = new DefaultAzureServiceBusCredential();
        assertNotNull(credential);
    }

    @Test
    void token_shouldBeCached() {
        configureCredentialMock(credentialMock, getTokenWithExpiration(OffsetDateTime.MAX));
        sbCredential.getSecurityTokenAsync("foo-audience");
        sbCredential.getSecurityTokenAsync("foo-audience");

        verify(credentialMock, times(1)).getToken(any());
    }

    @Test
    void token_shouldBeCached_byAudience() {
        configureCredentialMock(credentialMock, getTokenWithExpiration(OffsetDateTime.MAX));
        sbCredential.getSecurityTokenAsync("foo-audience-1");
        sbCredential.getSecurityTokenAsync("foo-audience-1");
        sbCredential.getSecurityTokenAsync("foo-audience-2");
        sbCredential.getSecurityTokenAsync("foo-audience-2");

        verify(credentialMock, times(2)).getToken(any());
    }

    @Test
    void token_shouldEvictFromCacheIfExpired() {
        configureCredentialMock(credentialMock, getTokenWithExpiration(OffsetDateTime.now().minusDays(100)));
        sbCredential.getSecurityTokenAsync("foo-audience");
        sbCredential.getSecurityTokenAsync("foo-audience");

        verify(credentialMock, times(2)).getToken(any());
    }

    private void configureCredentialMock(DefaultAzureCredential credential, AccessToken token) {
        Mono<AccessToken> tokenResponse = Mono.fromSupplier(() -> token);
        doReturn(tokenResponse).when(credential).getToken(any());
    }

    private AccessToken getTokenWithExpiration(OffsetDateTime dateTime) {
        return new AccessToken("foo-token", dateTime);
    }
}
