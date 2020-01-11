package org.h2.message

import org.h2.api.ErrorCode.OUT_OF_MEMORY
import org.h2.jdbc.JdbcException
import org.h2.util.SortedProperties
import org.h2.util.StringUtils
import org.h2.util.Utils
import java.io.ByteArrayInputStream
import java.io.IOException
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
        fun translate(key: String, vararg params: String): String {
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
        fun getJdbcSQLException(message: String?, sql: String?, state: String?, errorCode: Int, cause: Throwable, stackTrace: String?): SQLException {
            val _sql: String? = filterSQL(sql)
            when (errorCode / 1_000) {
                2->
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
            if (e.getSQL() == null) e.setSQL(filterSQL(sql))
            return this
        }

    }


}