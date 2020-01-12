package org.h2.jdbc

import org.h2.message.DbException

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
//    fun getOriginalMessage(): String?
    val originalMessage: String?

    /**
     * Returns the SQL statement.
     * <p>
     * SQL statements that contain '--hide--' are not listed
     * </p>
     *
     * @return the SQL statement
     */
//    fun getSQL(): String?
    var SQL: String?

    /**
     * INTERNAL
     */
//    fun setSQL(sql: String?): Unit

    val stackTrace: String?

    var message: String?
}

/**
 * Returns the class name, the message, and in the server mode, the stack
 * trace of the server
 *
 * @return the string representation
 */
fun JdbcException.toString(): String = stackTrace ?: this.toString()

fun JdbcException.setSQL(sql: String?) {
    this.SQL = sql
    this.message = DbException.buildMessageForException(this)
}