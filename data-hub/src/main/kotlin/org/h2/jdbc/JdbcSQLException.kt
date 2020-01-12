package org.h2.jdbc

import java.sql.SQLNonTransientException

/**
 * Represents a database exception.
 */
/**
 * Creates a SQLException.
 *
 * @param message the reason
 * @param sql the SQL statement
 * @param state the SQL state
 * @param errorCode the error code
 * @param cause the exception that was the reason for this exception
 * @param stackTrace the stack trace
 */
class JdbcSQLException(
        override var message: String?,
        override var SQL: String?,
        override val originalMessage: String? = null,
        override val stackTrace: String?,
        cause: Throwable?,
        state: String?,
        errorCode: Int) : SQLNonTransientException(message, state, errorCode), JdbcException {
    init {
        setSQL(SQL)
        initCause(cause)
    }
}