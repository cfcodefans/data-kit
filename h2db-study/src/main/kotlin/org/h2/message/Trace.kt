package org.h2.message

import org.h2.engine.SysProperties
import org.h2.expression.ParameterInterface
import org.h2.message.TraceSystem.Companion.DEBUG
import org.h2.message.TraceSystem.Companion.ERROR
import org.h2.message.TraceSystem.Companion.INFO
import org.h2.util.StringUtils
import java.text.MessageFormat

/**
 * This class represents a trace module.
 */
class Trace(val traceWriter: TraceWriter,
            val module: String,
            val lineSeparator: String = SysProperties.LINE_SEPARATOR) {
    companion object {
        /**
         * The trace module id for commands.
         */
        const val COMMAND: Int = 0

        /**
         * The trace module id for constraints.
         */
        const val CONSTRAINT: Int = 1

        /**
         * The trace module id for databases.
         */
        const val DATABASE: Int = 2

        /**
         * The trace module id for functions.
         */
        const val FUNCTION: Int = 3

        /**
         * The trace module is for file locks.
         */
        const val FILE_LOCK: Int = 4

        /**
         * The trace module id for indexes.
         */
        const val INDEX: Int = 5

        /**
         * The trace module id for the JDBC API.
         */
        const val JDBC: Int = 6

        /**
         * The trace module id for locks.
         */
        const val LOCK: Int = 7

        /**
         * The trace module id for schemas.
         */
        const val SCHEMA: Int = 8

        /**
         * The trace module id for sequences.
         */
        const val SEQUENCE: Int = 9

        /**
         * The trace module id for settings.
         */
        const val SETTING: Int = 10

        /**
         * The trace module id for tables.
         */
        const val TABLE: Int = 11

        /**
         * The trace module id for triggers.
         */
        const val TRIGGER: Int = 12

        /**
         * The trace module id for users.
         */
        const val USER: Int = 13

        /**
         * The trace module id for the page org.h2.store.
         */
        const val PAGE_STORE: Int = 14

        /**
         * The trace module id for the JDBCX API
         */
        const val JDBCX: Int = 15

        /**
         * Module names by their ids as array indexes.
         */
        val MODULE_NAMES: Array<String> = arrayOf("command", "constraint", "database", "function", "fileLock", "index", "jdbc", "lock", "schema", "sequence", "setting", "table", "trigger", "user", "pageStore", "JDBCX")

        /**
         * Format the parameter list.
         *
         * @param parameters the parameter list
         * @return the formatted text
         */
        fun formatParams(parameters: ArrayList<out ParameterInterface>): String {
            if (parameters.isEmpty()) return ""

            return parameters.filter { it.isValueSet() }
                .mapIndexed { i, p -> "$i: ${p.getParamValue()?.getTraceSQL()}" }
                .joinToString(prefix = "{", separator = ", ", postfix = "}")
        }

    }

    constructor(traceWriter: TraceWriter, moduleId: Int) : this(traceWriter, MODULE_NAMES[moduleId])

    var traceLevel: Int = TraceSystem.PARENT
    inline fun isEnabled(level: Int): Boolean {
        return if (traceLevel == TraceSystem.PARENT)
            traceWriter.isEnabled(level)
        else level <= this.traceLevel
    }

    /**
     * Check if the trace level is equal or higher than INFO.
     * @return true if it is
     */
    inline fun isInfoEnabled(): Boolean = isEnabled(INFO)

    /**
     * Check if the trace level is equal or higher than DEBUG.
     * @return true if it is
     */
    inline fun isDebugEnabled(): Boolean = isEnabled(DEBUG)

    /**
     * Write a message with trace level ERROR to the trace system.
     * @param t the exception
     * @param s the message
     */
    inline fun error(t: Throwable, s: String?): Unit {
        if (isEnabled(ERROR)) {
            traceWriter.write(ERROR, module, s, t)
        }
    }

    /**
     * Write a message with trace level ERROR to the trace system.
     * @param t the exception
     * @param s the message
     * @param params the parameters
     */
    inline fun error(t: Throwable, s: String, vararg params: Any): Unit {
        if (isEnabled(ERROR)) {
            traceWriter.write(ERROR, module, s.format(params), t)
        }
    }

    /**
     * Write a message with trace level INFO to the trace system.
     * @param s the message
     */
    inline fun info(s: String?): Unit {
        if (isEnabled(INFO)) {
            traceWriter.write(INFO, module, s, null)
        }
    }

    /**
     * Write a message with trace level INFO to the trace system.
     * @param s the message
     * @param params the parameters
     */
    inline fun info(s: String, vararg params: Any): Unit {
        if (isEnabled(INFO)) {
            traceWriter.write(INFO, module, s.format(params), null)
        }
    }

    /**
     * Write a message with trace level INFO to the trace system.
     * @param t the exception
     * @param s the message
     */
    inline fun info(t: Throwable?, s: String?): Unit {
        if (isEnabled(INFO)) {
            traceWriter.write(INFO, module, s, t)
        }
    }

    /**
     * Write a message with trace level DEBUG to the trace system.
     * @param s the message
     */
    inline fun debug(s: String?): Unit {
        if (isEnabled(DEBUG)) {
            traceWriter.write(DEBUG, module, s, null)
        }
    }

    /**
     * Write a message with trace level DEBUG to the trace system.
     * @param s the message
     * @param params the parameters
     */
    @JvmName("debug1")
    inline fun debug(s: String, vararg params: Any): Unit {
        if (isEnabled(DEBUG)) {
            traceWriter.write(DEBUG, module, s.format(params), null)
        }
    }

    /**
     * Write a message with trace level DEBUG to the trace system.
     * @param t the exception
     * @param s the message
     */
    inline fun debug(t: Throwable?, s: String?): Unit {
        if (isEnabled(DEBUG)) {
            traceWriter.write(DEBUG, module, s, t)
        }
    }

    /**
     * Write a SQL statement with trace level INFO to the trace system.
     *
     * @param sql the SQL statement
     * @param params the parameters used, in the for {1:...}
     * @param count the update count
     * @param time the time it took to run the statement in ms
     */
    fun infoSQL(sql: String, params: String, count: Long, time: Long) {
        var sql = sql
        if (!isEnabled(INFO)) return

        val buff = StringBuilder(sql.length + params.length + 20)
        buff.append(lineSeparator).append("/*SQL")
        var space = false
        if (params.isNotEmpty()) {
            // This looks like a bug, but it is intentional:
            // If there are no parameters, the SQL statement is
            // the rest of the line. If there are parameters, they
            // are appended at the end of the line. Knowing the size
            // of the statement simplifies separating the SQL statement
            // from the parameters (no need to parse).
            space = true
            buff.append(" l:").append(sql.length)
        }
        if (count > 0) {
            space = true
            buff.append(" #:").append(count)
        }
        if (time > 0) {
            space = true
            buff.append(" t:").append(time)
        }
        if (!space) {
            buff.append(' ')
        }
        buff.append("*/")
        StringUtils.javaEncode(sql, buff, false)
        StringUtils.javaEncode(params, buff, false)
        buff.append(';')
        sql = buff.toString()
        traceWriter.write(INFO, module, sql, null)
    }

    /**
     * Write a message with trace level DEBUG to the trace system.
     *
     * @param s the message
     * @param params the parameters
     */
    fun debug(s: String?, vararg params: Any?) {
        var s = s
        if (!isEnabled(DEBUG)) return
        s = MessageFormat.format(s, *params)
        traceWriter.write(DEBUG, module, s, null)
    }

    /**
     * Write Java source code with trace level INFO to the trace system.
     *
     * @param java the source code
     */
    fun infoCode(java: String) {
        if (isEnabled(INFO)) traceWriter.write(INFO, module, """$lineSeparator/**/$java""", null)
    }

    /**
     * Write Java source code with trace level DEBUG to the trace system.
     *
     * @param java the source code
     */
    fun debugCode(java: String) {
        if (isEnabled(DEBUG)) traceWriter.write(DEBUG, module, """$lineSeparator/**/$java""", null)
    }
}

