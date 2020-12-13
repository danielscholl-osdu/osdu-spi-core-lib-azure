package org.opengroup.osdu.azure.logging;

/**
 * Interface for Core Logger Factory to return appropriate logger instances.
 */
public interface ICoreLoggerFactory {
    /**
     * @param name the name of the logger
     * @return Return a logger named according to the name parameter.
     */
    ICoreLogger getLogger(String name);

    /**
     * @param clazz the name of the logger
     * @return Return a logger named corresponding to the class passed as parameter.
     */
    ICoreLogger getLogger(Class<?> clazz);
}
