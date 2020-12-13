package org.opengroup.osdu.azure.logging;

/**
 * Interface for Core Logger Factory Provider.
 */
public interface ICoreLoggerFactoryProvider {
    /**
     * @return Return a logger factory.
     */
    ICoreLoggerFactory getLoggerFactory();
}
