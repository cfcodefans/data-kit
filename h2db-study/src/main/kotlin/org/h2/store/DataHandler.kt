package org.h2.store

import org.h2.api.JavaObjectSerializer
import org.h2.message.DbException
import org.h2.util.TempFileDeleter

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
     *  Get the maximum length of a in-place large object
     *  @return the maximum size
     */
    fun getMaxLengthInplaceLog(): Int

    /**
     * Get the compression algorithm used for large objects.
     * @param type the data type (CLOB or BLOB)
     * @return the compression algorithm, or null
     */
    fun getLogCompressionAlgorithm(type: Int): String

    /**
     * Get the temp file deleter mechanism.
     *
     * @return the temp file deleter
     */
    fun getTempFileDeleter(): TempFileDeleter?

    /**
     * Get the synchronization object for lob operations.
     *
     * @return the synchronization object
     */
    fun getLobSyncObject(): Any

    /**
     * Return the serializer to be used for java objects being stored in
     * column of type OTHER.
     *
     * @return the serializer to be used for java objects being stored in
     * column of type OTHER
     */
    fun getJavaObjectSerializer(): JavaObjectSerializer?

    /**
     * Get the maximum length of a in-place large object
     *
     * @return the maximum size
     */
    fun getMaxLengthInplaceLob(): Int

    /**
     * Get the lob storage mechanism to use.
     *
     * @return the lob storage mechanism
     */
    fun getLobStorage(): LobStorageInterface
}