package org.h2.store

import org.h2.message.DbException
import org.h2.util.SmallLRUCache
import org.h2.util.TempFileDeleter
import org.h2.value.CompareMode

/**
 * A data handler contains a number of callback methods, mostly related to CLOB
 * and BLOB handling. The most important implementing class is a database.
 *
 */
interface DataHandler {
    /**
     * Get the database path.
     * @return the database path
     */
    fun getDatabasePath(): String

    /**
     * Open a file at the given location.
     *
     * @param name the file name
     * @param mode the mode
     * @param mustExist whether the file must already exist
     * @return the file
     */
    fun openFile(name: String, mode: String, mustExist: Boolean): FileStore

    /**
     * Check if the simulated power failure occurred.
     * This call will decrement the countdown.
     *
     * @throws DbException if the simulated power failure occurred
     */
    @Throws(DbException::class)
    fun checkPowerOff(): Unit

    /**
     *  Check if writing is allowed.
     *  @throws DbException if it is not allowed
     */
    @Throws(DbException::class)
    fun checkWritingAllowed(): Unit

    /**
     * Get the maximum length of a in-place large object
     *
     * @return the maximum size
     */
    fun getMaxLengthInplaceLob(): Int

    /**
     * Get the temp file deleter mechanism.
     * @return the temp file deleter
     */
    fun getTempFileDeleter(): TempFileDeleter

    /**
     * Get the synchronization object for lob operations.
     * @return the synchronization object
     */
    fun getLobSyncObject(): Any?

    /**
     * Get the lob file list cache if it is used.
     *
     * @return the cache or null
     */
    fun getLobFileListCache(): SmallLRUCache<String, Array<String>>?

    /**
     * Get the lob storage mechanism to use.
     * @return the lob storage mechanism
     */
    fun getLobStorage(): LobStorageInterface

    /**
     * Read from a lob.
     *
     * @param lobId the lob id
     * @param hmac the message authentication code
     * @param offset the offset within the lob
     * @param buff the target buffer
     * @param off the offset within the target buffer
     * @param length the number of bytes to read
     * @return the number of bytes read
     */
    fun readLob(lobId: Long, hmac: ByteArray?, offset: Long, buff: ByteArray?, off: Int, length: Int): Int

    /**
     * Return compare mode.
     * @return Compare mode.
     */
    fun getCompareMode(): CompareMode?
}