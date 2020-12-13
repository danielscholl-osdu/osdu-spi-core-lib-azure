package org.opengroup.osdu.azure.logging;

import org.springframework.stereotype.Component;

/**
 * Implementation for Core Logger Factory Provider instances.
 */
@Component
public final class CoreLoggerFactoryProvider implements ICoreLoggerFactoryProvider {

    /**
     * @return Return a logger factory.
     */
    @Override
    public ICoreLoggerFactory getLoggerFactory() {
        return CoreLoggerFactory.getInstance();
    }
}
