package org.h2.store.fs

import org.h2.util.MathUtils
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentHashMap

/**
 * A path to a file, It similar to the Java 7 <code>java.nio.file.Path</code>,
 * but simpler, and works with older versions of java. It also implements the
 * relevant methods found in <code>java.nio.file.FileSystem</code> and
 * <code>FileSystems</code>
 */
abstract class FilePath {
    companion object {
        @JvmStatic
        lateinit var defaultProvider: FilePath

        @JvmStatic
        lateinit var providers: ConcurrentHashMap<String, FilePath>

        /**
         * The prefix for temporary files.
         */
        @JvmStatic
        lateinit var tempRandom: String

        @JvmStatic
        var tempSequence: Long = 0L

        @JvmStatic
        @Synchronized
        fun getNextTempFileNamePart(newRandom: Boolean): String {
            if (newRandom || tempRandom == null)
                tempRandom = MathUtils.randomInt(Int.MAX_VALUE).toString() + "."
            return tempRandom + tempSequence++
        }

        @JvmStatic
        private fun registerDefaultProviders() {
            if (providers == null || defaultProvider == null) {
                val map: ConcurrentHashMap<String, FilePath> = ConcurrentHashMap()
                for (c in arrayOf("org.h2.store.fs.FilePathDisk",
                        "org.h2.store.fs.FilePathMem",
                        "org.h2.store.fs.FilePathMemLZF",
                        "org.h2.store.fs.FilePathNioMem",
                        "org.h2.store.fs.FilePathNioMemLZF",
                        "org.h2.store.fs.FilePathSplit",
                        "org.h2.store.fs.FilePathNio",
                        "org.h2.store.fs.FilePathNioMapped",
                        "org.h2.store.fs.FilePathAsync",
                        "org.h2.store.fs.FilePathZip",
                        "org.h2.store.fs.FilePathRetryOnInterrupt")) {
                    try {
                        val p: FilePath = Class.forName(c).getDeclaredConstructor().newInstance().cast()
                        map[p.scheme] = p
                        if (defaultProvider == null) defaultProvider = p
                    } catch (e: Exception) {
                    }
                }
                providers = map
            }
        }

        /**
         * Get the file path object for the given path.
         * Windows-style '\' is replaced with '/'.
         * @param path the path
         * @return the file path object
         */
        @JvmStatic
        fun get(path: String): FilePath {
            registerDefaultProviders()
            val _path: String = path.replace('\\', '/')
            val index: Int = _path.indexOf(':')
            if (index < 2) {
                // use the default provider if no prefix or
                // only a single character (driver name)
                return defaultProvider.getPath(path)
            }
            val scheme: String = path.substring(0, index)
            val p: FilePath = providers[scheme] ?: defaultProvider
            return p.getPath(path)
        }

        /**
         * Register a file provider.
         * @param provider the file provider
         */
        @JvmStatic
        fun register(provider: FilePath) {
            registerDefaultProviders()
            providers[provider.scheme] = provider
        }

        /**
         * Unregister a file provider.
         * @param provider the file provider
         */
        @JvmStatic
        fun unregister(provider: FilePath) {
            registerDefaultProviders()
            providers.remove(provider.scheme)
        }
    }

    /**
     * Get the size of a file in bytes
     * @return the size in bytes
     */
    abstract fun size(): Long

    /**
     * Rename a file if this is allowed.
     * @param newName the new fully qualified file name
     * @param atomicReplace whether the move should be atomic, and the target
     *                      file should be replaced if it exists and replacing is possible
     */
    abstract fun moveTo(newName: FilePath, atomicReplace: Boolean)

    /**
     * Create a new file.
     * @return true if creating was successful
     */
    abstract fun createFile(): Boolean

    /**
     * Checks if a file exists.
     * @return true if it exists
     */
    abstract fun exists(): Boolean

    /**
     * Delete a file or directory if it exists.
     * Directories may only be deleted if they are empty.
     */
    abstract fun delete()

    /**
     * List the files and directories in the given directory.
     * @return the list of fully qualified file names
     */
    abstract fun newDirectoryStream(): List<FilePath?>

    /**
     * Normalize a file name.
     * @return the normalized file name
     */
    abstract fun toRealPath(): FilePath

    /**
     * Get the parent directory of a file or directory
     * @return the parent directory name
     */
    abstract fun getParent(): FilePath?

    /**
     * Check if it is a file or a directory.
     * @return true if it is a directory
     */
    abstract fun isDirectory(): Boolean

    /**
     *  Check if the file name includes a path.
     *  @return true if the file name is absolute
     */
    abstract fun isAbsolute(): Boolean

    /**
     * Get the last modified date of a file
     * @return the last modified date
     */
    abstract fun lastModified(): Long

    /**
     *  Check if the file is writable
     *  @return if the file is writable
     */
    abstract fun canWrite(): Boolean

    /**
     * The complete path (which may be absolute or relative, depending on the file system).
     */
    open lateinit var name: String

    /**
     * Get the schema (prefix) for this file provider.
     * This is similar to
     * <code>java.nio.file.spi.FileSystemProvider.getSchema</code>
     * @return the schema
     */
    abstract val scheme: String

    /**
     * Convert a file to a path. This is similar to
     * <code>java.nio.file.spi.FileSystemProvider.getPath</code>, but may
     * return an object even if the scheme doesn't match in case of the
     * default file provider.
     * @param path the path
     * @return the file path object
     */
    abstract fun getPath(path: String): FilePath

    /**
     * Create a directory (all required parent directories already exist).
     */
    abstract fun createDirectory()

    /**
     * Get the file or directory name (the last element of the path).
     * @return the last element of the path
     */
    val fileName: String
        get() {
            val idx: Int = Math.max(name.indexOf(':'), name.lastIndexOf('/'))
            return if (idx < 0) name else name.substring(idx + 1)
        }

    /**
     * Create an output stream to write into the file.
     * @param append if true, the file will grow, if false, the file will be truncated first
     * @return the output stream
     * @throws IOException If an I/O error occurs
     */
    @Throws(IOException::class)
    abstract fun newOutputStream(append: Boolean): OutputStream

    /**
     * Open a random access file object.
     * @param mode the access mode. Supported are r, rw, rws, rwd
     * @return the file object
     * @throws IOException If an I/O error occurs
     */
    @Throws(IOException::class)
    abstract fun open(mode: String): FileChannel

    /**
     * Create an input stream to read from the file.
     * @return the input stream
     * @throws IOException If an I/O error occurs
     */
    @Throws(IOException::class)
    abstract fun newInputStream(): InputStream

    /**
     * Disable the ability to write.
     * @return true if the call was successful
     */
    abstract fun setReadOnly(): Boolean

    /**
     * Create a new temporary file.
     * @param suffix the suffix
     * @param inTempDir if the file should be stored in the temporary directory
     * @return the name of the created file
     */
    @Throws(IOException::class)
    open fun createTempFile(suffix: String, inTempDir: Boolean): FilePath? {
        while (true) {
            val p: FilePath = getPath(name + getNextTempFileNamePart(false) + suffix)
            if (p.exists() || !p.createFile()) {
                //in theory, the random number could collide
                getNextTempFileNamePart(true)
                continue
            }
            p.open("rw").close()
            return p
        }
    }

    /**
     * Get the unwrapped file name (without wrapper prefixes if wrapping /
     * delegating file systems are used
     */
    fun unwrap(): FilePath = this

    override fun toString(): String = name
}