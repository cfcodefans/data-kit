package org.h2.jdbc

import java.sql.SQLIntegrityConstraintViolationException

/**
 * Represents a database exception
 */
/**
 * Create a SQLIntegrityConstraintViolationException.
 * @param message the reason
 * @param sql the SQL statement
 * @param state the SQL state
 * @param errorCode the error code
 * @param cause the exception that was the reason for this exception
 * @param stackTrace the stack trace
 */
class JdbcSQLIntegrityConstraintViolationException(
        state: String?,
        cause: Throwable?,
        errorCode: Int,
        override var message: String?,
        override val originalMessage: String? = null,
        override var SQL: String?,
        override val stackTrace: String?
) : SQLIntegrityConstraintViolationException(message, state, errorCode), JdbcException {
    init {
        setSQL(SQL)
        initCause(cause)
    }
}