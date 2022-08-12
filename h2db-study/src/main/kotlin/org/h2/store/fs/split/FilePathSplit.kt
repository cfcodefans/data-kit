package org.h2.store.fs.split

import org.h2.engine.SysProperties
import org.h2.message.DbException
import org.h2.store.fs.FilePath
import org.h2.store.fs.FilePathWrapper
import java.io.IOException
import java.io.InputStream
import java.io.SequenceInputStream
import java.lang.Long.max

/**
 * A file system that may split files into multiple smaller files.
 * (required for a FAT32 because it only support files up to 2 GB).
 */
open class FilePathSplit : FilePathWrapper() {
    companion object {
        private const val PART_SUFFIX = ".part"
    }

    override val scheme: String = "split"

    /**
     * Split the file name into size and base file name.
     *
     * @param fileName the file name
     * @return an array with size and file name
     */
    private fun parse(fileName: String): Array<String> {
        if (!fileName.startsWith(scheme)) {
            throw DbException.getInternalError("$fileName doesn't start with $scheme")
        }
        var fileName: String = fileName.substring(scheme.length + 1)

        return if (fileName.isNotEmpty() && Character.isDigit(fileName[0])) {
            val idx = fileName.indexOf(':')
            arrayOf(fileName.substring(0, idx), fileName.substring(idx + 1))
        } else {
            arrayOf(SysProperties.SPLIT_FILE_SIZE_SHIFT.toString(), fileName)
        }
    }

    override fun getPrefix(): String = "$scheme:${parse(name)[0]}:"

    override fun unwrap(path: String): FilePath = get(parse(fileName)[1])

    /**
     * Get the file name of a part file.
     *
     * @param id the part id
     * @return the file name including the part id
     */
    open fun getBase(id: Int): FilePath = get(getName(id))

    private fun getName(id: Int): String = if (id > 0) "${base.name}.$id$PART_SUFFIX" else base.name

    override fun setReadOnly(): Boolean {
        var result = false
        var i = 0; while (true) {
            val f = getBase(i)
            if (!f.exists()) break
            result = f.setReadOnly()

            i++; }
        return result
    }

    override fun delete() {
        var i = 0; while (true) {
            val f = getBase(i)
            if (!f.exists()) break
            f.delete()
            i++; }
    }

    override fun lastModified(): Long {
        var lastModified: Long = 0
        var i = 0; while (true) {
            val f = getBase(i)
            if (!f.exists()) break
            lastModified = max(lastModified, f.lastModified())
            i++; }
        return lastModified
    }

    override fun size(): Long {
        var length: Long = 0
        var i = 0; while (true) {
            val f = getBase(i)
            if (!f.exists()) break
            length += f.size()
            i++; }
        return length
    }

    override fun newDirectoryStream(): List<FilePath?> = base.newDirectoryStream().filter { it!!.name.endsWith(PART_SUFFIX).not() }

    @Throws(IOException::class)
    override fun newInputStream(): InputStream {
        var input = base.newInputStream()
        var i = 1; while (true) {
            val f = getBase(i)
            if (!f.exists()) break
            input = SequenceInputStream(input, f.newInputStream())
            i++; }
        return input
    }
}