package org.h2.store

import org.h2.value.ValueBlob
import org.h2.value.ValueClob
import org.h2.value.ValueLob
import java.io.IOException
import java.io.InputStream
import java.io.Reader

/**
 * A mechanism to store and retrieve lob data.
 */
interface LobStorageInterface {
    /**
     * Create a CLOB object.
     *
     * @param reader the reader
     * @param maxLength the maximum length (-1 if not known)
     * @return the LOB
     */
    fun createClob(reader: Reader?, maxLength: Long): ValueClob?

    /**
     * Create a BLOB object.
     *
     * @param in the input stream
     * @param maxLength the maximum length (-1 if not known)
     * @return the LOB
     */
    fun createBlob(`in`: InputStream?, maxLength: Long): ValueBlob

    /**
     * Copy a lob.
     *
     * @param old the old lob
     * @param tableId the new table id
     * @return the new lob
     */
    fun copyLob(old: ValueLob?, tableId: Int): ValueLob

    /**
     * Get the input stream for the given lob, only called on server side of a TCP connection.
     *
     * @param lobId the lob id
     * @param byteCount the number of bytes to read, or -1 if not known
     * @return the stream
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun getInputStream(lobId: Long, byteCount: Long): InputStream?

    /**
     * Get the input stream for the given lob
     *
     * @param lobId the lob id
     * @param tableId the able id
     * @param byteCount the number of bytes to read, or -1 if not known
     * @return the stream
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun getInputStream(lobId: Long, tableId: Int, byteCount: Long): InputStream?

    /**
     * Delete a LOB (from the database, if it is stored there).
     *
     * @param lob the lob
     */
    fun removeLob(lob: ValueLob?)

    /**
     * Remove all LOBs for this table.
     *
     * @param tableId the table id
     */
    fun removeAllForTable(tableId: Int)

    /**
     * Whether the storage is read-only
     *
     * @return true if yes
     */
    fun isReadOnly(): Boolean
}