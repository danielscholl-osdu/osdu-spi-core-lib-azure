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

package org.opengroup.osdu.azure;

import com.azure.core.exception.ResourceNotFoundException;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.azure.logging.DependencyPayload;
import org.opengroup.osdu.common.Validators;

import java.time.Duration;

/**
 * A simpler interface for interacting with keyVault.
 * <p>
 * Usage Examples:
 * <pre>
 * {@code
 *     @Bean
 *     @Named("MY_STRING_SECRET")
 *     public String cosmosKey(SecretClient kv) {
 *         return KeyVaultFacade.getSecretWithValidation(kv, "my-secret-name");
 *     }
 * }
 * </pre>
 */
public final class KeyVaultFacade {
    private static final String LOGGER_NAME = KeyVaultFacade.class.getName();

    /**
     * Private constructor -- this class should never be instantiated.
     */
    private KeyVaultFacade() {
    }

    /**
     * @param kv         Client configured to the correct vault
     * @param secretName Name of secret to return
     * @return A guaranteed to be non null secret value
     */
    public static String getSecretWithValidation(final SecretClient kv, final String secretName) {
        String secretValue = getSecretWithDefault(kv, secretName, null);
        Validators.checkNotNullAndNotEmpty(secretValue, secretName);

        return secretValue;
    }

    /**
     * Get the secret with a default value. If the secret is not found or is null return the default value.
     *
     * @param kv           Client configured to the correct vault
     * @param secretName   name of secret
     * @param defaultValue to be used in case the secret is null or empty.
     * @return Secret value. It is guaranteed to be returned with either default value or a non null, non empty secret.
     */
    public static String getSecretWithDefault(final SecretClient kv, final String secretName, final String defaultValue) {
        Validators.checkNotNull(secretName, "Secret with name " + secretName);

        KeyVaultSecret secret;
        final long start = System.currentTimeMillis();
        int statusCode = HttpStatus.SC_OK;
        try {
            secret = kv.getSecret(secretName);
            CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("Successfully retrieved {}.", secretName);
            if (secret == null || secret.getValue() == null || secret.getValue().isEmpty()) {
                CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("Value for {} is empty.", secretName);
                return defaultValue;
            }
        } catch (ResourceNotFoundException secretNotFound) {
            statusCode = HttpStatus.SC_NOT_FOUND;
            CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).warn("Failed to retrieve {}. Not found.", secretName);
            return defaultValue;
        } finally {
            final long timeTaken = System.currentTimeMillis() - start;
            logDependency("GET_SECRET", secretName, kv.getVaultUrl(), timeTaken, statusCode);
        }
        return secret.getValue();
    }

    /**
     * Log dependency.
     *
     * @param name          the name of the command initiated with this dependency call
     * @param data          the command initiated by this dependency call
     * @param target        the target of this dependency call
     * @param timeTakenInMs the request duration in milliseconds
     * @param resultCode    the result code of the call
     */
    private static void logDependency(final String name, final String data, final String target, final long timeTakenInMs, final int resultCode) {
        DependencyPayload payload = new DependencyPayload(name, data, Duration.ofMillis(timeTakenInMs), String.valueOf(resultCode), resultCode == HttpStatus.SC_OK);
        payload.setType("KeyVault");
        payload.setTarget(target);

        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).logDependency(payload);
    }
}
