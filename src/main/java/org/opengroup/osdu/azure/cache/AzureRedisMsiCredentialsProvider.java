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
import io.lettuce.core.RedisCredentialsProvider;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.azure.logging.ICoreLogger;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/**
 * Azure Redis credentials provider for MSI (Managed Service Identity) authentication with Lettuce.
 * Automatically provides fresh MSI tokens when Lettuce detects authentication failures.
 * This implementation mirrors RedissonMsiCredentialsResolver for consistency across Redis clients.
 *
 * <p>Lettuce 6.6.0+ introduced RedisCredentialsProvider interface.
 * Lettuce calls resolveCredentials() on each new connection.
 * When an expired token causes an AUTH error, Lettuce auto-reconnects and obtains a fresh token.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Automatic token refresh on AUTH failures (no scheduled refresh needed)</li>
 *   <li>Thread-safe token acquisition using Azure DefaultAzureCredential</li>
 *   <li>Reactive programming model using Project Reactor's Mono</li>
 *   <li>Blocking IMDS call offloaded to boundedElastic scheduler to protect Lettuce's Netty I/O threads</li>
 *   <li>10-second timeout on IMDS calls to prevent indefinite blocking under throttling</li>
 * </ul>
 *
 * <p>Note: token caching is handled internally by {@code DefaultAzureCredential} via MSAL4J's
 * {@code ManagedIdentityApplication.acquireTokenForManagedIdentity()}, which maintains a
 * thread-safe {@code TokenCache} and only calls IMDS when the cached token is within
 * its refresh buffer (~2 minutes before expiry). No additional caching layer is needed here.</p>
 */
public class AzureRedisMsiCredentialsProvider implements RedisCredentialsProvider {
    private static final String LOGGER_NAME = AzureRedisMsiCredentialsProvider.class.getName();
    private static final String AZURE_REDIS_SCOPE = "https://redis.azure.com/.default";

    /** Maximum time to wait for a token from IMDS before giving up. */
    private static final Duration IMDS_TIMEOUT = Duration.ofSeconds(10);

    private final String principalId;
    private final DefaultAzureCredential credential;

    /**
     * Constructor for AzureRedisMsiCredentialsProvider.
     *
     * @param msiPrincipalId the Principal ID for MSI authentication from REDIS_PRINCIPAL_ID environment variable
     * @param azureCredential the Azure credential for MSI token acquisition
     */
    public AzureRedisMsiCredentialsProvider(final String msiPrincipalId, final DefaultAzureCredential azureCredential) {
        this.principalId = msiPrincipalId;
        this.credential = azureCredential;
    }

    /**
     * Resolves Redis credentials by obtaining an MSI token from Azure.
     * {@code DefaultAzureCredential} (via MSAL4J) handles token caching internally —
     * it only calls IMDS when the cached token is near expiry.
     *
     * <p>The blocking IMDS call is offloaded to {@code Schedulers.boundedElastic()} so it
     * never runs on a Lettuce Netty I/O thread, which would freeze all connections on that
     * event loop. A 10-second timeout guards against IMDS being unresponsive under load.</p>
     *
     * @return Mono containing RedisCredentials with username (Principal ID) and password (access token)
     */
    @Override
    public Mono<RedisCredentials> resolveCredentials() {
        return Mono.fromCallable(() -> {
            final ICoreLogger logger = CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME);

            try {
                TokenRequestContext tokenRequestContext = new TokenRequestContext();
                tokenRequestContext.addScopes(AZURE_REDIS_SCOPE);

                AccessToken accessToken = credential.getToken(tokenRequestContext)
                        .block(IMDS_TIMEOUT);

                if (accessToken == null || accessToken.getToken() == null) {
                    logger.error("Failed to acquire access token from MSI");
                    throw new RuntimeException("Failed to acquire access token from MSI");
                }

                logger.debug("AzureRedisMsiCredentialsProvider providing fresh credentials. Token expires at: {}", accessToken.getExpiresAt());

                return RedisCredentials.just(principalId, accessToken.getToken());

            } catch (Exception e) {
                logger.error("AzureRedisMsiCredentialsProvider error resolving credentials", e);
                throw new RuntimeException("Failed to resolve MSI credentials", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()); // CRITICAL: offload blocking IMDS call from Lettuce's Netty I/O thread
    }
}
