package org.h2.store

import org.h2.engine.SessionRemote
import org.h2.value.ValueBlob
import org.h2.value.ValueClob
import org.h2.value.ValueLob
import java.io.IOException
import java.io.InputStream
import java.io.Reader

/**
 * This factory creates in-memory objects and temporary files. It is used on the client side.
 */
class LobStorageFrontend(private var sessionRemote: SessionRemote) : LobStorageInterface {

    companion object {
        /**
         * The table id for session variables (LOBs not assigned to a table).
         */
        const val TABLE_ID_SESSION_VARIABLE = -1

        /**
         * The table id for temporary objects (not assigned to any object).
         */
        const val TABLE_TEMP = -2

        /**
         * The table id for result sets.
         */
        const val TABLE_RESULT = -3

        private val sessionRemote: SessionRemote? = null
    }

    override fun removeLob(lob: ValueLob?) {
        // not stored in the database
    }

    @Throws(IOException::class)
    override fun getInputStream(lobId: Long, byteCount: Long): InputStream? {
        // this method is only implemented on the server side of a TCP connection
        throw IllegalStateException()
    }

    @Throws(IOException::class)
    override fun getInputStream(lobId: Long, tableId: Int, byteCount: Long): InputStream? {
        // this method is only implemented on the server side of a TCP
        // connection
        throw IllegalStateException()
    }

    override fun isReadOnly(): Boolean = false

    override fun copyLob(old: ValueLob?, tableId: Int): ValueLob = throw UnsupportedOperationException()

    override fun removeAllForTable(tableId: Int): Unit = throw UnsupportedOperationException()

    override fun createBlob(inputStream: InputStream?, maxLength: Long): ValueBlob {
        // need to use a temp file, because the input stream could come from
        // the same database, which would create a weird situation (trying
        // to read a block while writing something)
        return ValueBlob.createTempBlob(inputStream!!, maxLength, sessionRemote)
    }

    /**
     * Create a CLOB object.
     *
     * @param reader the reader
     * @param maxLength the maximum length (-1 if not known)
     * @return the LOB
     */
    override fun createClob(reader: Reader?, maxLength: Long): ValueClob? {
        // need to use a temp file, because the input stream could come from
        // the same database, which would create a weird situation (trying
        // to read a block while writing something)
        return ValueClob.createTempClob(reader, maxLength, sessionRemote)
    }
}