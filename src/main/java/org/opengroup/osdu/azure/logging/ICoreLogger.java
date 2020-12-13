package org.opengroup.osdu.azure.logging;

import org.opengroup.osdu.core.common.logging.audit.AuditPayload;

/**
 * Interface for common logger to support traces, exceptions, dependencies among services, and audit events.
 */
public interface ICoreLogger {
    /**
     * Log a message at the INFO level.
     *
     * @param msg the message string to be logged
     */
    void info(String msg);

    /**
     * Log a message at the INFO level according to the specified format and arguments.
     *
     * @param format    the format string
     * @param arguments a list of arguments
     */
    void info(String format, Object... arguments);

    /**
     * Log a message at the WARN level.
     *
     * @param msg the message string to be logged
     */
    void warn(String msg);

    /**
     * Log a message at the WARN level according to the specified format and arguments.
     *
     * @param format    the format string
     * @param arguments a list of arguments
     */
    void warn(String format, Object... arguments);

    /**
     * Log an exception (throwable) at the WARN level with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    void warn(String msg, Throwable t);

    /**
     * Log a message at the ERROR level.
     *
     * @param msg the message string to be logged
     */
    void error(String msg);

    /**
     * Log a message at the ERROR level according to the specified format and arguments.
     *
     * @param format    the format string
     * @param arguments a list of arguments
     */
    void error(String format, Object... arguments);

    /**
     * Log an exception (throwable) at the ERROR level with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    void error(String msg, Throwable t);

    /**
     * Log an audit.
     *
     * @param payload the audit payload
     */
    void logAudit(AuditPayload payload);

    /**
     * Log a dependency.
     *
     * @param payload the dependency payload
     */
    void logDependency(DependencyPayload payload);
}
