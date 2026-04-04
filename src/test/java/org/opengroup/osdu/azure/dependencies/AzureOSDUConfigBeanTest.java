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
package org.opengroup.osdu.azure.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.di.AzureActiveDirectoryConfiguration;

import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;

@ExtendWith(MockitoExtension.class)
public class AzureOSDUConfigBeanTest {

    @Mock
    private AzureActiveDirectoryConfiguration aadConfiguration;

    @Mock
    private SecretClient secretClient;

    @Mock
    private KeyVaultSecret keyVaultSecret;

    private AzureOSDUConfig config;

    @BeforeEach
    void setUp() throws Exception {
        config = new AzureOSDUConfig();
        Field aadField = AzureOSDUConfig.class.getDeclaredField("aadConfiguration");
        aadField.setAccessible(true);
        aadField.set(config, aadConfiguration);
    }

    @Test
    public void aadClientID_returns_configured_value() {
        when(aadConfiguration.getAadClientId()).thenReturn("test-client-id");

        String result = config.aadClientID();

        assertEquals("test-client-id", result);
    }

    @Test
    public void appDevSpUsername_returns_clientId_when_configured() {
        when(aadConfiguration.getClientId()).thenReturn("configured-client-id");

        String result = config.appDevSpUsername(secretClient);

        assertEquals("configured-client-id", result);
    }

    @Test
    public void appDevSpUsername_falls_back_to_keyvault_when_clientId_empty() {
        when(aadConfiguration.getClientId()).thenReturn("");
        when(secretClient.getSecret("app-dev-sp-username")).thenReturn(keyVaultSecret);
        when(keyVaultSecret.getValue()).thenReturn("kv-username");

        String result = config.appDevSpUsername(secretClient);

        assertEquals("kv-username", result);
    }

    @Test
    public void appDevSpTenantId_returns_tenantId_when_configured() {
        when(aadConfiguration.getTenantId()).thenReturn("configured-tenant-id");

        String result = config.appDevSpTenantId(secretClient);

        assertEquals("configured-tenant-id", result);
    }

    @Test
    public void appDevSpUsername_falls_back_to_keyvault_when_clientId_null() {
        when(aadConfiguration.getClientId()).thenReturn(null);
        when(secretClient.getSecret("app-dev-sp-username")).thenReturn(keyVaultSecret);
        when(keyVaultSecret.getValue()).thenReturn("kv-username");

        String result = config.appDevSpUsername(secretClient);

        assertEquals("kv-username", result);
    }

    @Test
    public void appDevSpPassword_returns_clientSecret_when_configured() {
        when(aadConfiguration.getClientSecret()).thenReturn("configured-secret");

        String result = config.appDevSpPassword(secretClient);

        assertEquals("configured-secret", result);
    }

    @Test
    public void appDevSpTenantId_falls_back_to_keyvault_when_tenantId_null() {
        when(aadConfiguration.getTenantId()).thenReturn(null);
        when(secretClient.getSecret("app-dev-sp-tenant-id")).thenReturn(keyVaultSecret);
        when(keyVaultSecret.getValue()).thenReturn("kv-tenant-id");

        String result = config.appDevSpTenantId(secretClient);

        assertEquals("kv-tenant-id", result);
    }

    @Test
    public void appDevSpTenantId_falls_back_to_keyvault_when_tenantId_empty() {
        when(aadConfiguration.getTenantId()).thenReturn("");
        when(secretClient.getSecret("app-dev-sp-tenant-id")).thenReturn(keyVaultSecret);
        when(keyVaultSecret.getValue()).thenReturn("kv-tenant-id");

        String result = config.appDevSpTenantId(secretClient);

        assertEquals("kv-tenant-id", result);
    }
}
