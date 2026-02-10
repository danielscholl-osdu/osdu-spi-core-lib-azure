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

package org.opengroup.osdu.azure.cache;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredential;
import io.lettuce.core.RedisCredentials;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.logging.CoreLogger;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AzureRedisMsiCredentialsProviderTest {

    private static final String TEST_PRINCIPAL_ID = "test-principal-id";
    private static final String TEST_ACCESS_TOKEN = "test-access-token-value";
    private static final String AZURE_REDIS_SCOPE = "https://redis.azure.com/.default";

    @Mock
    private DefaultAzureCredential mockCredential;

    @Mock
    private CoreLoggerFactory coreLoggerFactory;

    @Mock
    private CoreLogger coreLogger;

    private AzureRedisMsiCredentialsProvider provider;

    /**
     * Workaround for inability to mock static methods like getInstance().
     */
    private void mockSingleton(CoreLoggerFactory mock) {
        try {
            Field instance = CoreLoggerFactory.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, mock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reset workaround for inability to mock static methods like getInstance().
     */
    private void resetSingleton() {
        try {
            Field instance = CoreLoggerFactory.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
            instance.setAccessible(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        provider = new AzureRedisMsiCredentialsProvider(TEST_PRINCIPAL_ID, mockCredential);
        mockSingleton(coreLoggerFactory);
        when(coreLoggerFactory.getLogger(anyString())).thenReturn(coreLogger);
    }

    @AfterEach
    void tearDown() {
        resetSingleton();
    }

    /**
     * Helper to verify expected error logging for MSI credential failures.
     */
    private void verifyMsiCredentialErrorLogging() {
        verify(coreLogger).error(eq("Failed to acquire access token from MSI"));
        verify(coreLogger).error(eq("AzureRedisMsiCredentialsProvider error resolving credentials"), any(RuntimeException.class));
    }

    @Test
    public void should_resolve_credentials_successfully() {
        // Arrange
        OffsetDateTime expiryTime = OffsetDateTime.now().plusHours(1);
        AccessToken accessToken = new AccessToken(TEST_ACCESS_TOKEN, expiryTime);
        when(mockCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(accessToken));

        // Act
        RedisCredentials credentials = provider.resolveCredentials().block();

        // Assert - verify credentials are correct
        assertNotNull(credentials);
        assertEquals(TEST_PRINCIPAL_ID, credentials.getUsername());
        assertArrayEquals(TEST_ACCESS_TOKEN.toCharArray(), credentials.getPassword());

        // Assert - verify correct Azure Redis scope was used
        verify(mockCredential).getToken(argThat(context -> {
            assertNotNull(context);
            assertEquals(1, context.getScopes().size());
            assertTrue(context.getScopes().contains(AZURE_REDIS_SCOPE));
            return true;
        }));

        // Assert - verify logging with token expiry
        verify(coreLogger).info(argThat((String msg) -> 
            msg.contains("AzureRedisMsiCredentialsProvider providing fresh credentials") &&
            msg.contains("Token expires at")
        ), eq(expiryTime));
    }

    @Test
    public void should_fail_when_access_token_is_null() {
        // Arrange
        when(mockCredential.getToken(any(TokenRequestContext.class)))
            .thenReturn(Mono.fromCallable(() -> null));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            provider.resolveCredentials().block();
        });

        assertEquals("Failed to resolve MSI credentials", exception.getMessage());
        verifyMsiCredentialErrorLogging();
    }

    @Test
    public void should_fail_when_token_string_is_null() {
        // Arrange
        AccessToken accessToken = new AccessToken(null, OffsetDateTime.now().plusHours(1));
        when(mockCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(accessToken));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            provider.resolveCredentials().block();
        });

        assertEquals("Failed to resolve MSI credentials", exception.getMessage());
        verifyMsiCredentialErrorLogging();
    }

    @Test
    public void should_fail_when_credential_getToken_throws_exception() {
        // Arrange
        when(mockCredential.getToken(any(TokenRequestContext.class)))
            .thenReturn(Mono.error(new RuntimeException("Azure credential error")));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            provider.resolveCredentials().block();
        });

        assertEquals("Failed to resolve MSI credentials", exception.getMessage());
        verify(coreLogger).error(eq("AzureRedisMsiCredentialsProvider error resolving credentials"), any(Exception.class));
    }
}
