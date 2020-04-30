package org.h2.tools

import org.h2.api.ErrorCode
import org.h2.engine.SysProperties
import org.h2.message.DbException
import org.h2.util.IOUtils
import org.h2.util.StringUtils
import java.io.Reader
import java.io.Writer
import java.sql.ResultSet
import java.sql.SQLException

/**
 * A facility to read from and write to CSV (comma separated values) files.
 * When reading, the BOM (the byte-order-mark) character 0xfeff at the beginning of
 * the file is ignored.
 */
class Csv : SimpleRowSource {
    companion object {
        @JvmStatic
        fun convertException(message: String, e: Exception): SQLException = DbException.getJdbcSQLException(ErrorCode.IO_EXCEPTION_1, e, message)

        private fun isParam(key: String, vararg values: String): Boolean =
                values.any { key.equals(it, ignoreCase = true) }
    }

    var columnNames: Array<String>? = null
    var charSet: String? = null
    var escapeChar: Char = '"'
    var fieldDelimiter: Char = '"'
    var fieldSeparatorRead: Char = ','
    var caseSensitiveColumnNames: Boolean = false
    var preserveWhitespace: Boolean = false
    var writeColumnHeader: Boolean = true

    var lineComment: Char? = null
    var lineSeparator: String = SysProperties.LINE_SEPARATOR
    var nullString: String = ""

    var fileName: String = ""
    var input: Reader? = null
    var inputBuffer: Array<Char>? = null
    var inputBufferPos: Int = 0
    var inputBufferStart: Int = -1
    var inputBufferEnd: Int = 0
    var output: Writer? = null
    var endOfLine: Boolean = false
    var endOfFile: Boolean = false

    /**
     * INTERNAL
     */
    override fun close() {
        IOUtils.closeSilently(input)
        input = null
        IOUtils.closeSilently(output)
        output = null
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun reset() {
        throw SQLException("Method is not supported", "CSV")
    }

    @Throws(SQLException::class)
    fun writeResultSet(rs: ResultSet): Int {

    }

    /**
     * INTERNAL.
     * Parse and set the CSV options.
     * @param options the options
     * @return the character set
     */
    fun setOption(options: String): String {
        var charset: String? = null
        val keyValuePars: Array<String> = StringUtils.arraySplit(options, ' ', false)!!


        return charset!!
    }
}