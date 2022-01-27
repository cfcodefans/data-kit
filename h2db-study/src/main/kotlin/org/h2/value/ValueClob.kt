package org.h2.value

import org.h2.engine.CastDataProvider
import org.h2.engine.Constants
import org.h2.engine.SysProperties
import org.h2.message.DbException
import org.h2.store.DataHandler
import org.h2.store.FileStoreOutputStream
import org.h2.store.RangeReader
import org.h2.util.Bits
import org.h2.util.HasSQL
import org.h2.util.IOUtils
import org.h2.util.MathUtils
import org.h2.util.StringUtils
import org.h2.value.lob.LobData
import org.h2.value.lob.LobDataDatabase
import org.h2.value.lob.LobDataFetchOnDemand
import org.h2.value.lob.LobDataFile
import org.h2.value.lob.LobDataInMemory
import java.io.BufferedReader
import java.io.IOException
import java.io.Reader
import java.nio.charset.StandardCharsets

/**
 * Implementation of the CHARACTER LARGE OBJECT data type.
 */
class ValueClob(lobData: LobData, octetLen: Long, charLen: Long) : ValueLob(lobData, octetLen, charLen) {

    companion object {
        /**
         * Creates a small CLOB value that can be stored in the row directly.
         *
         * @param data the data in UTF-8 encoding
         * @return the CLOB
         */
        fun createSmall(data: ByteArray): ValueClob = ValueClob(
                lobData = LobDataInMemory(data),
                octetLen = data.size.toLong(),
                charLen = String(data, StandardCharsets.UTF_8).length.toLong())

        /**
         * Creates a small CLOB value that can be stored in the row directly.
         *
         * @param data the data in UTF-8 encoding
         * @param charLength the count of characters, must be exactly the same as count of characters in the data
         * @return the CLOB
         */
        fun createSmall(data: ByteArray, charLength: Long): ValueClob =
                ValueClob(lobData = LobDataInMemory(data),
                        octetLen = data.size.toLong(),
                        charLen = charLength)

        /**
         * Creates a small CLOB value that can be stored in the row directly.
         *
         * @param string the string with value
         * @return the CLOB
         */
        fun createSmall(string: String): ValueClob = string.toByteArray(StandardCharsets.UTF_8)
                .let { bytes ->
                    ValueClob(lobData = LobDataInMemory(bytes),
                            octetLen = bytes.size.toLong(),
                            charLen = string.length.toLong())
                }

        /**
         * Create a temporary CLOB value from a stream.
         *
         * @param `in` the reader
         * @param length the number of characters to read, or -1 for no limit
         * @param handler the data handler
         * @return the lob value
         */
        fun createTempClob(reader: Reader, length: Long, handler: DataHandler): ValueClob {
            var rd = reader
            if (length >= 0) {
                // Otherwise BufferedReader may try to read more data than needed and that
                // blocks the network level
                rd = try {
                    RangeReader(rd, 0, length)
                } catch (e: IOException) {
                    throw DbException.convert(e)
                }
            }

            val reader: BufferedReader = if (rd is BufferedReader) rd else BufferedReader(rd, Constants.IO_BUFFER_SIZE)

            return try {
                var remaining = if (length >= 0 && length < Long.MAX_VALUE) length else Long.MAX_VALUE

                var len = getBufferSize(handler, remaining)
                val buff: CharArray
                if (len >= Int.MAX_VALUE) {
                    val data = IOUtils.readStringAndClose(reader, -1)
                    buff = data.toCharArray()
                    len = buff.size
                } else {
                    buff = CharArray(len)
                    reader.mark(len)
                    len = IOUtils.readFully(reader, buff, len)
                }
                if (len <= handler.getMaxLengthInplaceLob()) {
                    return createSmall(String(buff, 0, len))
                }
                reader.reset()
                createTemporary(handler, reader, remaining)
            } catch (e: IOException) {
                throw DbException.convertIOException(e, null)
            }
        }

        /**
         * Create a CLOB in a temporary file.
         */
        @Throws(IOException::class)
        private fun createTemporary(handler: DataHandler, `in`: Reader, remaining: Long): ValueClob {
            val fileName = createTempLobFileName(handler)
            val tempFile = handler.openFile(fileName!!, "rw", false)
            tempFile.autoDelete()

            var octetLength = 0L
            var charLength = 0L

            FileStoreOutputStream(tempFile, null).use { out ->
                val buff = CharArray(Constants.IO_BUFFER_SIZE)
                while (true) {
                    var len = getBufferSize(handler, remaining)
                    len = IOUtils.readFully(`in`, buff, len)
                    if (len == 0) break
                    // TODO reduce memory allocation
                    val data = String(buff, 0, len).toByteArray(StandardCharsets.UTF_8)
                    out.write(data)
                    octetLength += data.size.toLong()
                    charLength += len.toLong()
                }
            }
            return ValueClob(LobDataFile(handler, fileName, tempFile), octetLength, charLength)
        }

        /**
         * Compares two CLOB values directly.
         *
         * @param v1 first CLOB value
         * @param v2 second CLOB value
         * @return result of comparison
         */
        private fun compare(v1: ValueClob, v2: ValueClob): Int {
            var minPrec = v1.charLength.coerceAtMost(v2.charLength)
            try {
                v1.getReader().use { reader1 ->
                    v2.getReader().use { reader2 ->
                        val buf1 = CharArray(BLOCK_COMPARISON_SIZE)
                        val buf2 = CharArray(BLOCK_COMPARISON_SIZE)

                        while (minPrec >= BLOCK_COMPARISON_SIZE) {
                            if (IOUtils.readFully(reader1!!, buf1, BLOCK_COMPARISON_SIZE) != BLOCK_COMPARISON_SIZE
                                    || IOUtils.readFully(reader2!!, buf2, BLOCK_COMPARISON_SIZE) != BLOCK_COMPARISON_SIZE) {
                                throw DbException.getUnsupportedException("Invalid LOB")
                            }
                            val cmp = Bits.compareNotNull(buf1, buf2)
                            if (cmp != 0) return cmp

                            minPrec -= BLOCK_COMPARISON_SIZE.toLong()
                        }

                        while (true) {
                            val c1 = reader1!!.read()
                            val c2 = reader2!!.read()
                            if (c1 < 0) return if (c2 < 0) 0 else -1
                            if (c2 < 0) return 1
                            if (c1 != c2) return if (c1 < c2) -1 else 1
                        }
                    }
                }
            } catch (ex: IOException) {
                throw DbException.convert(ex)
            }
        }

        internal fun Value.convertToClob(targetType: TypeInfo, conversionMode: Int, column: Any?): ValueClob {
            var v: ValueClob = when (getValueType()) {
                CLOB -> this as ValueClob
                JAVA_OBJECT -> throw getDataConversionError(targetType.valueType)
                BLOB -> {
                    val data = (this as ValueBlob).lobData
                    // Try to reuse the array, if possible
                    if (data is LobDataInMemory) {
                        val small = data.small
                        var bytes: ByteArray = String(small, StandardCharsets.UTF_8).toByteArray(StandardCharsets.UTF_8)
                        if (bytes.contentEquals(small)) {
                            bytes = small
                        }
                        createSmall(bytes)
                    } else if (data is LobDataDatabase) {
                        data.getDataHandler()!!.getLobStorage().createClob(getReader(), -1)!!
                    } else createSmall(getString())
                }
                else -> createSmall(getString()!!)
            }

            if (conversionMode == CONVERT_TO) return v

            if (conversionMode == CAST_TO) return v.convertPrecision(targetType.precision)

            if (v.charLength() > targetType.precision) throw v.getValueTooLongException(targetType, column)

            return v
        }
    }

    override fun copy(database: DataHandler, tableId: Int): ValueLob? {
        return when (lobData) {
            is LobDataInMemory -> {
                val small = lobData.small
                if (small.size > database.getMaxLengthInplaceLob()) {
                    val s = database.getLobStorage()
                    val v = s.createClob(getReader(), charLength)
                    val v2 = v!!.copy(database, tableId)
                    v!!.remove()
                    return v2
                }
                this
            }
            is LobDataDatabase -> database.getLobStorage().copyLob(this, tableId)
            else -> throw UnsupportedOperationException()
        }
    }

    override fun charLength(): Long = charLength

    override fun octetLength(): Long {
        if (octetLength >= 0L) return octetLength

        if (lobData is LobDataInMemory) {
            octetLength = lobData.small.size.toLong()
            return octetLength
        }

        var p: Long = 0L
        try {
            getInputStream()!!.use { inputStream ->
                while (true) {
                    p += inputStream.skip(Long.MAX_VALUE)
                    if (inputStream.read() < 0) break
                    p++
                }
            }
        } catch (e: IOException) {
            throw DbException.convertIOException(e, null)
        }
        octetLength = p
        return p
    }

    override fun getBytesInternal(): ByteArray {
        if (octetLength >= 0L) {
            if (octetLength > Constants.MAX_STRING_LENGTH) throw getBinaryTooLong(octetLength)!!
            return readBytes(octetLength.toInt())
        }

        val b: ByteArray = readBytes(Int.MAX_VALUE)
        octetLength = b.size.toLong()
        if (octetLength > Constants.MAX_STRING_LENGTH) throw getBinaryTooLong(octetLength)!!
        return b
    }

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder {
        if (sqlFlags and HasSQL.REPLACE_LOBS_FOR_TRACE != 0
                && (lobData !is LobDataInMemory || charLength > SysProperties.MAX_TRACE_DATA_LENGTH)) {
            builder.append("SPACE(").append(charLength)
            val lobDb = lobData as LobDataDatabase
            builder.append(" /* table: ")
                    .append(lobDb.tableId)
                    .append(" id: ")
                    .append(lobDb.lobId)
                    .append(" */)")
        } else {
            if (sqlFlags and (HasSQL.REPLACE_LOBS_FOR_TRACE or HasSQL.NO_CASTS) == 0) {
                StringUtils.quoteStringSQL(builder.append("CAST("), getString())
                        .append(" AS CHARACTER LARGE OBJECT(")
                        .append(charLength)
                        .append("))")
            } else {
                StringUtils.quoteStringSQL(builder, getString())
            }
        }
        return builder
    }

    override fun getValueType(): Int = CLOB

    override fun getString(): String = when {
        charLength > Constants.MAX_STRING_LENGTH -> throw getStringTooLong(charLength)!!
        lobData is LobDataInMemory -> String(lobData.small, StandardCharsets.UTF_8)
        else -> readString(charLength.toInt())
    }

    override fun compareTypeSafe(v: Value, mode: CompareMode?, provider: CastDataProvider?): Int {
        if (v === this) return 0

        val v2 = v as ValueClob
        val lobData = lobData
        val lobData2 = v2.lobData

        if (lobData.javaClass != lobData2.javaClass) return ValueClob.compare(this, v2)

        if (lobData is LobDataInMemory) return Integer.signum(getString().compareTo(v2.getString()))
        if (lobData is LobDataDatabase && lobData.lobId == (lobData2 as LobDataDatabase).lobId) return 0
        if (lobData is LobDataFetchOnDemand && lobData.lobId == (lobData2 as LobDataFetchOnDemand).lobId) return 0

        return compare(this, v2)
    }

    /**
     * Convert the precision to the requested value.
     *
     * @param precision
     * the new precision
     * @return the truncated or this value
     */
    fun convertPrecision(precision: Long): ValueClob {
        if (charLength <= precision) return this

        val handler = lobData.getDataHandler()
        return if (handler != null) {
            createTempClob(getReader()!!, precision, handler)
        } else {
            try {
                createSmall(IOUtils.readStringAndClose(getReader()!!, MathUtils.convertLongToInt(precision)))
            } catch (e: IOException) {
                throw DbException.convertIOException(e, null)
            }
        }
    }
}