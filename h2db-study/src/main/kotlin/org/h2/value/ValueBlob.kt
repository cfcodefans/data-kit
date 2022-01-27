package org.h2.value

import org.h2.api.ErrorCode
import org.h2.engine.CastDataProvider
import org.h2.engine.Constants
import org.h2.engine.SysProperties
import org.h2.message.DbException
import org.h2.store.DataHandler
import org.h2.store.FileStore
import org.h2.store.FileStoreOutputStream
import org.h2.util.Bits
import org.h2.util.HasSQL
import org.h2.util.IOUtils
import org.h2.util.MathUtils
import org.h2.util.StringUtils
import org.h2.util.Utils
import org.h2.value.lob.LobData
import org.h2.value.lob.LobDataDatabase
import org.h2.value.lob.LobDataFetchOnDemand
import org.h2.value.lob.LobDataFile
import org.h2.value.lob.LobDataInMemory
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * Implementation of the BINARY LARGE OBJECT data type.
 */
class ValueBlob(lobData: LobData, octetLength: Long) : ValueLob(
        lobData = lobData,
        octetLength = octetLength,
        charLength = -1) {

    companion object {
        /**
         * Creates a small BLOB value that can be stored in the row directly.
         *
         * @param data the data
         * @return the BLOB
         */
        fun createSmall(data: ByteArray): ValueBlob = ValueBlob(LobDataInMemory(data), data.size.toLong())

        /**
         * Create a BLOB in a temporary file.
         */
        @Throws(IOException::class)
        private fun createTemporary(handler: DataHandler, buff: ByteArray, len: Int, `in`: InputStream, remaining: Long): ValueBlob {
            var len = len
            var remaining = remaining

            val fileName = createTempLobFileName(handler)
            val tempFile: FileStore = handler.openFile(fileName!!, "rw", false).apply { autoDelete() }

            var tmpPrecision: Long = 0
            FileStoreOutputStream(tempFile, null).use { out ->
                while (true) {
                    tmpPrecision += len.toLong()
                    out.write(buff, 0, len)
                    remaining -= len.toLong()
                    if (remaining <= 0) break

                    len = `in`.readNBytes(buff, 0, getBufferSize(handler, remaining))
                    if (len <= 0) break
                }
            }
            return ValueBlob(LobDataFile(handler, fileName, tempFile), tmpPrecision)
        }

        /**
         * Create a temporary BLOB value from a stream.
         *
         * @param in the input stream
         * @param length the number of characters to read, or -1 for no limit
         * @param handler the data handler
         * @return the lob value
         */
        fun createTempBlob(`in`: InputStream, length: Long, handler: DataHandler): ValueBlob = try {
            val remaining = if (length >= 0 && length < Long.MAX_VALUE) length else Long.MAX_VALUE

            var len = getBufferSize(handler, remaining)
            val buff: ByteArray
            if (len >= Int.MAX_VALUE) {
                buff = IOUtils.readBytesAndClose(`in`, -1)
                len = buff.size
            } else {
                buff = Utils.newBytes(len)
                len = `in`.readNBytes(buff, 0, len)
            }
            if (len <= handler.getMaxLengthInplaceLob()) {
                createSmall(Utils.copyBytes(buff, len))
            } else {
                createTemporary(handler, buff, len, `in`, remaining)
            }
        } catch (e: IOException) {
            throw DbException.convertIOException(e, null)
        }

        /**
         * Compares two BLOB values directly.
         *
         * @param v1 first BLOB value
         * @param v2 second BLOB value
         * @return result of comparison
         */
        private fun compare(v1: ValueBlob, v2: ValueBlob): Int {
            var minPrec = v1.octetLength.coerceAtMost(v2.octetLength)
            try {
                v1.getInputStream()?.use { is1 ->
                    v2.getInputStream()!!.use { is2 ->
                        val buf1 = ByteArray(BLOCK_COMPARISON_SIZE)
                        val buf2 = ByteArray(BLOCK_COMPARISON_SIZE)
                        while (minPrec >= BLOCK_COMPARISON_SIZE) {
                            if (IOUtils.readFully(is1, buf1, BLOCK_COMPARISON_SIZE) != BLOCK_COMPARISON_SIZE
                                    || IOUtils.readFully(is2, buf2, BLOCK_COMPARISON_SIZE) != BLOCK_COMPARISON_SIZE) {
                                throw DbException.getUnsupportedException("Invalid LOB")
                            }
                            val cmp = Bits.compareNotNullUnsigned(buf1, buf2)
                            if (cmp != 0) return cmp
                            minPrec -= BLOCK_COMPARISON_SIZE.toLong()
                        }
                        while (true) {
                            val c1 = is1!!.read()
                            val c2 = is2!!.read()
                            if (c1 < 0) return if (c2 < 0) 0 else -1
                            if (c2 < 0) return 1
                            if (c1 != c2) return if (c1 and 0xFF < c2 and 0xFF) -1 else 1
                        }
                    }
                }
            } catch (ex: IOException) {
                throw DbException.convert(ex)
            }
            return 0
        }

        fun Value.convertToBlob(targetType: TypeInfo, conversionMode: Int, column: Any?): ValueBlob {
            val v: ValueBlob = getValueType().let { vt ->
                if (vt == BLOB) return@let (this as ValueBlob)

                if (vt == CLOB) {
                    val handler = (this as ValueLob).lobData.getDataHandler()
                    if (handler != null) {
                        return@let handler.getLobStorage().createBlob(getInputStream(), -1)
                    }
                }

                return@let try {
                    ValueBlob.createSmall(getBytesNoCopy())
                } catch (e: DbException) {
                    throw if (e.getErrorCode() == ErrorCode.DATA_CONVERSION_ERROR_1) getDataConversionError(BLOB) else e
                }
            }

            if (conversionMode == CONVERT_TO) return v
            if (conversionMode == CAST_TO) return v.convertPrecision(targetType.precision)
            if (v.octetLength() > targetType.precision) throw v.getValueTooLongException(targetType, column)

            return v
        }
    }


    override fun compareTypeSafe(v: Value, mode: CompareMode?, provider: CastDataProvider?): Int {
        if (v === this) return 0

        val v2 = v as ValueBlob
        val lobData: LobData = this.lobData
        val lobData2 = v2.lobData
        if (lobData.javaClass != lobData2.javaClass) return compare(this, v2)

        if (lobData is LobDataInMemory) {
            return Bits.compareNotNullUnsigned(lobData.small, (lobData2 as LobDataInMemory).small)
        }
        if (lobData is LobDataDatabase) {
            if (lobData.lobId == (lobData2 as LobDataDatabase).lobId) return 0
        } else if (lobData is LobDataFetchOnDemand) {
            if (lobData.lobId == (lobData2 as LobDataFetchOnDemand).lobId) return 0
        }
        return compare(this, v2)
    }

    override fun getValueType(): Int = BLOB

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder {
        if (sqlFlags and HasSQL.REPLACE_LOBS_FOR_TRACE != 0 && (lobData !is LobDataInMemory || octetLength > SysProperties.MAX_TRACE_DATA_LENGTH)) {
            builder.append("CAST(REPEAT(CHAR(0), ").append(octetLength).append(") AS BINARY VARYING")
            val lobDb = lobData as LobDataDatabase
            builder.append(" /* table: ").append(lobDb.tableId).append(" id: ").append(lobDb.lobId).append(" */)")
        } else {
            if (sqlFlags and (HasSQL.REPLACE_LOBS_FOR_TRACE or HasSQL.NO_CASTS) == 0) {
                builder.append("CAST(X'")
                StringUtils.convertBytesToHex(builder, getBytesNoCopy()!!)!!
                        .append("' AS BINARY LARGE OBJECT(")
                        .append(octetLength).append("))")
            } else {
                builder.append("X'")
                StringUtils.convertBytesToHex(builder, getBytesNoCopy()!!)!!
                        .append('\'')
            }
        }
        return builder
    }

    override fun charLength(): Long {
        if (charLength >= 0L) return charLength

        if (lobData is LobDataInMemory) {
            charLength = String(lobData.small, StandardCharsets.UTF_8).length.toLong()
            return charLength
        }

        var p = 0L
        try {
            getReader()!!.use { r ->
                while (true) {
                    p += r.skip(Long.MAX_VALUE)
                    if (r.read() < 0) break
                    p++
                }
            }
        } catch (e: IOException) {
            throw DbException.convertIOException(e, null)
        }

        charLength = p
        return p
    }

    /**
     * Convert the precision to the requested value.
     *
     * @param precision
     * the new precision
     * @return the truncated or this value
     */
    fun convertPrecision(precision: Long): ValueBlob {
        if (octetLength <= precision) return this

        return lobData.getDataHandler()?.let { handler -> createTempBlob(getInputStream()!!, precision, handler) }
                ?: try {
                    createSmall(IOUtils.readBytesAndClose(getInputStream()!!, MathUtils.convertLongToInt(precision)))
                } catch (e: IOException) {
                    throw DbException.convertIOException(e, null)
                }
    }

    override fun copy(database: DataHandler, tableId: Int): ValueLob {
        return when (lobData) {
            is LobDataInMemory -> {
                if (lobData.small.size > database.getMaxLengthInplaceLob()) {
                    val v = database.getLobStorage().createBlob(getInputStream(), octetLength)
                    val v2 = v.copy(database, tableId)
                    v.remove()
                    return v2
                }
                this
            }
            is LobDataDatabase -> database.getLobStorage().copyLob(this, tableId)
            else -> throw UnsupportedOperationException()
        }
    }

    override fun getBytesInternal(): ByteArray {
        if (octetLength > Constants.MAX_STRING_LENGTH) throw getBinaryTooLong(octetLength)!!
        return readBytes(octetLength.toInt())
    }

    override fun getString(): String {
        if (charLength >= 0L) {
            if (charLength > Constants.MAX_STRING_LENGTH) throw getStringTooLong(charLength)!!
            return readString(charLength.toInt())
        }
        // 1 Java character may be encoded with up to 3 bytes
        if (octetLength > Constants.MAX_STRING_LENGTH * 3) throw getStringTooLong(charLength())!!

        val s: String = if (lobData is LobDataInMemory)
            String(lobData.small, StandardCharsets.UTF_8)
        else
            readString(Int.MAX_VALUE)

        charLength = s.length.toLong()
        if (charLength > Constants.MAX_STRING_LENGTH) throw getStringTooLong(charLength)!!
        return s
    }

    override fun octetLength(): Long = octetLength
}