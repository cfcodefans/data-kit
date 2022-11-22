package org.h2.message

/**
 * The backend of the trace system must implement this interface. Two
 * implementations are supported: the (default) native trace writer
 * implementation that can write to a file and to system out, and an adapter
 * that uses SLF4J (Simple Logging Facade for Java).
 */
interface TraceWriter {
    /**
     * Set the name of the database or trace object.
     * @param name the new name
     */
    fun setName(name: String): Unit

    /**
     * Write a message
     * @param level the trace level
     * @param module the name of the module
     * @param s the message
     * @param t the exception (may be null)
     */
    fun write(level: Int, module: String, s: String, t: Throwable?): Unit

    /**
     * Write a message
     * @param level the trace level
     * @param moduleId the id of the module
     * @param s the message
     * @param t the exception (may be null)
     */
    fun write(level: Int, moduleId: Int, s: String, t: Throwable?): Unit

    /**
     * Check the given trace / log level is enabled.
     * @param level the level
     * @return true if the level is enabled
     */
    fun isEnabled(level: Int): Boolean
}