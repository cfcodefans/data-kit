package org.h2.tools

import org.h2.api.ErrorCode
import org.h2.engine.Constants
import org.h2.engine.SysProperties
import org.h2.message.DbException
import org.h2.store.fs.FileUtils
import org.h2.util.IOUtils
import org.h2.util.StringUtils
import org.h2.util.Utils
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.*
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

        @JvmStatic
        fun isSimpleColunName(columnName: String): Boolean {
            for (i in 0..columnName.length) {
                val ch = columnName[i]
                if (i == 0) {
                    if (ch != '_' && !Character.isLetter(ch)) {
                        return false
                    }
                } else if (ch != '_' && !Character.isLetterOrDigit(ch)) return false
            }
            return columnName.isNotEmpty()
        }
    }

    var columnNames: Array<String?>? = null
    var charSet: String? = null
    var escapeChar: Char = '"'
    var fieldDelimiter: Char = '"'
    var fieldSeparatorRead: Char = ','
    var fieldSeparatorWrite: String = ","
    var caseSensitiveColumnNames: Boolean = false
    var preserveWhitespace: Boolean = false
    var writeColumnHeader: Boolean = true

    var lineComment: Char? = null
    var lineSeparator: String = SysProperties.LINE_SEPARATOR
    var nullString: String = ""

    var fileName: String = ""
    var input: Reader? = null
    var inputBuffer: CharArray? = null
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
        val keyValuePairs: Array<String> = StringUtils.arraySplit(options, ' ', false)!!
        for (pair in keyValuePairs) {
            if (pair.isEmpty()) continue

            val index = pair.indexOf('=')
            val key: String = StringUtils.trim(pair.substring(0, index), " ")
            val value: String = pair.substring(index + 1)
            val ch: Char = if (value.isEmpty()) 0.toChar() else value[0]

            when {
                isParam(key, "escape", "esc", "escapeCharacter") -> escapeChar = ch
                isParam(key, "fieldDelimiter", "filedDelim") -> fieldDelimiter = ch
                isParam(key, "fieldSeparator", "fieldSep") -> {
                    fieldSeparatorRead = ch
                    fieldSeparatorWrite = value
                }
                isParam(key, "lineComment", "lineCommentCharacter") -> lineComment = ch
                isParam(key, "lineSeparator", "linSep") -> lineSeparator = value
                isParam(key, "null", "nullString") -> nullString = value
                isParam(key, "charset", "characterSet") -> charset = value
                isParam(key, "preserveWhitespace") -> preserveWhitespace = Utils.parseBoolean(value, false, false)
                isParam(key, "writeColumnHeader") -> writeColumnHeader = Utils.parseBoolean(value, true, false)
                isParam(key, "caseSenstitiveColumnNames") -> caseSensitiveColumnNames = Utils.parseBoolean(value, false, false)
                else -> DbException.getUnsupportedException(key)
            }
        }

        return charset!!
    }

    @Throws(SQLException::class)
    override fun readRow(): Array<Any> {
        input ?: return emptyArray()
        val row: Array<String?> = Array(columnNames!!.size) { null }
        try {
            var i = 0
            while (true) {
                val v: String = readValue()
                if (v == null && endOfLine) {
                    if (i == 0) {
                        if (endOfFile) return emptyArray()
                        //empty line
                        continue
                    }
                    break
                }
                if (i < row.size) row[i++] = v
                if (endOfLine) break
            }
        } catch (e: IOException) {
            throw convertException("IOException reading from $fileName", e)
        }
        return row.cast()
    }

    @Throws(IOException::class)
    private fun readValue(): String {
        endOfLine = false
        inputBufferStart = inputBufferPos
        while (true) {
            val ch: Int = readChar()
        }
    }

    @Throws(IOException::class)
    private fun readChar(): Int {
        return if (inputBufferPos >= inputBufferEnd) {
            readBuffer()
        } else inputBuffer!![inputBufferPos++].toInt()
    }

    @Throws(IOException::class)
    private fun readBuffer(): Int {
        if (endOfFile) return -1
        var keep = 0
        if (inputBufferStart >= 0) {
            keep = inputBufferPos - inputBufferStart
            if (keep > 0) {
                val src = inputBuffer
                if (keep + Constants.IO_BUFFER_SIZE > src.size) {
                    inputBuffer = CharArray(src!!.size * 2)
                }
                src!!.copyInto(inputBuffer!!, 0, inputBufferStart, inputBufferStart + keep)
            }
        }
    }


    @Throws(IOException::class)
    private fun initRead() {
        if (input == null) {
            try {
                val ins: InputStream = BufferedInputStream(FileUtils.newInputStream(fileName), Constants.IO_BUFFER_SIZE)
                input = if (charSet != null)
                    InputStreamReader(ins, charSet)
                else
                    InputStreamReader(ins)
            } catch (e: IOException) {
                close()
                throw e
            }
        }

        if (!input!!.markSupported()) input = BufferedReader(input)
        input!!.mark(1)
        val bom: Int = input!!.read()
        if (bom != 0xfeff) {
            // Microsoft Excel compatibility
            // ignore pseduo-BOM
            input!!.reset()
        }
        if (columnNames == null) readHeader()
    }

    @Throws(IOException::class)
    private fun readHeader() {
        val list: ArrayList<String?> = ArrayList()
        while (true) {
            var v: String? = readValue()
            if (v == null) {
                if (endOfLine) {
                    if (endOfFile || list.isNotEmpty()) break
                } else {
                    v = "COLUMN" + list.size
                    list.add(v)
                }
            } else {
                if (v.isEmpty()) {
                    v = "COLUMN" + list.size
                } else if (!caseSensitiveColumnNames && isSimpleColunName(v)) {
                    v = StringUtils.toUpperEnglish(v)
                }
                list.add(v)
                if (endOfLine) break
            }
        }
        columnNames = list.toTypedArray()
    }

    fun escape(data: String): String {
        if (data.indexOf(fieldDelimiter) < 0) {
            if (escapeChar == fieldDelimiter || data.indexOf(escapeChar) < 0)
                return data
        }
        val sb: StringBuilder = StringBuilder(data.length)
        for (ch in data) {
            if (ch == fieldDelimiter || ch == escapeChar) {
                sb.append(escapeChar)
            }
            sb.append(ch)
        }
        return sb.toString()
    }


}