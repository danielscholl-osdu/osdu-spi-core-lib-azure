package org.opengroup.osdu.azure.cache;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Implementation of ICache for AzureServicePrincipleTokenService.
 */
@Component
@Lazy
public class AzureServicePrincipleTokenServiceCache extends VmCache<String, Object> {
    private static final int CACHE_TTL_SECONDS = 75 * 60;
    private static final int MAX_CACHE_SIZE = 1;

    /**
     *  Default cache constructor.
     */
    public AzureServicePrincipleTokenServiceCache() {
        super(CACHE_TTL_SECONDS, MAX_CACHE_SIZE);
    }

    /**
     * @param key cache key
     * @return true if found in cache
     */
    public boolean containsKey(final String key) {
        return this.get(key) != null;
    }
}