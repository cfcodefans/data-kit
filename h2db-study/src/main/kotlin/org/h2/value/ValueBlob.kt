package org.h2.value

import org.h2.message.DbException
import org.h2.store.DataHandler
import org.h2.store.FileStore
import org.h2.store.FileStoreOutputStream
import org.h2.util.IOUtils
import org.h2.util.Utils
import org.h2.value.lob.LobData
import org.h2.value.lob.LobDataFile
import org.h2.value.lob.LobDataInMemory
import java.io.IOException
import java.io.InputStream

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
        fun createSmall(data: ByteArray): ValueBlob? = ValueBlob(LobDataInMemory(data), data.size.toLong())

        /**
         * Create a BLOB in a temporary file.
         */
        @Throws(IOException::class)
        private fun createTemporary(handler: DataHandler, buff: ByteArray, len: Int, `in`: InputStream, remaining: Long): ValueBlob? {
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
                    len = getBufferSize(handler, remaining)
                    len = `in`.readNBytes(buff, 0, len)
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
        fun createTempBlob(`in`: InputStream, length: Long, handler: DataHandler): ValueBlob? {
            return try {
                var remaining = if (length >= 0 && length < Long.MAX_VALUE) length else Long.MAX_VALUE

                var len = getBufferSize(handler, remaining)
                val buff: ByteArray
                if (len >= Int.MAX_VALUE) {
                    buff = IOUtils.readBytesAndClose(`in`!!, -1)
                    len = buff.size
                } else {
                    buff = Utils.newBytes(len)
                    len = `in`.readNBytes(buff, 0, len)
                }
                if (len <= handler.getMaxLengthInplaceLob()) {
                    createSmall(Utils.copyBytes(buff, len))
                } else ValueBlob.createTemporary(handler, buff, len, `in`, remaining)
            } catch (e: IOException) {
                throw DbException.convertIOException(e, null)
            }
        }
    }
}