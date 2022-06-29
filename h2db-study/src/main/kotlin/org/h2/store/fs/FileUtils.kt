package org.h2.store.fs

import org.h2.store.fs.FilePath.Companion.get
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

object FileUtils {
    @JvmStatic
    fun exists(fileName: String): Boolean = get(fileName).exists()

    /**
     * Fully read from the file. This will read all remaining bytes,
     * or throw an EOFException if not successful
     * @param channel the file channel
     * @param dst the byte buffer
     */
    @Throws(IOException::class)
    @JvmStatic
    fun readFully(channel: FileChannel, dst: ByteBuffer) {
        do {
            if (channel.read(dst) < 0) {
                throw EOFException()
            }
        } while (dst.remaining() > 0)
    }

    /**
     * Fully write to the file. This will write all remaining bytes.
     * @param channel the file channel
     * @param src the byte buffer
     */
    @Throws(IOException::class)
    @JvmStatic
    fun writeFully(channel: FileChannel, src: ByteBuffer) {
        do {
            channel.write(src)
        } while (src.remaining() > 0)
    }

    @JvmStatic
    fun isDirectory(fileName: String): Boolean = get(fileName).isDirectory()

    /**
     * Open a random access file object.
     * This method is similar to Java 7
     * `java.nio.channels.FileChannel.open`.
     *
     * @param fileName the file name
     * @param mode the access mode. Supported are r, rw, rws, rwd
     * @return the file object
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun open(fileName: String, mode: String): FileChannel = get(fileName).open(mode)

    /**
     * Create a directory (all required parent directories must already exist).
     * This method is similar to Java7
     * <code>java.nio.file.Path.createDirectory</code>
     */
    @JvmStatic
    fun createDirectory(directoryName: String) = get(directoryName).createDirectory()

    /**
     * Create the directory and all required parent directories.
     * @param dir the directory name
     */
    @JvmStatic
    fun createDirectories(dir: String?) {
        if (dir == null) return
        if (exists(dir)) {
            if (!isDirectory(dir)) {
                createDirectory(dir)
            }
        } else {
            createDirectories(getParent(dir))
            createDirectory(dir)
        }
    }

    /**
     * Create a new file. This method is similar to Java 7
     * `java.nio.file.Path.createFile`, but returns false instead of
     * throwing a exception if the file already existed.
     *
     * @param fileName the file name
     * @return true if creating was successful
     */
    fun createFile(fileName: String): Boolean = get(fileName).createFile()

    /**
     * Delete a file or directory if it exists.
     * Directories may only be deleted if they are empty.
     * This method is similar to Java 7
     * `java.nio.file.Path.deleteIfExists`.
     *
     * @param path the file or directory name
     */
    fun delete(path: String) = get(path).delete()

    /**
     * Get the canonical file or directory name. This method is similar to Java
     * 7 `java.nio.file.Path.toRealPath`.
     *
     * @param fileName the file name
     * @return the normalized file name
     */
    fun toRealPath(fileName: String): String = get(fileName).toRealPath().toString()


    /**
     * Get the parent directory of a file or directory. This method returns null
     * if there is no parent. This method is similar to Java 7
     * <code>java.nio.file.Path.getParent</code>
     * @param fileName the file or directory name
     * @return the parent directory name
     */
    @JvmStatic
    fun getParent(fileName: String): String? = get(fileName).getParent()?.toString()

    /**
     * Create an input stream to read from the file.
     * This method is similar to Java 7
     * <code>java.nio.file.path.newInputStream</code>.
     * @param fileName the file name
     * @return the input stream
     */
    @Throws(IOException::class)
    @JvmStatic
    fun newInputStream(fileName: String): InputStream = get(fileName).newInputStream()

    /**
     * Create an output stream to write into the file.
     * This method is similar to Java 7
     * <code>java.nio.file.Path.newOutputStream</code>
     * @param fileName the file name
     * @param append if true, the file will grow, if false, the file will be truncated first
     * @return the output stream
     */
    @Throws(IOException::class)
    @JvmStatic
    fun newOutputStream(fileName: String, append: Boolean): OutputStream = get(fileName).newOutputStream(append)

    /**
     * Check if the file is writable.
     * This method is similar to Java 7
     * `java.nio.file.Path.checkAccess(AccessMode.WRITE)`
     *
     * @param fileName the file name
     * @return if the file is writable
     */
    fun canWrite(fileName: String): Boolean = get(fileName).canWrite()

    /**
     * Try to delete a file or directory (ignoring errors).
     *
     * @param path the file or directory name
     * @return true if it worked
     */
    fun tryDelete(path: String): Boolean = kotlin.runCatching { get(path).delete() }.isSuccess

    /**
     * Create a new temporary file.
     *
     * @param prefix the prefix of the file name (including directory name if
     * required)
     * @param suffix the suffix
     * @param inTempDir if the file should be stored in the temporary directory
     * @return the name of the created file
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun createTempFile(prefix: String?, suffix: String?, inTempDir: Boolean): String = get(prefix!!).createTempFile(suffix!!, inTempDir).toString()
}

