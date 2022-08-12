package org.h2.store.fs

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.FileChannel

/**
 * The base class for wrapping  / delegating file systems such as
 * the split file system.
 */
abstract class FilePathWrapper : FilePath() {
    protected lateinit var base: FilePath

    /**
     * Get the base path for the given wrapped path.
     * @param path the path including the scheme prefix
     * @return the base file path
     */
    protected open fun unwrap(path: String): FilePath = get(path.substring(scheme.length + 1))

    protected open fun getPrefix(): String = "$scheme:"

    /**
     * Create a wrapped path instance for the given base path.
     * @param base the base path
     * @return the wrapped path
     */
    open fun wrap(base: FilePath?): FilePathWrapper? {
        return base?.let { _base -> create(getPrefix() + _base.name, _base) }
    }

    override fun getPath(path: String): FilePath {
        return create(path, unwrap(path))
    }

    private fun create(path: String, base: FilePath): FilePathWrapper {
        try {
            val p: FilePathWrapper = javaClass.getDeclaredConstructor().newInstance()
            p.name = path
            p.base = base
            return p
        } catch (e: Exception) {
            throw IllegalArgumentException("Path: $path", e)
        }
    }

    override fun canWrite(): Boolean = base.canWrite()
    override fun createDirectory() = base.createDirectory()
    override fun createFile(): Boolean = base.createFile()
    override fun delete() = base.delete()
    override fun exists(): Boolean = base.exists()
    override fun getParent(): FilePath? = wrap(base.getParent())
    override fun isAbsolute(): Boolean = base.isAbsolute()
    override fun isDirectory(): Boolean = base.isDirectory()
    override fun lastModified(): Long = base.lastModified()
    override fun toRealPath(): FilePath = base.toRealPath()

    override fun newDirectoryStream(): List<FilePath?> {
        return base.newDirectoryStream().map { it -> wrap(it) }
    }

    override fun moveTo(newName: FilePath, atomicReplace: Boolean) = base.moveTo((newName as FilePathWrapper).base, atomicReplace)

    @Throws(IOException::class)
    override fun newInputStream(): InputStream = base.newInputStream()

    @Throws(IOException::class)
    override fun newOutputStream(append: Boolean): OutputStream = base.newOutputStream(append)

    @Throws(IOException::class)
    override fun open(mode: String): FileChannel = base.open(mode)

    override fun setReadOnly(): Boolean = base.setReadOnly()

    override fun size(): Long = base.size()

    override fun createTempFile(suffix: String, inTempDir: Boolean): FilePath? = wrap(base.createTempFile(suffix, inTempDir))
}