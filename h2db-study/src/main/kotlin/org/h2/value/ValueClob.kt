package org.h2.value

import org.h2.engine.CastDataProvider
import org.h2.engine.Constants
import org.h2.message.DbException
import org.h2.store.DataHandler
import org.h2.store.FileStoreOutputStream
import org.h2.store.RangeReader
import org.h2.util.IOUtils
import org.h2.value.lob.LobData
import org.h2.value.lob.LobDataDatabase
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
        fun createTempClob(reader: Reader?, length: Long, handler: DataHandler): ValueClob? {
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
                ValueClob.createTemporary(handler, reader, remaining)
            } catch (e: IOException) {
                throw DbException.convertIOException(e, null)
            }
        }

        /**
         * Create a CLOB in a temporary file.
         */
        @Throws(IOException::class)
        private fun createTemporary(handler: DataHandler, `in`: Reader, remaining: Long): ValueClob? {
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
    }

    override fun copy(database: DataHandler, tableId: Int): ValueLob? {
        return when (lobData) {
            is LobDataInMemory -> {
                val small = lobData.getSmall()
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
            octetLength = lobData.getSmall().size.toLong()
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
        TODO("Not yet implemented")
    }

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder {
        TODO("Not yet implemented")
    }

    override fun getValueType(): Int {
        TODO("Not yet implemented")
    }

    override fun getString(): String? {
        TODO("Not yet implemented")
    }

    override fun compareTypeSafe(v: Value?, mode: CompareMode?, provider: CastDataProvider?): Int {
        TODO("Not yet implemented")
    }
}