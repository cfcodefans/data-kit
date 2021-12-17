package org.h2.value

import org.h2.engine.Constants
import org.h2.engine.SysProperties
import org.h2.message.DbException
import org.h2.store.DataHandler
import org.h2.store.LobStorageFrontend
import org.h2.store.LobStorageInterface
import org.h2.store.RangeInputStream
import org.h2.store.fs.FileUtils
import org.h2.util.IOUtils
import org.h2.util.MathUtils
import org.h2.util.StringUtils
import org.h2.util.Utils
import org.h2.value.lob.LobData
import org.h2.value.lob.LobDataDatabase
import org.h2.value.lob.LobDataInMemory
import java.io.IOException
import java.io.InputStream
import java.io.Reader

/**
 * A implementation of the BINARY LARGE OBJECT and CHARACTER LARGE OBJECT data
 * types. Small objects are kept in memory and stored in the record. Large
 * objects are either stored in the database, or in temporary files.
 *
 * @param octetLength Length in bytes
 * @param charLength Length in characters
 */
abstract class ValueLob(val lobData: LobData, var octetLength: Long, var charLength: Long) : Value() {
    /**
     * Cache the hashCode because it can be expensive to compute.
     */
    private var hash: Int = 0

    companion object {
        const val BLOCK_COMPARISON_SIZE = 512

        private fun rangeCheckUnknown(zeroBasedOffset: Long, length: Long) {
            if (zeroBasedOffset < 0) throw DbException.getInvalidValueException("offset", zeroBasedOffset + 1)
            if (length < 0) throw DbException.getInvalidValueException("length", length)
        }


        /**
         * Create an input stream that is s subset of the given stream.
         *
         * @param inputStream the source input stream
         * @param oneBasedOffset the offset (1 means no offset)
         * @param length the length of the result, in bytes
         * @param dataSize the length of the input, in bytes
         * @return the smaller input stream
         */
        protected fun rangeInputStream(inputStream: InputStream,
                                       oneBasedOffset: Long,
                                       length: Long,
                                       dataSize: Long): InputStream? {
            if (dataSize > 0) {
                rangeCheck(oneBasedOffset - 1, length, dataSize)
            } else {
                rangeCheckUnknown(oneBasedOffset - 1, length)
            }
            return try {
                RangeInputStream(inputStream, oneBasedOffset - 1, length)
            } catch (e: IOException) {
                throw DbException.getInvalidValueException("offset", oneBasedOffset)
            }
        }

        /**
         * Create file name for temporary LOB storage
         * @param handler to get path from
         * @return full path and name of the created file
         * @throws IOException if file creation fails
         */
        @Throws(IOException::class)
        fun createTempLobFileName(handler: DataHandler): String? {
            var path = handler.getDatabasePath().ifEmpty { SysProperties.PREFIX_TEMP_FILE }
            return FileUtils.createTempFile(path, Constants.SUFFIX_TEMP_FILE, true)
        }

        fun getBufferSize(handler: DataHandler, remaining: Long): Int {
            var remaining = remaining
            if (remaining < 0 || remaining > Int.MAX_VALUE) {
                remaining = Int.MAX_VALUE.toLong()
            }
            val inplace: Int = handler.getMaxLengthInplaceLob()
            var m = Constants.IO_BUFFER_SIZE.toLong()
            if (m < remaining && m <= inplace) {
                // using "1L" to force long arithmetic because
                // inplace could be Integer.MAX_VALUE
                m = remaining.coerceAtMost(inplace + 1L)
                // the buffer size must be bigger than the inplace lob, otherwise we
                // can't know if it must be stored in-place or not
                m = MathUtils.roundUpLong(m, Constants.IO_BUFFER_SIZE.toLong())
            }
            m = remaining.coerceAtMost(m)
            m = MathUtils.convertLongToInt(m).toLong()
            if (m < 0) m = Int.MAX_VALUE.toLong()

            return m.toInt()
        }
    }

    /**
     * Check if this value is linked to a specific table. For values that are
     * kept fully in memory, this method returns false.
     *
     * @return true if it is
     */
    open fun isLinkedToTable(): Boolean = lobData.isLinkedToTable()

    /**
     * Remove the underlying resource, if any. For values that are kept fully in
     * memory this method has no effect.
     */
    open fun remove() = lobData.remove(this)

    /**
     * Copy a large value, to be used in the given table. For values that are
     * kept fully in memory this method has no effect.
     *
     * @param database the data handler
     * @param tableId the table where this object is used
     * @return the new value or itself
     */
    abstract fun copy(database: DataHandler, tableId: Int): ValueLob?

    override var type: TypeInfo? = null
        get() {
            if (field == null) {
                val valueType = getValueType()
                field = TypeInfo(valueType = valueType,
                        precision = if (valueType == CLOB) charLength else octetLength,
                        scale = 0,
                        extTypeInfo = null)
            }
            return field
        }

    open fun getStringTooLong(precision: Long): DbException? {
        return DbException.getValueTooLongException("CHARACTER VARYING", readString(81), precision)
    }

    open fun readString(len: Int): String = try {
        IOUtils.readStringAndClose(getReader()!!, len)
    } catch (e: IOException) {
        throw DbException.convertIOException(e, toString())
    }

    override fun getReader(): Reader? = IOUtils.getReader(getInputStream())

    private fun getSmall(): ByteArray {
        val small = (lobData as LobDataInMemory).getSmall()
        val p = small.size
        if (p > Constants.MAX_STRING_LENGTH) {
            throw DbException.getValueTooLongException("BINARY VARYING", StringUtils.convertBytesToHex(small, 41), p.toLong())
        }
        return small
    }

    abstract fun getBytesInternal(): ByteArray

    override fun getBytes(): ByteArray = if (lobData is LobDataInMemory) Utils.cloneByteArray(getSmall())!! else getBytesInternal()

    open fun getBinaryTooLong(precision: Long): DbException? {
        return DbException.getValueTooLongException("BINARY VARYING", StringUtils.convertBytesToHex(readBytes(41)), precision)
    }

    open fun readBytes(len: Int): ByteArray = try {
        IOUtils.readBytesAndClose(getInputStream()!!, len)
    } catch (e: IOException) {
        throw DbException.convertIOException(e, toString())
    }

    override fun hashCode(): Int {
        if (hash != 0) return hash

        val valueType = getValueType()
        val length = if (valueType == CLOB) charLength else octetLength
        if (length > 4096) {
            // TODO: should calculate the hash code when saving, and store
            // it in the database file
            return (length xor (length ushr 32)).toInt()
        }
        hash = Utils.getByteArrayHash(getBytesNoCopy()!!)
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ValueLob) return false
        return if (hashCode() != other.hashCode()) false else compareTypeSafe(other as Value?, null, null) == 0
    }

    override fun getMemory(): Int = lobData.getMemory()

    /**
     * Create an independent copy of this value, that will be bound to a result.
     *
     * @return the value (this for small objects)
     */
    open fun copyToResult(): ValueLob? {
        if (lobData !is LobDataDatabase) return this

        val s: LobStorageInterface = lobData.getDataHandler()?.getLobStorage()!!
        return if (s.isReadOnly()) this else return s.copyLob(this, LobStorageFrontend.TABLE_RESULT)
    }
}