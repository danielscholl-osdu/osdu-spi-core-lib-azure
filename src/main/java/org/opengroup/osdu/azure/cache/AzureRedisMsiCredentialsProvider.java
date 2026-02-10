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
 * </ul>
 */
public class AzureRedisMsiCredentialsProvider implements RedisCredentialsProvider {
    private static final String LOGGER_NAME = AzureRedisMsiCredentialsProvider.class.getName();
    private static final String AZURE_REDIS_SCOPE = "https://redis.azure.com/.default";

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
     * Resolves Redis credentials by obtaining a fresh MSI token from Azure.
     * Called automatically by Lettuce when authentication is needed or when AUTH errors occur.
     *
     * @return Mono containing RedisCredentials with username (Principal ID) and password (access token)
     */
    @Override
    public Mono<RedisCredentials> resolveCredentials() {
        return Mono.fromCallable(() -> {
            final ICoreLogger logger = CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME);

            try {
                // Get fresh MSI token from Azure
                TokenRequestContext tokenRequestContext = new TokenRequestContext();
                tokenRequestContext.addScopes(AZURE_REDIS_SCOPE);

                AccessToken accessToken = credential.getToken(tokenRequestContext).block();
                if (accessToken == null || accessToken.getToken() == null) {
                    logger.error("Failed to acquire access token from MSI");
                    throw new RuntimeException("Failed to acquire access token from MSI");
                }

                String token = accessToken.getToken();

                logger.info("AzureRedisMsiCredentialsProvider providing fresh credentials. Token expires at: {}", accessToken.getExpiresAt());

                return RedisCredentials.just(principalId, token);

            } catch (Exception e) {
                logger.error("AzureRedisMsiCredentialsProvider error resolving credentials", e);
                throw new RuntimeException("Failed to resolve MSI credentials", e);
            }
        });
    }
}
