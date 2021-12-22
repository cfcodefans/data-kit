package org.h2.store

import org.h2.engine.Constants
import org.h2.message.DbException
import org.h2.security.SecureFileStore
import org.h2.store.fs.FileUtils
import java.io.IOException
import java.lang.ref.Reference
import java.nio.channels.FileChannel

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
open class FileStore(var handler: DataHandler, val name: String, _mode: String) {
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
                 keyIterations: Int): FileStore? = cipher?.let {
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
        fun open(handler: DataHandler?, name: String?, mode: String?): FileStore? {
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
                 key: ByteArray?): FileStore? = open(handler, name, mode, cipher, key, Constants.ENCRYPTION_KEY_HASH_ITERATIONS)
    }

    private lateinit var mode: String
    private var file: FileChannel? = null
    private var fileLength: Long = 0
    private var autoDeleteReference: Reference<*>? = null

    init {
        try {
            val exists = FileUtils.exists(name)
            if (exists && !FileUtils.canWrite(name)) {
                this.mode = "r"
            } else {
                FileUtils.createDirectories(FileUtils.getParent(name))
            }
            file = FileUtils.open(name, _mode)
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
        autoDeleteReference = handler.getTempFileDeleter().addFile(name, this)
    }

    /**
     * No longer automatically delete the file once it is no longer in use.
     */
    open fun stopAutoDelete() {
        handler.getTempFileDeleter().stopAutoDelete(autoDeleteReference, name)
        autoDeleteReference = null
    }
}