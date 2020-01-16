package org.h2.message

import org.h2.api.ErrorCode.EXCEPTION_IN_FUNCTION_1
import org.h2.api.ErrorCode.GENERAL_ERROR_1
import org.h2.api.ErrorCode.IO_EXCEPTION_1
import org.h2.api.ErrorCode.METHOD_NOT_ALLOWED_FOR_QUERY
import org.h2.api.ErrorCode.METHOD_ONLY_ALLOWED_FOR_QUERY
import org.h2.api.ErrorCode.OUT_OF_MEMORY
import org.h2.api.ErrorCode.UNKNOWN_DATA_TYPE_1
import org.h2.api.ErrorCode.getState
import org.h2.engine.Constants
import org.h2.jdbc.*
import org.h2.util.SortedProperties
import org.h2.util.StringUtils
import org.h2.util.Utils
import java.io.ByteArrayInputStream
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.nio.charset.StandardCharsets
import java.sql.DriverManager
import java.sql.SQLException
import java.text.MessageFormat
import java.util.*

/**
 * This exception wraps a checked exception.
 * It is used in methods where checked exceptions are not supported,
 * for example in a comparator
 */
class DbException(msg: String?, e: SQLException) : RuntimeException(msg, e) {
    companion object {
        /**
         * If the SQL statement contains this text, then it is never added to the
         * SQL exception. Hiding the SQL statement may be important if it contains a
         * passwords, such as a CREATE LINKED TABLE statement
         */
        @JvmStatic
        val HIDE_SQL: String = "--hide--"

        @JvmStatic
        val MESSAGES: Properties = Properties()

        val SQL_OOME: SQLException = SQLException("OutOfMemoryError", "HY000", OUT_OF_MEMORY, OutOfMemoryError())
        val OOME: DbException = DbException(SQL_OOME)

        /**
         * Write the exception to the driver manager log writer if configured
         *
         * @param e the exception
         */
        fun traceThrowable(e: Throwable): Unit {
            DriverManager.getLogWriter()?.let {
                e.printStackTrace(it)
            }
        }

        init {
            try {
                Utils.getResource("/org/h2/res/_messages_en.prop")?.let {
                    MESSAGES.load(ByteArrayInputStream(it))
                }
                val lang: String = Locale.getDefault().language
                if ("en" != lang) {
                    Utils.getResource("/org/h2/res/_messages_$lang.prop")?.let { translations ->
                        SortedProperties.fromLines(String(translations, StandardCharsets.UTF_8))
                                .entries
                                .filter { translation -> translation.value != null && !(translation.value as String).startsWith('#') }
                                .forEach { translation ->
                                    MESSAGES.compute(translation.key) { k, v ->
                                        "$translation.value\n$v"
                                    }
                                }
                    }
                }

            } catch (e: Throwable) {
                traceThrowable(e)
            }
        }

        /**
         * Convert an exception to an IO exception.
         *
         * @param e the root cause
         * @return the IO exception
         */
        @JvmStatic
        fun convertToIOException(e: Throwable): IOException {
            if (e is IOException) return e
            return (if (e is JdbcException) e.cause else e).let { IOException(it.toString(), it) }
        }

        @JvmStatic
        fun translate(key: String, vararg params: String?): String {
            val message: String = MESSAGES.getProperty(key) ?: "(Message $key not found)"
            return MessageFormat.format(message,
                    params.map { p ->
                        if (!p.isNullOrEmpty()) StringUtils.quotedIdentifier(p)
                        else p
                    })
        }

        @JvmStatic
        fun filterSQL(sql: String?): String? {
            return if (sql == null || !sql.contains(HIDE_SQL)) sql else "-"
        }

        /**
         * Creates a SQLException.
         *
         * @param message the reason
         * @param sql the SQL statement
         * @param state the SQL state
         * @param errorCode the error code
         * @param cause the exception that was the reason for this exception
         * @param stackTrace the stack trace
         * @return the SQLException object
         */
        @JvmStatic
        fun getJdbcSQLException(message: String?, sql: String?, state: String?, errorCode: Int, cause: Throwable?, stackTrace: String?): SQLException {
            val _sql: String? = filterSQL(sql)
            when (errorCode / 1_000) {
                2 -> return JdbcSQLNonTransientException(
                        originalMessage = message,
                        message = message,
                        SQL = sql,
                        state = state,
                        errorCode = errorCode,
                        cause = cause,
                        stackTrace = stackTrace)
                7, 21, 42 -> return JdbcSQLSyntaxErrorException(
                        originalMessage = message,
                        message = message,
                        SQL = sql,
                        state = state,
                        errorCode = errorCode,
                        cause = cause,
                        stackTrace = stackTrace)
                8 -> return JdbcSQLNonTransientConnectionException(
                        originalMessage = message,
                        message = message,
                        SQL = sql,
                        state = state,
                        errorCode = errorCode,
                        cause = cause,
                        stackTrace = stackTrace)
                22 -> return JdbcSQLDataException(
                        originalMessage = message,
                        message = message,
                        SQL = sql,
                        state = state,
                        errorCode = errorCode,
                        cause = cause,
                        stackTrace = stackTrace)
                23 -> return JdbcSQLIntegrityConstraintViolationException(
                        originalMessage = message,
                        message = message,
                        SQL = sql,
                        state = state,
                        errorCode = errorCode,
                        cause = cause,
                        stackTrace = stackTrace)
                28 -> return JdbcSQLInvalidAuthorizationSpecException(
                        originalMessage = message,
                        message = message,
                        SQL = sql,
                        state = state,
                        errorCode = errorCode,
                        cause = cause,
                        stackTrace = stackTrace)
                40 -> return JdbcSQLTransactionRollbackException(
                        originalMessage = message,
                        message = message,
                        SQL = sql,
                        state = state,
                        errorCode = errorCode,
                        cause = cause,
                        stackTrace = stackTrace)
                else -> return JdbcSQLException(message = message,
                        originalMessage = message,
                        SQL = sql,
                        state = state,
                        errorCode = errorCode,
                        cause = cause,
                        stackTrace = stackTrace)
            }

            // Check error code
            when (errorCode) {
                GENERAL_ERROR_1,
                UNKNOWN_DATA_TYPE_1,
                METHOD_NOT_ALLOWED_FOR_QUERY, METHOD_ONLY_ALLOWED_FOR_QUERY,

            }
        }

        /**
         * Builds message for an exception.
         *
         * @param e exception
         * @return message
         */
        @JvmStatic
        fun buildMessageForException(e: JdbcException): String {
            val sql: String = if (e.SQL != null) "; SQL statement:\n${e.SQL}" else ""
            return "${e.originalMessage ?: "- "} $sql [${e.getErrorCode()}-${Constants.BUILD_ID}]"
        }


        /**
         * Create a database exception for a specific error code.
         * @param errorCode the error code
         * @param params the list of parameters of the message
         * @return the exception
         */
        @JvmStatic
        fun get(errorCode: Int, vararg params: String): DbException = DbException(getJdbcSQLException(errorCode, null, *params))

        /**
         * Create a database exception for a specific error code.
         * @param errorCode the error code
         * @param params the list of parameters of the message
         * @return the exception
         */
        @JvmStatic
        fun get(errorCode: Int, cause: Throwable, vararg params: String?): DbException =
                DbException(getJdbcSQLException(errorCode, cause, *params))

        /**
         * Gets the SQL exception object for a specific error code.
         * @param errorCode the error code
         * @param cause the cause of the exception
         * @param params the list of parameters of the message
         * @return the SQLException object
         */
        @JvmStatic
        fun getJdbcSQLException(errorCode: Int, cause: Throwable?, vararg params: String?): SQLException = getState(errorCode).let { getJdbcSQLException(translate(it, *params), null, it, errorCode, cause, null) }

        /**
         * Convert an InvocationTarget exception to a database exception.
         * @param te the root cause
         * @param message the added message or null
         * @return the database exception object
         */
        @JvmStatic
        fun convertInvocation(te: InvocationTargetException, message: String?): DbException {
            val t: Throwable = te.targetException
            if (t is SQLException || t is DbException)
                return convert(t)
            return get(EXCEPTION_IN_FUNCTION_1, t, message)
        }

        /**
         * Convert a throwable to an SQL exception using the default mapping. All
         * errors except the following are re-thrown: StackOverflowError, LinkageError.
         * @param e the root cause
         * @return the exception object
         */
        @JvmStatic
        fun convert(e: Throwable): DbException = try {
            when (e) {
                is DbException -> e
                is SQLException -> DbException(e)
                is InvocationTargetException -> convertInvocation(e, null)
                is IOException -> get(IO_EXCEPTION_1, e, e.toString())
                is OutOfMemoryError -> get(OUT_OF_MEMORY, e)
                is StackOverflowError, is LinkageError -> get(GENERAL_ERROR_1, e, e.toString())
                is Error -> throw e
                else -> get(GENERAL_ERROR_1, e, e.toString())
            }
        } catch (ignore: OutOfMemoryError) {
            OOME
        } catch (ex: Throwable) {
            try {
                DbException(SQLException("GeneralError", "HY000", GENERAL_ERROR_1, e)).let {
                    it.addSuppressed(ex)
                    it
                }
            } catch (ignore: OutOfMemoryError) {
                OOME
            }
        }
    }

    private constructor(e: SQLException) : this(e.message, e)

    /**
     * Get the SQLException object.
     *
     * @return the exception
     */
    fun getSQLException(): SQLException? = cause as SQLException?

    /**
     * Get the error code.
     *
     * @return the error code
     */
    fun getErrorCode(): Int = getSQLException()!!.errorCode

    /**
     * Set the SQL statement of the give exception.
     * This method may create a new object.
     *
     * @param sql the SQL statement
     * @return the exception
     */
    fun addSQL(sql: String?): DbException {
        var e: SQLException? = getSQLException()
        if (e is JdbcException) {
            if (e.SQL == null) e.SQL = filterSQL(sql)
            return this
        }
        return DbException(getJdbcSQLException(message = e!!.message,
                sql = sql,
                state = e.sqlState,
                errorCode = e.errorCode,
                cause = e,
                stackTrace = null))
    }
}