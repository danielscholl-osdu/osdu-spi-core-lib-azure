package org.opengroup.osdu.azure.cache;

import com.microsoft.azure.servicebus.SubscriptionClient;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Implementation of ICache for SubscriptionClient.
 */
@Component
@Lazy
public class SubscriptionClientCache extends VmCache<String, SubscriptionClient> {

    /**
     *  Default cache constructor.
     */
    public SubscriptionClientCache() {
        super(60 * 60, 1000);
    }

    /**
     * @param key cache key
     * @return true if found in cache
     */
    public boolean containsKey(final String key) {
        return this.get(key) != null;
    }
}