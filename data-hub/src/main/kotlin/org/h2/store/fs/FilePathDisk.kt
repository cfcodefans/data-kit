package org.h2.store.fs

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.SystemUtils
import org.h2.api.ErrorCode
import org.h2.engine.SysProperties
import org.h2.message.DbException
import org.h2.util.IOUtils
import java.io.*
import java.net.URL
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.NonWritableChannelException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Pattern
import kotlin.math.min

/**
 * This file system stores files on disk.
 * This is the most common file system.
 */
class FilePathDisk : FilePath() {
    companion object {
        const val CLASSPATH_PREFIX: String = "classpath:"

        /**
         * Expand '~' to the user home directory. It is only be expanded if the '~'
         * stands alone, or is followed by '/' or '\'
         * @param fileName the file name
         * @return the native file name
         */
        @JvmStatic
        fun expandUserHomeDirectory(fileName: String): String = when {
            fileName == "~" || fileName.startsWith("~/") -> {
                SysProperties.USER_HOME + fileName.substring(1)
            }
            else -> fileName
        }

        /**
         * Translate the file name to the native format. This will replace '\' with '/' and expand the home directory ('~').
         * @param fileName the file name
         * @return the native file name
         */
        @JvmStatic
        fun translateFileName(fileName: String): String = expandUserHomeDirectory(
                fileName.replace('\\', '/').let {
                    if (it.startsWith("file:"))
                        it.substring("file:".length)
                    else it
                })

        @JvmStatic
        fun wait(i: Int) {
            if (i == 8) System.gc()
            try {
                Thread.sleep(min(256, i * i).toLong())
            } catch (e: InterruptedException) {
            }
        }

        @JvmStatic
        private fun canWriteInternal(file: File): Boolean = Files.isWritable(file.toPath())

        /**
         * Call the garbage collection and run finalization. This close all files
         * that were not closed, and are no longer referenced.
         */
        @JvmStatic
        fun freeMemoryAndFinalize() {
            IOUtils.trace("freeMemoryAndFinalize", null, null)
            val rt: Runtime = Runtime.getRuntime()
            var mem: Long = rt.freeMemory()
            repeat(16) {
                rt.gc()
                val now: Long = rt.freeMemory()
                rt.runFinalization()
                if (now == mem) return
                mem = now
            }
        }
    }

    override fun getPath(path: String): FilePath {
        val p: FilePathDisk = FilePathDisk()
        p.name = translateFileName(path)
        return p
    }

    override fun size(): Long {
        if (name.startsWith(CLASSPATH_PREFIX)) {
            return try {
                var filename: String = StringUtils.prependIfMissing(name.substring(CLASSPATH_PREFIX.length), "/")
                val resource: URL? = this.javaClass.getResource(filename)
                if (resource != null)
                    Files.size(Paths.get(resource.toURI()))
                else
                    0
            } catch (e: Exception) {
                0
            }
        }
        return File(name).length()
    }

    override fun moveTo(newName: FilePath, atomicReplace: Boolean) {
        val oldFile: File = File(name)
        val newFile: File = File(newName.name)
        if (oldFile.absolutePath == newFile.absolutePath) return

        if (!oldFile.exists()) {
            throw DbException.get(ErrorCode.FILE_RENAME_FAILED_2, "$name (not found)", newName.name)
        }

        if (atomicReplace) {
            if (oldFile.renameTo(newFile)) return
            throw DbException.get(ErrorCode.FILE_RENAME_FAILED_2, name, newName.name)
        }

        if (newFile.exists()) {
            throw DbException.get(ErrorCode.FILE_RENAME_FAILED_2, name, "$newName (exists)")
        }

        repeat(SysProperties.MAX_FILE_RETRY) {
            IOUtils.trace("rename", "$name > $newName", null)
            if (oldFile.renameTo(newFile)) return
            wait(it)
        }
        throw DbException.get(ErrorCode.FILE_RENAME_FAILED_2, name, newName.name)
    }

    override fun createFile(): Boolean {
        val file: File = File(name)
        repeat(SysProperties.MAX_FILE_RETRY) {
            try {
                return file.createNewFile()
            } catch (e: IOException) {
                wait(it)
            }
        }
        return false
    }

    override fun exists(): Boolean = File(name).exists()

    override fun delete() {
        val file: File = File(name)
        repeat(SysProperties.MAX_FILE_RETRY) {
            IOUtils.trace("delete", name, null)
            if (file.delete() || !file.exists()) return
            wait(it)
        }
        throw DbException.get(ErrorCode.FILE_DELETE_FAILED_1, name)
    }

    override fun newDirectoryStream(): List<FilePath?> {
        val f: File = File(name)
        try {
            val files: Array<String>? = f.list()
            files ?: return emptyList()
            val base: String = StringUtils.appendIfMissing(f.canonicalPath, SysProperties.FILE_SEPARATOR)
            return files.map { file -> getPath(base + file) }.toList()
        } catch (e: IOException) {
            throw  DbException.convertIOException(e, name)
        }
    }

    override fun canWrite(): Boolean = canWriteInternal(File(name))

    override fun setReadOnly(): Boolean = File(name).setReadOnly()

    override fun toRealPath(): FilePath = try {
        getPath(File(name).canonicalPath)
    } catch (e: IOException) {
        throw DbException.convertIOException(e, name)
    }

    override fun getParent(): FilePath? = File(name).parent?.let { getPath(it) }

    override fun isDirectory(): Boolean = File(name).isDirectory

    override fun isAbsolute(): Boolean = File(name).isAbsolute

    override fun lastModified(): Long = File(name).lastModified()

    override fun createDirectory() {
        val dir: File = File(name)
        repeat(SysProperties.MAX_FILE_RETRY) {
            if (dir.exists()) {
                if (dir.isDirectory) return
                throw DbException.get(ErrorCode.FILE_CREATION_FAILED_1, "$name (a file with this name already exists")
            }
        }
    }

    @Throws(IOException::class)
    override fun newOutputStream(append: Boolean): OutputStream =
            try {
                val file: File = File(name)
                val parent: File? = file.parentFile
                if (parent != null) {
                    FileUtils.createDirectories(parent.absolutePath)
                }
                val out: FileOutputStream = FileOutputStream(name, append)
                IOUtils.trace("newOutputStream", name, out)
                out
            } catch (e: IOException) {
                freeMemoryAndFinalize()
                FileOutputStream(name)
            }

    @Throws(IOException::class)
    override fun newInputStream(): InputStream {
        if (Pattern.matches("[a-zA-Z]{2,19}:.*", name)) {
            // if the ':' is in the position 1, a windows file access is assumed:
            // C:.. or D:, and if the ':' is not at the beginning, assume its a
            // file name with a colon
            if (name.startsWith(CLASSPATH_PREFIX)) {
                val filename: String = StringUtils.prependIfMissing(name.substring(CLASSPATH_PREFIX.length), "/")
                return this.javaClass.getResourceAsStream(fileName)
                        ?: Thread.currentThread().contextClassLoader.getResourceAsStream(fileName.substring(1))
                        ?: throw FileNotFoundException("resource $filename")
            }
            return URL(name).openStream()
        }
        val fin: FileInputStream = FileInputStream(name)
        IOUtils.trace("newOutputStream", name, fin)
        return fin
    }

    @Throws(IOException::class)
    override fun open(mode: String): FileChannel = try {
        val f: FileDisk = FileDisk(name, mode)
        IOUtils.trace("open", name, f)
        f
    } catch (e: IOException) {
        freeMemoryAndFinalize()
        try {
            FileDisk(name, mode)
        } catch (e2: IOException) {
            throw e2
        }
    }

    override val scheme: String = "file"

    @Throws(IOException::class)
    override fun createTempFile(suffix: String, inTempDir: Boolean): FilePath? {
        val filename: String = name + "."
        val prefix: String = File(filename).name
        val dir: File = if (inTempDir)
            File(SystemUtils.JAVA_IO_TMPDIR)
        else
            File(filename).absoluteFile.parentFile

        FileUtils.createDirectories(dir.absolutePath)
        while (true) {
            val f: File = File(dir, prefix + getNextTempFileNamePart(false) + suffix)
            if (f.exists() || !f.createNewFile()) {
                //in theory, the random number could collide
                getNextTempFileNamePart(true)
                continue
            }
            return get(f.canonicalPath)
        }
    }
}

/**
 * Uses java.io.RandomAccessFile to access a file.
 */
class FileDisk(private val name: String) : FileBase() {
    private lateinit var file: RandomAccessFile
    private var readOnly: Boolean = false

    constructor(filename: String, mode: String) : this(filename) {
        file = RandomAccessFile(filename, mode)
        readOnly = mode == "r"
    }

    @Throws(IOException::class)
    override fun force(metaData: Boolean) {
        when (SysProperties.SYNC_METHOD) {
            "sync" -> file.fd.sync()
            "force" -> file.channel.force(true)
            "forceFalse" -> file.channel.force(false)
            "" -> {
            }
            else -> file.fd.sync()
        }
    }

    @Throws(IOException::class)
    override fun size(): Long = file.length()

    @Throws(IOException::class)
    override fun position(): Long = file.filePointer

    @Throws(IOException::class)
    override fun position(newPosition: Long): FileChannel {
        file.seek(newPosition)
        return this
    }

    @Throws(IOException::class)
    override fun read(dst: ByteBuffer): Int {
        val len: Int = file.read(dst.array(), dst.arrayOffset() + dst.position(), dst.remaining())
        if (len > 0) {
            dst.position(dst.position() + len)
        }
        return len
    }

    @Throws(IOException::class)
    override fun write(src: ByteBuffer): Int {
        val len: Int = src.remaining()
        file.write(src.array(), src.arrayOffset() + src.position(), len)
        src.position(src.position() + len)
        return len
    }

    @Throws(IOException::class)
    override fun truncate(size: Long): FileChannel {
        //compatibility with JDK FileChannel#truncate
        if (readOnly) throw NonWritableChannelException()

        /**
         * RandomAccessFile.setLength() does not always work here since Java 9 for
         * unknown reason so use FileChannel.truncate().
         */
        file.channel.truncate(size)
        return this
    }

    @Throws(IOException::class)
    override fun implCloseChannel() = file.close()

    @Throws(IOException::class)
    override fun tryLock(position: Long, size: Long, shared: Boolean): FileLock = file.channel.tryLock(position, size, shared)

    override fun toString(): String = name
}