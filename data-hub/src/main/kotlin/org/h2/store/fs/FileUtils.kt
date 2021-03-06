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
                throw  EOFException()
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
}

