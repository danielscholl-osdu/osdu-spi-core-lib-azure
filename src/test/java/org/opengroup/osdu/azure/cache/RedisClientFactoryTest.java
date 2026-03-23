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

import com.azure.core.credential.TokenRequestContext;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.identity.DefaultAzureCredential;
import com.azure.security.keyvault.secrets.SecretClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.di.MSIConfiguration;
import org.opengroup.osdu.azure.di.RedisAzureConfiguration;
import org.opengroup.osdu.azure.logging.CoreLogger;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.core.common.cache.IRedisCache;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.redisson.api.RedissonClient;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.MockitoAnnotations.openMocks;

@ExtendWith(MockitoExtension.class)
public class RedisClientFactoryTest {

    private static final String DUMMY_HOST = "dummyHost";
    private static final String SECRET_NOT_FOUND = "SecretDoesNotExists";
    private static final String DUMMY_NAME = "dummy";

    @Mock
    private SecretClient secretClient;
    @Mock
    private Map<String, IRedisCache<String, List<String>>> redisClientMap;
    @Mock
    private RedisAzureConfiguration redisConfiguration;
    @Mock
    private CoreLoggerFactory coreLoggerFactory;
    @Mock
    private CoreLogger coreLogger;
    @Mock
    private DefaultAzureCredential defaultAzureCredential;
    @Mock
    private MSIConfiguration msiConfiguration;
    @Mock
    private RedisCache<String, List<String>> mockRedisCache;
    @InjectMocks
    private RedisClientFactory<List<String>> redisClientFactory;

    @SuppressWarnings("unchecked")
    private static final Class<List<String>> LIST_STRING_CLASS = (Class<List<String>>) (Class<?>) List.class;
    private static final String DEFAULT_CACHE_KEY = String.format("%s-%s", List.class, "defaultCodec");

    /**
     * Helper method to create cache key for given codec class name.
     */
    private String createCacheKey(String codecClassName) {
        return String.format("%s-%s", List.class, codecClassName);
    }

    /**
     * Workaround for inability to mock static methods like getInstance().
     *
     * @param mock CoreLoggerFactory mock instance
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
        openMocks(this);
    }

    @Test
    public void should_return_cachedClient_when_cachedEarlier() {
        IRedisCache<String, List<String>> redisCache = createTypedMock(RedisCache.class);
        lenient().when(msiConfiguration.getIsEnabled()).thenReturn(false);
        setupCachedClient(DEFAULT_CACHE_KEY, redisCache);

        IRedisCache<String, List<String>> redisClient = this.redisClientFactory.getClient(LIST_STRING_CLASS, redisConfiguration, null);

        assertSame(redisCache, redisClient);
    }

    @Test
    public void should_return_NoOpRedisCache_when_computeIfAbsent_returns_null() {
        lenient().when(msiConfiguration.getIsEnabled()).thenReturn(false);
        when(this.redisClientMap.containsKey(DEFAULT_CACHE_KEY)).thenReturn(false);
        when(this.redisClientMap.computeIfAbsent(any(), any())).thenReturn(null);

        IRedisCache<String, List<String>> redisClient = this.redisClientFactory.getClient(LIST_STRING_CLASS, redisConfiguration, null);

        assertTrue(redisClient instanceof NoOpRedisCache);
    }

    @Test
    public void should_again_create_redisClient_if_cachedclient_instanceof_noOpRedisCache() {
        IRedisCache<String, List<String>> noOpRedisCache = createTypedMock(NoOpRedisCache.class);
        IRedisCache<String, List<String>> redisCache = createTypedMock(RedisCache.class);

        lenient().when(msiConfiguration.getIsEnabled()).thenReturn(false);
        try {
            setupLoggerMock();
            setupCachedClient(DEFAULT_CACHE_KEY, noOpRedisCache);
            when(this.redisClientMap.computeIfAbsent(any(), any())).thenReturn(redisCache);

            IRedisCache<String, List<String>> redisClient = this.redisClientFactory.getClient(LIST_STRING_CLASS, redisConfiguration, null);

            assertTrue(redisClient instanceof RedisCache);
        } finally {
            resetSingleton();
        }
    }

    @Test
    public void getRedissonClient_should_return_null_if_redis_secret_not_present() {
        when(redisConfiguration.getHostKey()).thenReturn(DUMMY_HOST);
        when(secretClient.getSecret(DUMMY_HOST)).thenThrow(new ResourceNotFoundException(SECRET_NOT_FOUND, null));
        try {
            setupLoggerMock();

            RedissonClient redissonClient = this.redisClientFactory.getRedissonClient(DUMMY_NAME, redisConfiguration);

            assertNull(redissonClient);
        } finally {
            resetSingleton();
        }
    }

    @Test
    public void should_keep_existing_client_when_msi_reauthentication_fails() {
        lenient().when(msiConfiguration.getIsEnabled()).thenReturn(false);
        setupCachedClient(DEFAULT_CACHE_KEY, mockRedisCache);

        IRedisCache<String, List<String>> result = redisClientFactory.getClient(LIST_STRING_CLASS, redisConfiguration, null);

        verifyNoMsiAuthentication();
        assertSame(mockRedisCache, result);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void should_not_refresh_client_when_msi_enabled_but_invalid_principal_id(String principalId) {
        lenient().when(msiConfiguration.getIsEnabled()).thenReturn(true);
        lenient().when(redisConfiguration.getPrincipalId()).thenReturn(principalId);
        setupCachedClient(DEFAULT_CACHE_KEY, mockRedisCache);

        IRedisCache<String, List<String>> result = redisClientFactory.getClient(LIST_STRING_CLASS, redisConfiguration, null);

        verifyNoMsiAuthentication();
        assertSame(mockRedisCache, result);
    }

    @Test
    public void should_cache_client_with_custom_codec_separately() {
        @SuppressWarnings("unchecked")
        io.lettuce.core.codec.RedisCodec<String, List<String>> customCodec = mock(io.lettuce.core.codec.RedisCodec.class);

        IRedisCache<String, List<String>> redisCache1 = createTypedMock(RedisCache.class);
        IRedisCache<String, List<String>> redisCache2 = createTypedMock(RedisCache.class);

        String customCodecCacheKey = createCacheKey(customCodec.getClass().getName());
        lenient().when(msiConfiguration.getIsEnabled()).thenReturn(true);

        setupCachedClient(DEFAULT_CACHE_KEY, redisCache1);
        setupCachedClient(customCodecCacheKey, redisCache2);

        IRedisCache<String, List<String>> clientWithDefaultCodec = this.redisClientFactory.getClient(LIST_STRING_CLASS, redisConfiguration, null);
        IRedisCache<String, List<String>> clientWithCustomCodec = this.redisClientFactory.getClient(LIST_STRING_CLASS, redisConfiguration, customCodec);

        assertSame(redisCache1, clientWithDefaultCodec);
        assertSame(redisCache2, clientWithCustomCodec);
        assertNotSame(clientWithDefaultCodec, clientWithCustomCodec);
    }

    @Test
    public void should_initialize_redis_client_map() throws Exception {
        // Create a new instance to test initialization
        RedisClientFactory<List<String>> newFactory = new RedisClientFactory<>();

        // Call the initialize method directly
        newFactory.initialize();

        // Verify the map is initialized by accessing it via reflection
        Field mapField = RedisClientFactory.class.getDeclaredField("redisClientMap");
        mapField.setAccessible(true);
        Map<?, ?> map = (Map<?, ?>) mapField.get(newFactory);

        assertNotNull(map);
        assertTrue(map instanceof ConcurrentHashMap);
        assertTrue(map.isEmpty());
    }

    @ParameterizedTest(name = "MSI enabled={0}, principalId={1}, isRedisson={2}")
    @CsvSource(nullValues = "NULL", value = {
            "false, principal-123, false",
            "true,  NULL,          false",
            "true,  ,              false",
            "false, principal-123, true",
            "true,  NULL,          true",
            "true,  ,              true"
    })
    public void should_throw_exception_when_direct_hostname_without_valid_msi(
            boolean isMsiEnabled,
            String principalId,
            boolean isRedissonClient) {
        
        when(redisConfiguration.getHostname()).thenReturn("redis.example.com");
        when(redisConfiguration.getPrincipalId()).thenReturn(principalId);
        when(msiConfiguration.getIsEnabled()).thenReturn(isMsiEnabled);
        
        if (isRedissonClient) {
            try {
                setupLoggerMock();

                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    redisClientFactory.getRedissonClient(DUMMY_NAME, redisConfiguration)
                );

                assertEquals("MSI authentication with valid principal ID is required when using direct hostname for Redis connection", exception.getMessage());
            } finally {
                resetSingleton();
            }
        } else {
            when(redisClientMap.containsKey(anyString())).thenReturn(false);
            when(redisClientMap.computeIfAbsent(anyString(), any())).thenAnswer(invocation -> {
                java.util.function.Function<String, IRedisCache<String, List<String>>> function = invocation.getArgument(1);
                return function.apply(invocation.getArgument(0));
            });

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                redisClientFactory.getClient(LIST_STRING_CLASS, redisConfiguration, null)
            );

            assertEquals("MSI authentication with valid principal ID is required when using direct hostname for Redis connection", exception.getMessage());
        }
    }

    @Test
    public void should_use_direct_hostname_when_provided_with_valid_msi() {
        String directHostname = "redis.example.com";
        String principalId = "principal-123";

        lenient().when(redisConfiguration.getHostname()).thenReturn(directHostname);
        lenient().when(redisConfiguration.getPrincipalId()).thenReturn(principalId);
        lenient().when(msiConfiguration.getIsEnabled()).thenReturn(true);
        setupCachedClient(DEFAULT_CACHE_KEY, mockRedisCache);

        IRedisCache<String, List<String>> result = redisClientFactory.getClient(LIST_STRING_CLASS, redisConfiguration, null);

        assertSame(mockRedisCache, result);
        // Verify that Key Vault was NOT accessed (direct hostname bypasses it)
        verify(secretClient, never()).getSecret(anyString());
    }

    @ParameterizedTest(name = "hostname={0}")
    @NullAndEmptySource
    public void should_use_akv_hostname_when_msi_enabled_and_no_direct_hostname(String directHostname) {
        String principalId = "principal-123";
        String akvHostname = "redis-akv.azure.com";

        setupMsiAuthScenario(directHostname, principalId, akvHostname);

        try {
            redisClientFactory.getClient(LIST_STRING_CLASS, redisConfiguration, null);

            // Verify AKV was accessed for hostname
            verify(secretClient).getSecret("redis-hostname");
            verify(secretClient, never()).getSecret("redis-password");
        } finally {
            resetSingleton();
        }
    }

    @ParameterizedTest(name = "MSI={0}, principalId={1} => uses password auth")
    @CsvSource(nullValues = "NULL", value = {
            "false, valid-principal",  // MSI disabled => password
            "false, NULL",              // MSI disabled + null principal => password
            "true,  NULL",              // MSI enabled + null principal => password fallback
            "true,  ''",                // MSI enabled + empty principal => password fallback
    })
    public void should_use_password_auth_from_akv(boolean msiEnabled, String principalId) {
        setupPasswordAuthScenario(msiEnabled, principalId);

        try {
            redisClientFactory.getClient(LIST_STRING_CLASS, redisConfiguration, null);

            // Verify both hostname and password were retrieved from AKV
            verify(secretClient).getSecret("redis-hostname");
            verify(secretClient).getSecret("redis-password");
        } finally {
            resetSingleton();
        }
    }

    @ParameterizedTest(name = "missing {0} => returns NoOpCache")
    @CsvSource({"hostname", "password"})
    public void should_return_noop_cache_when_required_secret_missing(String secretType) {
        setupRedisConfiguration(null, null, false);
        setupComputeIfAbsentMock();
        
        // Setup: hostname exists but password missing, or vice versa
        if (secretType.equals("hostname")) {
            lenient().when(secretClient.getSecret("redis-hostname"))
                .thenThrow(new ResourceNotFoundException(SECRET_NOT_FOUND, null));
        } else {
            var hostnameSecret = mockKeyVaultSecret("redis.azure.com");
            lenient().when(secretClient.getSecret("redis-hostname")).thenReturn(hostnameSecret);
            lenient().when(secretClient.getSecret("redis-password"))
                .thenThrow(new ResourceNotFoundException(SECRET_NOT_FOUND, null));
        }
        try {
            setupLoggerMock();

            IRedisCache<String, List<String>> result = redisClientFactory.getClient(LIST_STRING_CLASS, redisConfiguration, null);

            assertNotNull(result);
            assertTrue(result instanceof NoOpRedisCache);
        } finally {
            resetSingleton();
        }
    }

    @ParameterizedTest(name = "msiEnabled={0}, directHostname={1}, principalId={2} => {3}")
    @CsvSource(nullValues = "NULL", value = {
            "true,  NULL, principal-123, verifies AKV access with MSI",
            "false, NULL, NULL,          verifies AKV access with password auth",
            "true,  redis.example.com, principal-123, skips AKV with direct hostname"
    })
    public void should_handle_redisson_akv_access_scenarios(
            boolean msiEnabled,
            String directHostname,
            String principalId,
            String description) {

        setupRedisConfiguration(directHostname, principalId, msiEnabled);
        boolean isDirect = directHostname != null && !directHostname.isEmpty();

        if (!isDirect) {
            // For AKV scenarios, make secret retrieval fail to avoid real connections
            lenient().when(secretClient.getSecret("redis-hostname"))
                .thenThrow(new ResourceNotFoundException(SECRET_NOT_FOUND, null));
            try {
                setupLoggerMock();

                RedissonClient result = redisClientFactory.getRedissonClient(DUMMY_NAME, redisConfiguration);

                assertNull(result);
                verify(secretClient).getSecret("redis-hostname");
                if (msiEnabled) {
                    verify(secretClient, never()).getSecret("redis-password");
                }
            } finally {
                resetSingleton();
            }
        } else {
            // Direct hostname case - verify no AKV access
            verify(secretClient, never()).getSecret(anyString());
        }
    }

    @ParameterizedTest(name = "missing {0} => returns null")
    @CsvSource({"hostname", "password"})
    public void should_return_null_when_redisson_secret_missing(String secretType) {
        lenient().when(redisConfiguration.getHostname()).thenReturn(null);
        lenient().when(redisConfiguration.getHostKey()).thenReturn("redis-hostname");
        lenient().when(redisConfiguration.getPasswordKey()).thenReturn("redis-password");
        lenient().when(msiConfiguration.getIsEnabled()).thenReturn(false);

        // Setup: hostname exists but password missing, or vice versa
        if (secretType.equals("hostname")) {
            lenient().when(secretClient.getSecret("redis-hostname")).thenThrow(new ResourceNotFoundException(SECRET_NOT_FOUND, null));
        } else {
            var hostnameSecret = mockKeyVaultSecret("redis.azure.com");
            lenient().when(secretClient.getSecret("redis-hostname")).thenReturn(hostnameSecret);
            lenient().when(secretClient.getSecret("redis-password")).thenThrow(new ResourceNotFoundException(SECRET_NOT_FOUND, null));
        }
        try {
            setupLoggerMock();

            RedissonClient result = redisClientFactory.getRedissonClient(DUMMY_NAME, redisConfiguration);

            assertNull(result);
        } finally {
            resetSingleton();
        }
    }

    /**
     * Helper method to setup password authentication scenario with common configuration.
     */
    private void setupPasswordAuthScenario(boolean msiEnabled, String principalId) {
        setupRedisConfiguration(null, principalId, msiEnabled);
        setupComputeIfAbsentMock();
        
        var hostnameSecret = mockKeyVaultSecret("redis-akv.azure.com");
        var passwordSecret = mockKeyVaultSecret("test-password");
        lenient().when(secretClient.getSecret("redis-hostname")).thenReturn(hostnameSecret);
        lenient().when(secretClient.getSecret("redis-password")).thenReturn(passwordSecret);
        setupLoggerMock();
    }

    /**
     * Helper method to setup MSI authentication scenario for AKV hostname fallback.
     */
    private void setupMsiAuthScenario(String directHostname, String principalId, String akvHostname) {
        setupRedisConfiguration(directHostname, principalId, true);
        setupComputeIfAbsentMock();
        
        var hostnameSecret = mockKeyVaultSecret(akvHostname);
        lenient().when(secretClient.getSecret("redis-hostname")).thenReturn(hostnameSecret);
        setupLoggerMock();
    }

    /**
     * Helper method to setup common Redis configuration mocks.
     */
    private void setupRedisConfiguration(String hostname, String principalId, boolean msiEnabled) {
        lenient().when(redisConfiguration.getHostname()).thenReturn(hostname);
        lenient().when(redisConfiguration.getPrincipalId()).thenReturn(principalId);
        lenient().when(redisConfiguration.getHostKey()).thenReturn("redis-hostname");
        lenient().when(redisConfiguration.getPasswordKey()).thenReturn("redis-password");
        lenient().when(redisConfiguration.getPort()).thenReturn(6380);
        lenient().when(redisConfiguration.getDatabase()).thenReturn(0);
        lenient().when(redisConfiguration.getExpiration()).thenReturn(3600);
        lenient().when(redisConfiguration.getConnectionTimeout()).thenReturn(10L);
        lenient().when(redisConfiguration.getCommandTimeout()).thenReturn(5);
        lenient().when(msiConfiguration.getIsEnabled()).thenReturn(msiEnabled);
    }

    /**
     * Helper method to setup computeIfAbsent mock that invokes the function.
     */
    private void setupComputeIfAbsentMock() {
        lenient().when(redisClientMap.containsKey(anyString())).thenReturn(false);
        lenient().when(redisClientMap.computeIfAbsent(anyString(), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Function<String, IRedisCache<String, List<String>>> function = invocation.getArgument(1);
            try {
                return function.apply(invocation.getArgument(0));
            } catch (Exception e) {
                // Catch Redis connection exceptions that occur in unit tests
                return null;
            }
        });
    }

    /**
     * Helper method to create type-safe mocks without warnings.
     */
    @SuppressWarnings("unchecked")
    private <T> T createTypedMock(Class<?> classToMock) {
        return (T) mock(classToMock);
    }

    /**
     * Helper method to setup a cached client in the mock map.
     */
    private void setupCachedClient(String cacheKey, IRedisCache<String, List<String>> cache) {
        when(redisClientMap.containsKey(cacheKey)).thenReturn(true);
        when(redisClientMap.get(cacheKey)).thenReturn(cache);
    }

    /**
     * Helper method to verify no MSI authentication attempts were made.
     */
    private void verifyNoMsiAuthentication() {
        verify(defaultAzureCredential, never()).getToken(any(TokenRequestContext.class));
        verify(mockRedisCache, never()).authenticate(anyString(), anyString());
    }

    /**
     * Helper method to setup logger singleton mock.
     */
    private void setupLoggerMock() {
        mockSingleton(coreLoggerFactory);
        when(coreLoggerFactory.getLogger(anyString())).thenReturn(coreLogger);
    }

    /**
     * Helper method to mock KeyVault secret responses.
     */
    private com.azure.security.keyvault.secrets.models.KeyVaultSecret mockKeyVaultSecret(String value) {
        com.azure.security.keyvault.secrets.models.KeyVaultSecret secret = 
            mock(com.azure.security.keyvault.secrets.models.KeyVaultSecret.class);
        lenient().when(secret.getValue()).thenReturn(value);
        return secret;
    }
}
