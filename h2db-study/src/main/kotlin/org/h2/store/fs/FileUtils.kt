package org.h2.store.fs

import org.h2.store.fs.FilePath.Companion.get
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.OpenOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.FileAttribute
import java.util.Collections
import java.util.EnumSet

object FileUtils {
    /**
     * [StandardOpenOption.READ].
     */
    val R: Set<OpenOption> = setOf(StandardOpenOption.READ)

    /**
     * [StandardOpenOption.READ], [StandardOpenOption.WRITE], and
     * [StandardOpenOption.CREATE].
     */
    val RW: Set<OpenOption> = Collections
        .unmodifiableSet(EnumSet.of(StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE))

    /**
     * [StandardOpenOption.READ], [StandardOpenOption.WRITE],
     * [StandardOpenOption.CREATE], and [StandardOpenOption.SYNC].
     */
    val RWS: Set<OpenOption> = Collections.unmodifiableSet(EnumSet.of(StandardOpenOption.READ,
        StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.SYNC))

    /**
     * [StandardOpenOption.READ], [StandardOpenOption.WRITE],
     * [StandardOpenOption.CREATE], and [StandardOpenOption.DSYNC].
     */
    val RWD: Set<OpenOption> = Collections.unmodifiableSet(EnumSet.of(StandardOpenOption.READ,
        StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.DSYNC))

    /**
     * No file attributes.
     */
    val NO_ATTRIBUTES: Array<FileAttribute<*>> = emptyArray()

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
     * Check if the file name includes a path. This method is similar to Java 7
     * `java.nio.file.Path.isAbsolute`.
     *
     * @param fileName the file name
     * @return if the file name is absolute
     */
    fun isAbsolute(fileName: String): Boolean = (get(fileName).isAbsolute()
            // Allows Windows to recognize "/path" as absolute.
            // Makes the same configuration work on all platforms.
            || fileName.startsWith(File.separator) // Just in case of non-normalized path on Windows
            || fileName.startsWith("/"))

    /**
     * Rename a file if this is allowed. This method is similar to Java 7
     * `java.nio.file.Files.move`.
     *
     * @param source the old fully qualified file name
     * @param target the new fully qualified file name
     */
    fun move(source: String, target: String) = get(source).moveTo(get(target), false)

    /**
     * Rename a file if this is allowed, and try to atomically replace an
     * existing file. This method is similar to Java 7
     * `java.nio.file.Files.move`.
     *
     * @param source the old fully qualified file name
     * @param target the new fully qualified file name
     */
    fun moveAtomicReplace(source: String, target: String) = get(source).moveTo(get(target), true)

    /**
     * Get the file or directory name (the last element of the path).
     * This method is similar to Java 7 `java.nio.file.Path.getName`.
     *
     * @param path the directory and file name
     * @return just the file name
     */
    fun getName(path: String?): String = get(path!!).name

    /**
     * List the files and directories in the given directory.
     * This method is similar to Java 7
     * `java.nio.file.Path.newDirectoryStream`.
     *
     * @param path the directory
     * @return the list of fully qualified file names
     */
    fun newDirectoryStream(path: String?): List<String> = get(path!!)
        .newDirectoryStream()
        .map { filePath -> filePath.toString() }

    /**
     * Get the size of a file in bytes
     * This method is similar to Java 7
     * `java.nio.file.attribute.Attributes.
     * readBasicFileAttributes(file).size()`
     *
     * @param fileName the file name
     * @return the size in bytes
     */
    fun size(fileName: String?): Long = get(fileName!!).size()


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

    /**
     * Get the last modified date of a file.
     * This method is similar to Java 7
     * `java.nio.file.attribute.Attributes.
     * readBasicFileAttributes(file).lastModified().toMillis()`
     *
     * @param fileName the file name
     * @return the last modified date
     */
    fun lastModified(fileName: String?): Long = get(fileName!!).lastModified()

    /**
     * Convert the string representation to a set.
     *
     * @param mode the mode as a string
     * @return the set
     */
    fun modeToOptions(mode: String): Set<OpenOption> = when (mode) {
        "r" -> R
        "rw" -> RW
        "rws" -> RWS
        "rwd" -> RWD
        else -> throw IllegalArgumentException(mode)
    }
}

