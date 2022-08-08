package org.h2.store.fs

import org.h2.api.ErrorCode
import org.h2.message.DbException
import org.h2.store.fs.mem.FileMem
import org.h2.store.fs.mem.FileMemData
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.FileChannel
import java.util.*

/**
 * This file system keeps files fully in memory. There is an option to compress
 * file blocks to save memory.
 */
open class FilePathMem : FilePath() {
    companion object {
        val MEMORY_FILES: TreeMap<String, FileMemData> = TreeMap()
        val DIRECTORY: FileMemData = FileMemData("", false)

        /**
         * Get the canonical path for this file name.
         * @param fileName the file name
         * @return the canonical path
         */
        fun getCanonicalPath(_fileName: String): String {
            var fileName: String = _fileName.replace('\\', '/')
            val idx: Int = fileName.indexOf(':') + 1
            if (fileName.length > idx && fileName[idx] != '/') {
                fileName = fileName.substring(0, idx) + "/" + fileName.substring(idx)
            }
            return fileName
        }
    }

    override val scheme: String = "memFS"

    override fun getPath(path: String): FilePathMem {
        val p: FilePathMem = FilePathMem()
        p.name = getCanonicalPath(path)
        return p
    }

    /**
     * Whether the file should be compressed.
     *
     * @return if it should be compressed.
     */
    open fun compressed(): Boolean = false

    private fun getMemoryFile(): FileMemData {
        synchronized(MEMORY_FILES) {
            when (val m: FileMemData? = MEMORY_FILES[name]) {
                DIRECTORY -> throw DbException.get(ErrorCode.FILE_CREATION_FAILED_1,
                    "$name (a directory with this name already exists)")

                null -> return FileMemData(name, compressed()).also { MEMORY_FILES[name] = it }
                else -> return m
            }
        }
    }

    private fun isRoot(): Boolean = name == "$scheme:"

    override fun size(): Long = getMemoryFile().getLen() ?: -1

    override fun moveTo(newName: FilePath, atomicReplace: Boolean) {
        synchronized(MEMORY_FILES) {
            if (!atomicReplace && newName.name != name && MEMORY_FILES.containsKey(newName.name)) {
                throw DbException.get(ErrorCode.FILE_RENAME_FAILED_2, name, "$newName (exists)")
            }
            val f: FileMemData = getMemoryFile()
            f.name = newName.name
            MEMORY_FILES.remove(name)
            MEMORY_FILES.put(newName.name, f)
        }
    }

    override fun createFile(): Boolean {
        synchronized(MEMORY_FILES) {
            if (exists()) return false
            getMemoryFile()
        }
        return true
    }

    override fun exists(): Boolean = isRoot() || synchronized(MEMORY_FILES) { MEMORY_FILES[name] != null }

    override fun delete() {
        if (isRoot()) return
        synchronized(MEMORY_FILES) {
            MEMORY_FILES.remove(name)?.truncate(0)
        }
    }

    override fun newDirectoryStream(): List<FilePath?> {
        synchronized(MEMORY_FILES) {
            return@newDirectoryStream MEMORY_FILES.tailMap(name)
                .keys
                .takeWhile { n -> n.startsWith(name, false) && n != name && n.indexOf('/', name.length + 1) < 0 }
                .map { n -> getPath(n) }
                .toList()
        }
    }

    override fun setReadOnly(): Boolean {
        getMemoryFile().setReadOnly()
        return true
    }

    override fun canWrite(): Boolean = getMemoryFile().canWrite()

    override fun getParent(): FilePathMem? {
        val idx = name.lastIndexOf('/')
        return if (idx < 0) null else getPath(name.substring(0, idx))
    }

    override fun isDirectory(): Boolean = isRoot() || synchronized(MEMORY_FILES) { MEMORY_FILES[name] === DIRECTORY }

    override fun isAbsolute(): Boolean {
        // TODO relative files are not supported
        return true
    }

    override fun toRealPath(): FilePathMem = this

    override fun lastModified(): Long = getMemoryFile().lastModified

    override fun createDirectory() {
        if (exists()) {
            throw DbException.get(ErrorCode.FILE_CREATION_FAILED_1,
                "$name (a file with this name already exists)")
        }
        synchronized(MEMORY_FILES) { MEMORY_FILES.put(name, DIRECTORY) }
    }

    @Throws(IOException::class)
    override fun newOutputStream(append: Boolean): OutputStream {
        return FileChannelOutputStream(FileMem(getMemoryFile(), false), append)
    }

    override fun open(mode: String): FileChannel = FileMem(getMemoryFile(), "r" == mode)

    override fun newInputStream(): InputStream {
        return FileChannelInputStream(FileMem(getMemoryFile(), true), true)
    }
}
