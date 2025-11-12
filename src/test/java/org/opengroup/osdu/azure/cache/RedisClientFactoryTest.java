package org.opengroup.osdu.azure.cache;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.identity.DefaultAzureCredential;
import org.junit.jupiter.api.*;
import org.opengroup.osdu.azure.di.MSIConfiguration;
import org.opengroup.osdu.azure.logging.CoreLogger;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.core.common.cache.RedisCache;
import com.azure.security.keyvault.secrets.SecretClient;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.di.RedisAzureConfiguration;
import org.opengroup.osdu.core.common.cache.IRedisCache;
import org.redisson.api.RedissonClient;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

@ExtendWith(MockitoExtension.class)
public class RedisClientFactoryTest {

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
        final String cacheKey = String.format("%s-%s", String.class, List.class);
        when(msiConfiguration.getIsEnabled()).thenReturn(false);
        when(this.redisClientMap.containsKey(cacheKey)).thenReturn(true);
        when(this.redisClientMap.get(cacheKey)).thenReturn(redisCache);

        IRedisCache<String, List<String>> redisClient = this.redisClientFactory.getClient(String.class, LIST_STRING_CLASS, redisConfiguration);

        assertSame(redisCache, redisClient);
    }

    @Test
    public void should_return_NoOpRedisCache_when_computeIfAbsent_returns_null() {
        final String cacheKey = String.format("%s-%s", String.class, List.class);
        when(msiConfiguration.getIsEnabled()).thenReturn(false);
        when(this.redisClientMap.containsKey(cacheKey)).thenReturn(false);
        when(this.redisClientMap.computeIfAbsent(any(), any())).thenReturn(null);

        IRedisCache<String, List<String>> redisClient = this.redisClientFactory.getClient(String.class, LIST_STRING_CLASS, redisConfiguration);

        assertTrue(redisClient instanceof NoOpRedisCache);
    }

    @Test
    public void should_again_create_redisClient_if_cachedclient_instanceof_noOpRedisCache() {
        IRedisCache<String, List<String>> noOpRedisCache = createTypedMock(NoOpRedisCache.class);
        IRedisCache<String, List<String>> redisCache = createTypedMock(RedisCache.class);
        final String cacheKey = String.format("%s-%s", String.class, List.class);

        when(msiConfiguration.getIsEnabled()).thenReturn(false);
        mockSingleton(coreLoggerFactory);
        when(coreLoggerFactory.getLogger(anyString())).thenReturn(coreLogger);

        when(this.redisClientMap.containsKey(cacheKey)).thenReturn(true);
        when(this.redisClientMap.get(cacheKey)).thenReturn(noOpRedisCache);
        when(this.redisClientMap.computeIfAbsent(any(), any())).thenReturn(redisCache);

        IRedisCache<String, List<String>> redisClient = this.redisClientFactory.getClient(String.class, LIST_STRING_CLASS, redisConfiguration);
        resetSingleton();

        assertTrue(redisClient instanceof RedisCache);
    }

    @Test
    public void getRedissonClient_should_return_null_if_redis_secret_not_present() {
        when(redisConfiguration.getHostKey()).thenReturn("dummyHost");
        when(secretClient.getSecret("dummyHost")).thenThrow(new ResourceNotFoundException("SecretDoesNotExists", null));
        mockSingleton(coreLoggerFactory);
        when(coreLoggerFactory.getLogger(anyString())).thenReturn(coreLogger);

        RedissonClient redissonClient = this.redisClientFactory.getRedissonClient("dummy", redisConfiguration);

        assertNull(redissonClient);
    }

    @Test
    public void should_reauthenticate_lettuce_client_when_msi_enabled_and_token_expired() throws Exception {
        // Setup MSI configuration
        when(msiConfiguration.getIsEnabled()).thenReturn(true);
        when(redisConfiguration.getObjectId()).thenReturn("test-object-id");
        
        // Mock existing client in cache
        final String cacheKey = String.format("%s-%s", String.class, List.class);
        when(redisClientMap.get(cacheKey)).thenReturn(mockRedisCache);
        
        // Setup authentication time tracking to simulate expired token
        Map<String, Instant> authTimes = new ConcurrentHashMap<>();
        authTimes.put(cacheKey, Instant.now().minusSeconds(2000)); // 30+ minutes ago
        setPrivateField("redisClientAuthenticationTimes", authTimes);
        
        // Mock MSI token acquisition
        AccessToken accessToken = new AccessToken("mock-token", OffsetDateTime.now().plusHours(1));
        when(defaultAzureCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(accessToken));
        
        // Mock successful re-authentication
        when(mockRedisCache.authenticate("test-object-id", "mock-token")).thenReturn(true);
        
        mockSingleton(coreLoggerFactory);
        when(coreLoggerFactory.getLogger(anyString())).thenReturn(coreLogger);

        IRedisCache<String, List<String>> result = redisClientFactory.getClient(String.class, LIST_STRING_CLASS, redisConfiguration);
        
        resetSingleton();
        
        // Verify re-authentication was attempted
        verify(mockRedisCache).authenticate("test-object-id", "mock-token");
        assertSame(mockRedisCache, result);
    }

    @Test
    public void should_remove_cached_client_when_msi_reauthentication_fails() throws Exception {
        // Setup MSI configuration
        when(msiConfiguration.getIsEnabled()).thenReturn(true);
        when(redisConfiguration.getObjectId()).thenReturn("test-object-id");
        
        // Mock existing client in cache
        final String cacheKey = String.format("%s-%s", String.class, List.class);
        when(redisClientMap.containsKey(cacheKey)).thenReturn(true);
        when(redisClientMap.get(cacheKey)).thenReturn(mockRedisCache);
        
        // Setup authentication time tracking to simulate expired token
        Map<String, Instant> authTimes = new ConcurrentHashMap<>();
        authTimes.put(cacheKey, Instant.now().minusSeconds(2000)); // 30+ minutes ago
        setPrivateField("redisClientAuthenticationTimes", authTimes);
        
        // Mock MSI token acquisition
        AccessToken accessToken = new AccessToken("mock-token", OffsetDateTime.now().plusHours(1));
        when(defaultAzureCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(accessToken));
        
        // Mock failed re-authentication
        when(mockRedisCache.authenticate("test-object-id", "mock-token")).thenReturn(false);
        
        mockSingleton(coreLoggerFactory);
        when(coreLoggerFactory.getLogger(anyString())).thenReturn(coreLogger);

        redisClientFactory.getClient(String.class, LIST_STRING_CLASS, redisConfiguration);
        
        resetSingleton();
        
        // Verify cached client was removed due to auth failure
        verify(redisClientMap).remove(cacheKey);
    }

    @Test
    public void should_handle_msi_token_acquisition_failure_gracefully() throws Exception {
        // Setup MSI configuration
        when(msiConfiguration.getIsEnabled()).thenReturn(true);
        when(redisConfiguration.getObjectId()).thenReturn("test-object-id");
        
        // Mock existing client in cache
        final String cacheKey = String.format("%s-%s", String.class, List.class);
        when(redisClientMap.get(cacheKey)).thenReturn(mockRedisCache);
        
        // Setup authentication time tracking to simulate expired token
        Map<String, Instant> authTimes = new ConcurrentHashMap<>();
        authTimes.put(cacheKey, Instant.now().minusSeconds(2000)); // 30+ minutes ago
        setPrivateField("redisClientAuthenticationTimes", authTimes);
        
        // Mock MSI token acquisition failure
        when(defaultAzureCredential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.empty());
        
        mockSingleton(coreLoggerFactory);
        when(coreLoggerFactory.getLogger(anyString())).thenReturn(coreLogger);

        redisClientFactory.getClient(String.class, LIST_STRING_CLASS, redisConfiguration);
        
        resetSingleton();
        
        // Verify cached client was removed due to token acquisition failure
        verify(redisClientMap).remove(cacheKey);
        verify(mockRedisCache, never()).authenticate(anyString(), anyString());
    }

    @Test 
    public void should_not_refresh_client_when_msi_disabled() {
        // Setup non-MSI configuration
        when(msiConfiguration.getIsEnabled()).thenReturn(false);
        
        // Mock existing client in cache - use RedisCache directly to pass instanceof check
        final String cacheKey = String.format("%s-%s", String.class, List.class);
        when(redisClientMap.containsKey(cacheKey)).thenReturn(true);
        when(redisClientMap.get(cacheKey)).thenReturn(mockRedisCache);

        IRedisCache<String, List<String>> result = redisClientFactory.getClient(String.class, LIST_STRING_CLASS, redisConfiguration);
        
        // Verify no MSI authentication attempts were made
        verify(defaultAzureCredential, never()).getToken(any(TokenRequestContext.class));
        verify(mockRedisCache, never()).authenticate(anyString(), anyString());
        assertSame(mockRedisCache, result);
    }

    @Test
    public void should_not_refresh_client_when_msi_enabled_but_no_object_id() {
        // Setup MSI enabled but no object ID (fallback scenario)
        when(msiConfiguration.getIsEnabled()).thenReturn(true);
        when(redisConfiguration.getObjectId()).thenReturn(null); // No object ID provided
        
        // Mock existing client in cache
        final String cacheKey = String.format("%s-%s", String.class, List.class);
        when(redisClientMap.containsKey(cacheKey)).thenReturn(true);
        when(redisClientMap.get(cacheKey)).thenReturn(mockRedisCache);

        IRedisCache<String, List<String>> result = redisClientFactory.getClient(String.class, LIST_STRING_CLASS, redisConfiguration);
        
        // Verify no MSI authentication attempts were made (should fallback to password auth)
        verify(defaultAzureCredential, never()).getToken(any(TokenRequestContext.class));
        verify(mockRedisCache, never()).authenticate(anyString(), anyString());
        assertSame(mockRedisCache, result);
    }

    @Test
    public void should_not_refresh_client_when_msi_enabled_but_empty_object_id() {
        // Setup MSI enabled but empty object ID (fallback scenario)
        when(msiConfiguration.getIsEnabled()).thenReturn(true);
        when(redisConfiguration.getObjectId()).thenReturn(""); // Empty object ID
        
        // Mock existing client in cache
        final String cacheKey = String.format("%s-%s", String.class, List.class);
        when(redisClientMap.containsKey(cacheKey)).thenReturn(true);
        when(redisClientMap.get(cacheKey)).thenReturn(mockRedisCache);

        IRedisCache<String, List<String>> result = redisClientFactory.getClient(String.class, LIST_STRING_CLASS, redisConfiguration);
        
        // Verify no MSI authentication attempts were made (should fallback to password auth)
        verify(defaultAzureCredential, never()).getToken(any(TokenRequestContext.class));
        verify(mockRedisCache, never()).authenticate(anyString(), anyString());
        assertSame(mockRedisCache, result);
    }

    /**
     * Helper method to set private fields for testing.
     */
    private void setPrivateField(String fieldName, Object value) throws Exception {
        Field field = RedisClientFactory.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(redisClientFactory, value);
        field.setAccessible(false);
    }

    /**
     * Helper method to create type-safe mocks without warnings.
     */
    @SuppressWarnings("unchecked")
    private <T> T createTypedMock(Class<?> classToMock) {
        return (T) mock(classToMock);
    }

}
