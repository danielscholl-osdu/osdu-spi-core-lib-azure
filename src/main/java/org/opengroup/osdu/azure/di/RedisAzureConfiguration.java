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
    private String objectId;

    /**
     * Constructor with default hostKey and passwordKey.
     * @param pDatabase database
     * @param pExpiration expiration
     * @param pPort port
     * @param pConnectionTimeout connectionTimeout
     * @param pCommandTimeout commandTimeout
     */
    public RedisAzureConfiguration(final int pDatabase, final int pExpiration, final int pPort, final long pConnectionTimeout, final int pCommandTimeout) {
        this(pDatabase, pExpiration, pPort, pConnectionTimeout, pCommandTimeout, "redis-hostname", "redis-password", null);
    }

    /**
     * Constructor with default hostKey and custom objectId, but null passwordKey (for MSI authentication).
     * @param pDatabase database
     * @param pExpiration expiration
     * @param pPort port
     * @param pConnectionTimeout connectionTimeout
     * @param pCommandTimeout commandTimeout
     * @param pObjectId objectId for Redis MSI authentication
     */
    public RedisAzureConfiguration(final int pDatabase, final int pExpiration, final int pPort, final long pConnectionTimeout, final int pCommandTimeout, final String pObjectId) {
        this(pDatabase, pExpiration, pPort, pConnectionTimeout, pCommandTimeout, "redis-hostname", null, pObjectId);
    }

}
