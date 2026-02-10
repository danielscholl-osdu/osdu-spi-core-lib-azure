package org.opengroup.osdu.azure.cache;

import com.azure.core.credential.TokenRequestContext;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.identity.DefaultAzureCredential;
import com.azure.security.keyvault.secrets.SecretClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
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
    private RedisClientFactory<String, List<String>> redisClientFactory;

    @SuppressWarnings("unchecked")
    private static final Class<List<String>> LIST_STRING_CLASS = (Class<List<String>>) (Class<?>) List.class;
    private static final String DEFAULT_CACHE_KEY = String.format("%s-%s-%s", String.class, List.class, "defaultCodec");

    /**
     * Helper method to create cache key for given codec class name.
     */
    private String createCacheKey(String codecClassName) {
        return String.format("%s-%s-%s", String.class, List.class, codecClassName);
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

        IRedisCache<String, List<String>> redisClient = this.redisClientFactory.getClient(String.class, LIST_STRING_CLASS, redisConfiguration, null);

        assertSame(redisCache, redisClient);
    }

    @Test
    public void should_return_NoOpRedisCache_when_computeIfAbsent_returns_null() {
        lenient().when(msiConfiguration.getIsEnabled()).thenReturn(false);
        when(this.redisClientMap.containsKey(DEFAULT_CACHE_KEY)).thenReturn(false);
        when(this.redisClientMap.computeIfAbsent(any(), any())).thenReturn(null);

        IRedisCache<String, List<String>> redisClient = this.redisClientFactory.getClient(String.class, LIST_STRING_CLASS, redisConfiguration, null);

        assertTrue(redisClient instanceof NoOpRedisCache);
    }

    @Test
    public void should_again_create_redisClient_if_cachedclient_instanceof_noOpRedisCache() {
        IRedisCache<String, List<String>> noOpRedisCache = createTypedMock(NoOpRedisCache.class);
        IRedisCache<String, List<String>> redisCache = createTypedMock(RedisCache.class);

        lenient().when(msiConfiguration.getIsEnabled()).thenReturn(false);
        setupLoggerMock();
        setupCachedClient(DEFAULT_CACHE_KEY, noOpRedisCache);
        when(this.redisClientMap.computeIfAbsent(any(), any())).thenReturn(redisCache);

        IRedisCache<String, List<String>> redisClient = this.redisClientFactory.getClient(String.class, LIST_STRING_CLASS, redisConfiguration, null);
        resetSingleton();

        assertTrue(redisClient instanceof RedisCache);
    }

    @Test
    public void getRedissonClient_should_return_null_if_redis_secret_not_present() {
        when(redisConfiguration.getHostKey()).thenReturn(DUMMY_HOST);
        when(secretClient.getSecret(DUMMY_HOST)).thenThrow(new ResourceNotFoundException(SECRET_NOT_FOUND, null));
        setupLoggerMock();

        RedissonClient redissonClient = this.redisClientFactory.getRedissonClient(DUMMY_NAME, redisConfiguration);

        assertNull(redissonClient);
    }

    @Test
    public void should_keep_existing_client_when_msi_reauthentication_fails() {
        lenient().when(msiConfiguration.getIsEnabled()).thenReturn(false);
        setupCachedClient(DEFAULT_CACHE_KEY, mockRedisCache);

        IRedisCache<String, List<String>> result = redisClientFactory.getClient(String.class, LIST_STRING_CLASS, redisConfiguration, null);

        verifyNoMsiAuthentication();
        assertSame(mockRedisCache, result);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void should_not_refresh_client_when_msi_enabled_but_invalid_principal_id(String principalId) {
        lenient().when(msiConfiguration.getIsEnabled()).thenReturn(true);
        lenient().when(redisConfiguration.getPrincipalId()).thenReturn(principalId);
        setupCachedClient(DEFAULT_CACHE_KEY, mockRedisCache);

        IRedisCache<String, List<String>> result = redisClientFactory.getClient(String.class, LIST_STRING_CLASS, redisConfiguration, null);

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

        IRedisCache<String, List<String>> clientWithDefaultCodec =
            this.redisClientFactory.getClient(String.class, LIST_STRING_CLASS, redisConfiguration, null);
        IRedisCache<String, List<String>> clientWithCustomCodec =
            this.redisClientFactory.getClient(String.class, LIST_STRING_CLASS, redisConfiguration, customCodec);

        assertSame(redisCache1, clientWithDefaultCodec);
        assertSame(redisCache2, clientWithCustomCodec);
        assertNotSame(clientWithDefaultCodec, clientWithCustomCodec);
    }

    @Test
    public void should_initialize_redis_client_map() throws Exception {
        // Create a new instance to test initialization
        RedisClientFactory<String, List<String>> newFactory = new RedisClientFactory<>();

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
}
