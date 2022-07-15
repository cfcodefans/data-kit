package org.h2.store.fs.disk

import org.h2.api.ErrorCode
import org.h2.engine.SysProperties
import org.h2.message.DbException
import org.h2.store.fs.FilePath
import org.h2.store.fs.FileUtils
import org.h2.util.IOUtils
import org.jetbrains.kotlin.util.prefixIfNot
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import java.nio.channels.FileChannel
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.CopyOption
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.FileStore
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.DosFileAttributeView
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermission
import java.util.regex.Pattern

/**
 * This file system stores files on disk.
 * This is the most common file system.
 */
open class FilePathDisk(override val scheme: String = "file") : FilePath() {
    companion object {
        private const val CLASSPATH_PREFIX = "classpath:"

        /**
         * Translate the file name to the native format. This will replace '\' with
         * '/' and expand the home directory ('~').
         *
         * @param fileName the file name
         * @return the native file name
         */
        protected fun translateFileName(fileName: String): String = expandUserHomeDirectory(
            fileName.replace('\\', '/')
                .let {
                    if (it.startsWith("file:")) it.substring(5)
                    else if (fileName.startsWith("nio:")) it.substring(4)
                    else it
                })

        /**
         * Expand '~' to the user home directory. It is only be expanded if the '~'
         * stands alone, or is followed by '/' or '\'.
         *
         * @param fileName the file name
         * @return the native file name
         */
        fun expandUserHomeDirectory(fileName: String): String = if (fileName.startsWith("~") && (fileName.length == 1 || fileName.startsWith("~/"))) SysProperties.USER_HOME + fileName.substring(1) else fileName

        // sleep at most 256 ms
        private fun wait(i: Int) {
            if (i == 8) System.gc()
            try {
                Thread.sleep(256.coerceAtMost(i * i).toLong())
            } catch (ex: InterruptedException) {
            }
        }

        private fun toRealPath(path: Path): Path {
            var parent: Path? = path.parent ?: return path
            parent = try {
                parent!!.toRealPath()
            } catch (e: IOException) {
                toRealPath(parent!!)
            }
            return parent!!.resolve(path.fileName)
        }

        /**
         * Call the garbage collection and run finalization. This close all files
         * that were not closed, and are no longer referenced.
         */
        fun freeMemoryAndFinalize() {
            IOUtils.trace("freeMemoryAndFinalize", null, null)
            val rt = Runtime.getRuntime()
            var mem = rt.freeMemory()
            for (i in 0..15) {
                rt.gc()
                val now = rt.freeMemory()
                rt.runFinalization()
                if (now == mem) break
                mem = now
            }
        }
    }

    override fun getPath(path: String): FilePathDisk = FilePathDisk().also { name = translateFileName(path) }

    override fun size(): Long = if (name.startsWith(CLASSPATH_PREFIX))
        kotlin.runCatching {
            this.javaClass.getResource(name.substring(CLASSPATH_PREFIX.length).prefixIfNot("/"))
                ?.let { Files.size(Paths.get(it.toURI())) }
                ?: 0
        }.getOrDefault(0)
    else kotlin.runCatching { Files.size(Paths.get(name)) }.getOrDefault(0)

    override fun moveTo(newName: FilePath, atomicReplace: Boolean) {
        val oldFile = Paths.get(name)
        val newFile = Paths.get(newName.name)

        if (!Files.exists(oldFile)) {
            throw DbException.get(ErrorCode.FILE_RENAME_FAILED_2, "$name (not found)", newName.name)
        }

        if (atomicReplace) {
            try {
                Files.move(oldFile, newFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                return
            } catch (ex: AtomicMoveNotSupportedException) {
                // Ignore
            } catch (ex: IOException) {
                throw DbException.get(ErrorCode.FILE_RENAME_FAILED_2, ex, name, newName.name)
            }
        }

        val copyOptions: Array<CopyOption> = if (atomicReplace) arrayOf(StandardCopyOption.REPLACE_EXISTING) else emptyArray()
        try {
            Files.move(oldFile, newFile, *copyOptions);
        } catch (ex: FileAlreadyExistsException) {
            throw DbException.get(ErrorCode.FILE_RENAME_FAILED_2, name, "$newName (exists)")
        } catch (ex: IOException) {
            var ioe: IOException? = null
            repeat(SysProperties.MAX_FILE_RETRY) { i ->
                IOUtils.trace("rename", "$name >$newName", null)
                try {
                    Files.move(oldFile, newFile, *copyOptions)
                    return
                } catch (ex: FileAlreadyExistsException) {
                    throw DbException.get(ErrorCode.FILE_RENAME_FAILED_2, name, "$newName (exists)")
                } catch (ex: IOException) {
                    ioe = ex
                }
                wait(i)
            }
            throw DbException.get(ErrorCode.FILE_RENAME_FAILED_2, ioe, name, newName.name)
        }
    }

    override fun createFile(): Boolean {
        val file = Paths.get(name)
        for (i in 0 until SysProperties.MAX_FILE_RETRY) {
            try {
                Files.createFile(file)
                return true
            } catch (e: FileAlreadyExistsException) {
                return false
            } catch (e: IOException) {
                // 'access denied' is really a concurrent access problem
                wait(i)
            }
        }
        return false
    }

    override fun exists(): Boolean = Files.exists(Paths.get(name))

    override fun delete() {
        val file = Paths.get(name)
        var cause: IOException? = null
        for (i in 0 until SysProperties.MAX_FILE_RETRY) {
            IOUtils.trace("delete", name, null)
            cause = try {
                Files.deleteIfExists(file)
                return
            } catch (e: DirectoryNotEmptyException) {
                throw DbException.get(ErrorCode.FILE_DELETE_FAILED_1, e, name)
            } catch (e: IOException) {
                e
            }
            wait(i)
        }
        throw DbException.get(ErrorCode.FILE_DELETE_FAILED_1, cause, name)
    }

    override fun newDirectoryStream(): List<FilePath> {
        try {
            Files.list(Paths.get(name).toRealPath()).use { files ->
                return files.collect({ ArrayList() },
                    { t: ArrayList<FilePath>, u: Path -> t.add(getPath(u.toString())) }
                ) { obj: ArrayList<FilePath>, c: ArrayList<FilePath>? -> obj.addAll(c!!) }
            }
        } catch (e: NoSuchFileException) {
            return emptyList()
        } catch (e: IOException) {
            throw DbException.convertIOException(e, name)
        }
    }

    override fun canWrite(): Boolean = try {
        Files.isWritable(Paths.get(name))
    } catch (e: Exception) {
        // Catch security exceptions
        false
    }

    override fun toRealPath(): FilePathDisk {
        val path = Paths.get(name)
        return try {
            getPath(path.toRealPath().toString())
        } catch (e: IOException) {
/*
* File does not exist or isn't accessible, try to get the real path
* of parent directory.
*/
            getPath(toRealPath(path.toAbsolutePath().normalize()).toString())
        }
    }

    override fun getParent(): FilePath? = Paths.get(name).parent?.let { getPath(it.toString()) }

    override fun isDirectory(): Boolean = Files.isDirectory(Paths.get(name))

    override fun isAbsolute(): Boolean = Paths.get(name).isAbsolute

    override fun lastModified(): Long = try {
        Files.getLastModifiedTime(Paths.get(name)).toMillis()
    } catch (e: IOException) {
        0L
    }

    override fun createDirectory() {
        val dir = Paths.get(name)
        try {
            Files.createDirectory(dir)
        } catch (e: java.nio.file.FileAlreadyExistsException) {
            throw DbException.get(ErrorCode.FILE_CREATION_FAILED_1, "$name (a file with this name already exists)")
        } catch (e: IOException) {
            var cause: IOException? = e
            repeat(SysProperties.MAX_FILE_RETRY) { i ->
                if (Files.isDirectory(dir)) return
                try {
                    Files.createDirectory(dir)
                } catch (ex: FileAlreadyExistsException) {
                    throw DbException.get(ErrorCode.FILE_CREATION_FAILED_1, "$name (a file with this name already exists)")
                } catch (ex: IOException) {
                    cause = ex
                }
                wait(i)
            }
            throw DbException.get(ErrorCode.FILE_CREATION_FAILED_1, cause, name)
        }
    }

    @Throws(IOException::class)
    override fun newOutputStream(append: Boolean): OutputStream {
        val file = Paths.get(name)
        val options: Array<OpenOption> = if (append) arrayOf(StandardOpenOption.CREATE, StandardOpenOption.APPEND) else emptyArray()
        return try {
            val parent = file.parent
            if (parent != null) {
                Files.createDirectories(parent)
            }
            val out = Files.newOutputStream(file, *options)
            IOUtils.trace("openFileOutputStream", name, out)
            out
        } catch (e: IOException) {
            freeMemoryAndFinalize()
            Files.newOutputStream(file, *options)
        }
    }

    @Throws(IOException::class)
    override fun newInputStream(): InputStream {
        if (Pattern.matches("[a-zA-Z]{2,19}:.*", this.name)) {
            // if the ':' is in position 1, a windows file access is assumed:
            // C:.. or D:, and if the ':' is not at the beginning, assume its a
            // file name with a colon
            if (name.startsWith(CLASSPATH_PREFIX)) {
                val fileName = name.substring(CLASSPATH_PREFIX.length).prefixIfNot("/")
                // Force absolute resolution in Class.getResourceAsStream
                return javaClass.getResourceAsStream(fileName)
                    ?: Thread.currentThread().contextClassLoader.getResourceAsStream(fileName.substring(1)) // ClassLoader.getResourceAsStream doesn't need leading "/"
                    ?: throw FileNotFoundException("resource $fileName")
            }
            // otherwise a URL is assumed
            return URL(name).openStream()
        }
        return Files.newInputStream(Paths.get(name))
            .also { IOUtils.trace("openFileInputStream", name, it) }
    }

    @Throws(IOException::class)
    override fun createTempFile(suffix: String, inTempDir: Boolean): FilePath {
        var file = Paths.get("$name.").toAbsolutePath()
        val prefix = file.fileName.toString()
        file = if (inTempDir) {
            Files.createDirectories(Paths.get(System.getProperty("java.io.tmpdir", ".")))
            Files.createTempFile(prefix, suffix)
        } else {
            val dir = file.parent
            Files.createDirectories(dir)
            Files.createTempFile(dir, prefix, suffix)
        }
        return get(file.toString())
    }

    @Throws(IOException::class)
    override fun open(mode: String): FileChannel {
        val f = FileChannel.open(Paths.get(name),
            FileUtils.modeToOptions(mode),
            *FileUtils.NO_ATTRIBUTES)
        IOUtils.trace("open", name, f)
        return f
    }

    override fun setReadOnly(): Boolean {
        val f = Paths.get(name)
        return try {
            val fileStore: FileStore = Files.getFileStore(f)
            /*
                  * Need to check PosixFileAttributeView first because
                  * DosFileAttributeView is also supported by recent Java versions on
                  * non-Windows file systems, but it doesn't affect real access
                  * permissions.
                  */
            if (fileStore.supportsFileAttributeView(PosixFileAttributeView::class.java)) {
                Files.setPosixFilePermissions(f,
                    Files.getPosixFilePermissions(f)
                        .filterNot { p ->
                            p == PosixFilePermission.OWNER_WRITE
                                    || p == PosixFilePermission.GROUP_WRITE
                                    || p == PosixFilePermission.OTHERS_WRITE
                        }.toSet())
            } else if (fileStore.supportsFileAttributeView(DosFileAttributeView::class.java)) {
                Files.setAttribute(f, "dos:readonly", true)
            } else {
                return false
            }
            true
        } catch (e: IOException) {
            false
        }
    }
}