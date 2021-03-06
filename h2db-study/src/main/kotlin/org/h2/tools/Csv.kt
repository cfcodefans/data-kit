package org.h2.tools

import org.h2.api.ErrorCode
import org.h2.engine.Constants
import org.h2.engine.SysProperties
import org.h2.message.DbException
import org.h2.store.fs.FileUtils
import org.h2.util.IOUtils
import org.h2.util.JdbcUtils
import org.h2.util.StringUtils
import org.h2.util.Utils
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.*
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.SQLException
import java.sql.Types

/**
 * A facility to read from and write to CSV (comma separated values) files.
 * When reading, the BOM (the byte-order-mark) character 0xfeff at the beginning of
 * the file is ignored.
 */
class Csv : SimpleRowSource {
    companion object {
        @JvmStatic
        fun convertException(message: String, e: Exception): SQLException =
            DbException.getJdbcSQLException(ErrorCode.IO_EXCEPTION_1, e, message)

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

    var fileName: String? = ""
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
        try {
            var rows: Int = 0

            val meta: ResultSetMetaData = rs.metaData
            val colCount: Int = meta.columnCount
            val row: Array<String?> = Array(colCount) { null }
            val sqlTypes: IntArray = IntArray(colCount)
            for (i in 0 until colCount) {
                row[i] = meta.getColumnLabel(i + 1)
                sqlTypes[i] = meta.getColumnType(i)
            }
            if (writeColumnHeader) writeRow(row)
            while (rs.next()) {
                for (i in 0 until colCount) {
                    row[i] = rs.getString(i + 1)
                }
                writeRow(row)
                rows++
            }
            return rows
        } catch (e: IOException) {
            throw DbException.convertIOException(e, null)
        } finally {
            close()
            JdbcUtils.closeSilently(rs)
        }
    }

    /**
     * Writes the result set to a file in the CSV format.
     *
     * @param writer the writer
     * @param rs the result set
     * @return the number of rows written
     */
    @Throws(SQLException::class)
    fun write(writer: Writer?, rs: ResultSet?): Int {
        output = writer
        return writeResultSet(rs!!)
    }

    private fun init(newFileName: String?, charset: String?) {
        fileName = newFileName
        this.charSet = charset
    }

    @Throws(IOException::class)
    private fun initWrite() {
        if (output != null) return
        try {
            var out = FileUtils.newOutputStream(fileName, false)
            out = BufferedOutputStream(out, Constants.IO_BUFFER_SIZE)
            output = BufferedWriter(charSet?.let { OutputStreamWriter(out, charSet) } ?: OutputStreamWriter(out))
        } catch (e: java.lang.Exception) {
            close()
            throw DbException.convertToIOException(e)
        }
    }

    @Throws(IOException::class)
    private fun writeRow(values: Array<String?>): Unit {
        for (i in values.indices) {
            if (i > 0 && fieldSeparatorWrite != null) output!!.write(fieldSeparatorWrite)
            val s = values[i]
            if (s != null) {
                if (escapeChar != null) {
                    if (fieldDelimiter != null) output!!.write(fieldDelimiter.toInt())
                    output!!.write(escape(s))
                    if (fieldDelimiter != null) output!!.write(fieldDelimiter.toInt())
                } else {
                    output!!.write(s)
                }
            } else if (!nullString.isNullOrEmpty()) output!!.write(nullString)
        }
        output!!.write(lineSeparator)
    }

    /**
     * Writes the result set to a file in the CSV format. The result set is read
     * using the following loop:
     *
     * <pre>
     * while (rs.next()) {
     * writeRow(row);
     * }
    </pre> *
     *
     * @param outputFileName the name of the csv file
     * @param rs the result set - the result set must be positioned before the
     * first row.
     * @param charset the charset or null to use the system default charset
     * @return the number of rows written
     */
    @Throws(SQLException::class)
    fun write(outputFileName: String, rs: ResultSet?, charset: String?): Int {
        init(outputFileName, charset!!)
        return try {
            initWrite()
            writeResultSet(rs!!)
        } catch (e: IOException) {
            throw convertException("IOException writing $outputFileName", e)
        }
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
                isParam(key, "caseSenstitiveColumnNames") -> caseSensitiveColumnNames =
                    Utils.parseBoolean(value, false, false)
                else -> DbException.getUnsupportedException(key)
            }
        }

        return charset!!
    }

    @Throws(SQLException::class)
    override fun readRow(): Array<Any> {
        input ?: return emptyArray()
        val colSize = columnNames!!.size
        val row: Array<String?> = Array(colSize) { null }
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
                if (i < colSize) row[i++] = v
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
            var ch: Int = readChar()
            when (ch.toChar()) {
                fieldDelimiter -> {
                    //delimited value
                    var containsEscape: Boolean = false
                    inputBufferStart = inputBufferPos
                    var sep: Int = 0
                    while (true) {
                        ch = readChar()
                        if (ch.toChar() == fieldDelimiter) {
                            ch = readChar()
                            if (ch.toChar() != fieldDelimiter) {
                                sep = 2
                                break
                            }
                            containsEscape = true
                        } else if (ch.toChar() == escapeChar) {
                            ch = readChar()
                            if (ch < 0) {
                                sep = 1
                                break
                            }
                            containsEscape = true
                        } else if (ch < 0) {
                            sep = 1
                            break
                        }
                    }
                    var s = String(inputBuffer!!, inputBufferStart, inputBufferPos - inputBufferStart - sep)
                    if (containsEscape) s = unEscape(s)
                    inputBufferStart = -1
                    while (true) {
                        if (ch == fieldSeparatorRead.toInt()) {
                            break
                        } else if (ch == '\n'.toInt() || ch < 0 || ch == '\r'.toInt()) {
                            endOfLine = true
                            break
                        } else if (ch == ' '.toInt() || ch == '\t'.toInt()) {
                            // ignore
                        } else {
                            pushBack()
                            break
                        }
                        ch = readChar()
                    }
                    return s
                }
            }
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
                val src = inputBuffer!!
                if (keep + Constants.IO_BUFFER_SIZE > src.size) {
                    inputBuffer = CharArray(src.size * 2)
                }
                src.copyInto(inputBuffer!!, 0, inputBufferStart, inputBufferStart + keep)
            }
        } else keep = 0
        inputBufferPos = keep
        val len: Int = input!!.read(inputBuffer, keep, Constants.IO_BUFFER_SIZE)
        if (len == -1) {
            // ensure bufferPos > bufferEnd
            // even after pushBack
            inputBufferEnd = -1024
            endOfFile = true
            // ensure the right number of characters are read
            // in case the input buffer is still used
            inputBufferPos++
            return -1
        }
        inputBufferEnd = keep + len
        return inputBuffer!![inputBufferPos++].toInt()
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

    fun unEscape(s: String): String {
        val sb: java.lang.StringBuilder = java.lang.StringBuilder(s.length)
        var start: Int = 0
        var chars: CharArray? = null
        while (true) {
            var idx: Int = s.indexOf(escapeChar, start)
            if (idx < 0) {
                idx = s.indexOf(fieldDelimiter, start)
                if (idx < 0) break
            }
            if (chars == null) {
                chars = s.toCharArray()
            }
            sb.append(chars, start, idx - start)
            if (idx == s.length - 1) {
                start = s.length
                break
            }
            sb.append(chars[idx + 1])
            start = idx + 2
        }
        sb.append(s, start, s.length)
        return sb.toString()
    }

    private fun pushBack() {
        inputBufferPos--
    }

    /**
     * Reads CSV data from a reader and returns a result set. The rows in the
     * result set are created on demand, that means the reader is kept open
     * until all rows are read or the result set is closed.
     *
     * @param reader the reader
     * @param colNames or null if the column names should be read from the CSV
     * file
     * @return the result set
     */
    @Throws(IOException::class)
    fun read(reader: Reader?, colNames: Array<String?>?): ResultSet? {
        init(null, null)
        input = reader
        return readResultSet(colNames)
    }

    @Throws(IOException::class)
    private fun readResultSet(colNames: Array<String?>): ResultSet? {
        columnNames = colNames
        initRead()
        val result = SimpleResultSet(this)
        makeColumnNamesUnique()
        for (columnName in columnNames!!) {
            result.addColumn(columnName, Types.VARCHAR, Int.MAX_VALUE, 0)
        }
        return result
    }

    private fun makeColumnNamesUnique() {
        for (i in columnNames!!.indices) {
            val buff = StringBuilder()
            val n = columnNames!![i]
            if (n.isNullOrEmpty()) {
                buff.append('C').append(i + 1)
            } else {
                buff.append(n)
            }
            var j = 0
            while (j < i) {
                val y = columnNames!![j]!!
                if (buff.toString() == y) {
                    buff.append('1')
                    j = -1
                }
                j++
            }
            columnNames!![i] = buff.toString()
        }
    }
}