package org.h2.jdbc

import java.sql.SQLNonTransientException

/**
 * Represents a database exception
 */
/**
 * Creates a SQLNonTransientException.
 *
 * @param message the reason
 * @param sql the SQL statement
 * @param state the SQL state
 * @param errorCode the error code
 * @param cause the exception that was the reason for this exception
 * @param stackTrace the stack trace
 */
class JdbcSQLNonTransientException(
        message: String?,
        sql: String?,
        state: String?,
        errorCode: Int,
        cause: Throwable?,
        stackTrace: String?
) : SQLNonTransientException(message, state, errorCode), JdbcException {

    private val _originalMessage: String? = message
    private val stackTrace: String? = stackTrace

    init {
        setSQL(sql)
        initCause(cause)
    }

    fun getMessage(): String? {
        return message
    }

    override fun getOriginalMessage(): String? {
        return _originalMessage
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}