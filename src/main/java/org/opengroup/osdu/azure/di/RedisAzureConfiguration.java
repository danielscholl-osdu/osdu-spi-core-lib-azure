package org.opengroup.osdu.azure.di;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * A configuration bean class to set up redis cache related variables.
 */
@ConfigurationProperties
@AllArgsConstructor
@Getter
public class RedisAzureConfiguration {
    private int database;
    private int expiration;
    private int port;
    private long connectionTimeout = 10;
    private int commandTimeout = 5;
    private String hostKey;
    private String passwordKey;
    private String principalId;
    private String hostname;

    /**
     * Constructor with default hostKey and passwordKey.
     * @param pDatabase database
     * @param pExpiration expiration
     * @param pPort port
     * @param pConnectionTimeout connectionTimeout
     * @param pCommandTimeout commandTimeout
     */
    public RedisAzureConfiguration(final int pDatabase, final int pExpiration, final int pPort, final long pConnectionTimeout, final int pCommandTimeout) {
        this(pDatabase, pExpiration, pPort, pConnectionTimeout, pCommandTimeout, "redis-hostname", "redis-password", null, null);
    }

    /**
     * Constructor with default hostKey and passwordKey, but custom principalId, if valid, and MSI is enabled, redis connection will be made with MSI authentication.
     * @param pDatabase database
     * @param pExpiration expiration
     * @param pPort port
     * @param pConnectionTimeout connectionTimeout
     * @param pCommandTimeout commandTimeout
     * @param pPrincipalId principalId for Redis MSI authentication
     */
    public RedisAzureConfiguration(final int pDatabase, final int pExpiration, final int pPort, final long pConnectionTimeout, final int pCommandTimeout, final String pPrincipalId) {
        this(pDatabase, pExpiration, pPort, pConnectionTimeout, pCommandTimeout, "redis-hostname", "redis-password", pPrincipalId, null);
    }

    /**
     * Constructor with direct hostname instead of hostKey, using MSI authentication.
     * @param pDatabase database
     * @param pExpiration expiration
     * @param pPort port
     * @param pConnectionTimeout connectionTimeout
     * @param pCommandTimeout commandTimeout
     * @param pHostname direct hostname for Redis connection
     * @param pPrincipalId principalId for Redis MSI authentication (required)
     */
    public RedisAzureConfiguration(final int pDatabase, final int pExpiration, final int pPort, final long pConnectionTimeout, final int pCommandTimeout, final String pPrincipalId, final String pHostname) {
        this(pDatabase, pExpiration, pPort, pConnectionTimeout, pCommandTimeout, "redis-hostname", "redis-password", pPrincipalId, pHostname);
    }
}
