package org.h2.store

import org.h2.engine.Constants
import org.h2.engine.SysProperties
import org.h2.message.DbException
import org.h2.security.SecureFileStore
import org.h2.store.fs.FileUtils
import java.io.IOException
import java.lang.ref.Reference
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.charset.StandardCharsets

/**
 * This class is an abstraction of a random access file.
 * Each file contains a magic header, and reading / writing is done in blocks.
 * See also {@link SecureFileStore}
 */
/**
 * Create a new file using the given settings.
 *
 * @param handler the callback object
 * @param name the file name
 * @param _mode the access mode ("r", "rw", "rws", "rwd")
 */
open class FileStore(var handler: DataHandler?, var name: String?, _mode: String) {
    companion object {
        /**
         * The size of the file header in bytes.
         */
        const val HEADER_LENGTH = 3 * Constants.FILE_BLOCK_SIZE

        /**
         * The magic file header.
         */
        private val HEADER = """${"-- H2 0.5/B --      ".substring(0, Constants.FILE_BLOCK_SIZE - 1)}
            
            """.trimIndent()

        private var ASSERT = false

        init {
            var a = false
            // Intentional side-effect
            assert(true.also { a = it })
            ASSERT = a
        }


        /**
         * Open an encrypted file store with the given settings.
         *
         * @param handler the data handler
         * @param name the file name
         * @param mode the access mode (r, rw, rws, rwd)
         * @param cipher the name of the cipher algorithm
         * @param key the encryption key
         * @param keyIterations the number of iterations the key should be hashed
         * @return the created object
         */
        fun open(handler: DataHandler?,
                 name: String?,
                 mode: String?,
                 cipher: String?,
                 key: ByteArray?,
                 keyIterations: Int): FileStore = cipher?.let {
            SecureFileStore(handler, name, mode, it, key, keyIterations)
        } ?: FileStore(handler!!, name!!, mode!!)

        /**
         * Open a non encrypted file store with the given settings.
         *
         * @param handler the data handler
         * @param name the file name
         * @param mode the access mode (r, rw, rws, rwd)
         * @return the created object
         */
        fun open(handler: DataHandler?, name: String?, mode: String?): FileStore {
            return open(handler, name, mode, null, null, 0)
        }

        /**
         * Open an encrypted file store with the given settings.
         *
         * @param handler the data handler
         * @param name the file name
         * @param mode the access mode (r, rw, rws, rwd)
         * @param cipher the name of the cipher algorithm
         * @param key the encryption key
         * @return the created object
         */
        fun open(handler: DataHandler?,
                 name: String?,
                 mode: String?,
                 cipher: String?,
                 key: ByteArray?): FileStore = open(handler, name, mode, cipher, key, Constants.ENCRYPTION_KEY_HASH_ITERATIONS)

        private fun trace(method: String, fileName: String, o: Any) {
            if (SysProperties.TRACE_IO) {
                println("FileStore.$method $fileName $o")
            }
        }
    }

    private lateinit var mode: String
    private var file: FileChannel? = null
    private var fileLength: Long = 0
    private var autoDeleteReference: Reference<*>? = null

    init {
        try {
            val exists = FileUtils.exists(name!!)
            if (exists && !FileUtils.canWrite(name!!)) {
                this.mode = "r"
            } else {
                FileUtils.createDirectories(FileUtils.getParent(name!!))
            }
            file = FileUtils.open(name!!, _mode)
            if (exists) fileLength = file!!.size()
        } catch (e: IOException) {
            throw DbException.convertIOException(e, "name: $name mode: $_mode")
        }
    }

    /**
     * Automatically delete the file once it is no longer in use.
     */
    open fun autoDelete() {
        if (autoDeleteReference != null) return
        autoDeleteReference = handler?.getTempFileDeleter()?.addFile(name!!, this)
    }

    /**
     * No longer automatically delete the file once it is no longer in use.
     */
    open fun stopAutoDelete() {
        handler?.getTempFileDeleter()?.stopAutoDelete(autoDeleteReference, name!!)
        autoDeleteReference = null
    }

    /**
     * Generate the random salt bytes if required.
     *
     * @return the random salt or the magic
     */
    protected open fun generateSalt(): ByteArray? = HEADER.toByteArray(StandardCharsets.UTF_8)

    /**
     * Initialize the key using the given salt.
     *
     * @param salt the salt
     */
    protected open fun initKey(salt: ByteArray?) {
        // do nothing
    }

    var checkedWriting = true

    private fun checkWritingAllowed() {
        if (handler != null && checkedWriting) {
            handler!!.checkWritingAllowed()
        }
    }

    private fun checkPowerOff() {
        handler?.checkPowerOff()
    }

    /**
     * Close the file.
     */
    open fun close() {
        if (file == null) return

        try {
            trace("close", name!!, file!!)
            file!!.close()
        } catch (e: IOException) {
            throw DbException.convertIOException(e, name)
        } finally {
            file = null
        }
    }

    /**
     * Close the file without throwing any exceptions. Exceptions are simply
     * ignored.
     */
    open fun closeSilently() = kotlin.runCatching { close() }

    /**
     * Just close the file, without setting the reference to null. This method
     * is called when writing failed. The reference is not set to null so that
     * there are no NullPointerExceptions later on.
     */
    private fun closeFileSilently() = kotlin.runCatching { file!!.close() }

    /**
     * Close the file (ignoring exceptions) and delete the file.
     */
    open fun closeAndDeleteSilently() {
        if (file == null) return
        closeSilently()
        handler!!.getTempFileDeleter().deleteFile(autoDeleteReference, name)
        name = null
    }

    var filePos: Long = 0

    /**
     * Read a number of bytes.
     *
     * @param b the target buffer
     * @param off the offset
     * @param len the number of bytes to read
     */
    open fun readFully(b: ByteArray?, off: Int, len: Int) {
        if (len < 0 || len % Constants.FILE_BLOCK_SIZE != 0) {
            throw DbException.getInternalError("unaligned read $name len $len")
        }
        checkPowerOff()
        try {
            FileUtils.readFully(file!!, ByteBuffer.wrap(b, off, len))
        } catch (e: IOException) {
            throw DbException.convertIOException(e, name)
        }
        filePos += len.toLong()
    }

    /**
     * Go to the specified file location.
     *
     * @param pos the location
     */
    open fun seek(pos: Long) {
        if (pos % Constants.FILE_BLOCK_SIZE != 0L) {
            throw DbException.getInternalError("unaligned seek $name pos $pos")
        }
        try {
            if (pos != filePos) {
                file!!.position(pos)
                filePos = pos
            }
        } catch (e: IOException) {
            throw DbException.convertIOException(e, name)
        }
    }

    /**
     * Write a number of bytes.
     *
     * @param b the source buffer
     * @param off the offset
     * @param len the number of bytes to write
     */
    open fun write(b: ByteArray?, off: Int, len: Int) {
        if (len < 0 || len % Constants.FILE_BLOCK_SIZE != 0) {
            throw DbException.getInternalError("unaligned write $name len $len")
        }
        checkWritingAllowed()
        checkPowerOff()
        try {
            FileUtils.writeFully(file!!, ByteBuffer.wrap(b, off, len))
        } catch (e: IOException) {
            closeFileSilently()
            throw DbException.convertIOException(e, name)
        }
        filePos += len.toLong()
        fileLength = Math.max(filePos, fileLength)
    }

    /**
     * Re-open the file. The file pointer will be reset to the previous
     * location.
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    open fun openFile() {
        if (file != null) return
        file = FileUtils.open(name!!, mode)
        file!!.position(filePos)
    }

    private var lock: FileLock? = null

    /**
     * Try to lock the file.
     * @return true if successful
     */
    @Synchronized
    open fun tryLock(): Boolean = try {
        lock = file!!.tryLock()
        lock != null
    } catch (e: Exception) {
        // ignore OverlappingFileLockException
        false
    }

    /**
     * Release the file lock.
     */
    @Synchronized
    open fun releaseLock() {
        if (file == null || lock == null) return
        try {
            lock!!.release()
        } catch (e: java.lang.Exception) {
            // ignore
        }
        lock = null
    }
}