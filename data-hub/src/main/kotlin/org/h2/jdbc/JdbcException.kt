package org.h2.jdbc

/**
 * This interface contains additional methods for database exceptions.
 */
interface JdbcException {
    /**
     * Return the H2-specific error code.
     *
     * @return the H2-specific error code
     */
    fun getErrorCode(): Int

    /**
     * INTERNAL
     */
    fun getOriginalMessage(): String?

    /**
     * Returns the SQL statement.
     * <p>
     * SQL statements that contain '--hide--' are not listed
     * </p>
     *
     * @return the SQL statement
     */
    fun getSQL(): String?

    /**
     * INTERNAL
     */
    fun setSQL(sql: String?): Unit

    /**
     * Returns the class name, the message, and in the server mode, the stack
     * trace of the server
     *
     * @return the string representation
     */
    override fun toString(): String
}