package org.h2.store.fs.rec

import org.h2.store.fs.FilePath
import org.h2.store.fs.FilePathWrapper
import org.h2.store.fs.Recorder
import java.io.IOException
import java.io.OutputStream
import java.nio.channels.FileChannel

/**
 * A file system that records all write operations and can re-play them.
 */
open class FilePathRec : FilePathWrapper() {
    override val scheme: String = "rec"

    companion object {
        private val INSTANCE = FilePathRec()

        var recorder: Recorder? = null

        /**
         * Register the file system.
         */
        fun register() {
            register(INSTANCE)
        }
    }

    var trace: Boolean = false

    override fun delete() {
        log(Recorder.DELETE, name)
        super.delete()
    }

    @Throws(IOException::class)
    override fun open(mode: String): FileChannel = FileRec(this, super.open(mode), name)

    @Throws(IOException::class)
    override fun newOutputStream(append: Boolean): OutputStream {
        log(Recorder.OPEN_OUTPUT_STREAM, name)
        return super.newOutputStream(append)
    }

    override fun moveTo(newPath: FilePath, atomicReplace: Boolean) {
        log(Recorder.RENAME, "${unwrap(name)}:${unwrap(newPath.name)}")
        super.moveTo(newPath, atomicReplace)
    }

    override fun createFile(): Boolean {
        log(Recorder.CREATE_NEW_FILE, name)
        return super.createFile()
    }

    /**
     * Log the operation.
     *
     * @param op the operation
     * @param fileName the file name(s)
     */
    open fun log(op: Int, fileName: String?) = log(op, fileName, null, 0)

    /**
     * Log the operation.
     *
     * @param op the operation
     * @param fileName the file name
     * @param data the data or null
     * @param x the value or 0
     */
    open fun log(op: Int, fileName: String?, data: ByteArray?, x: Long) = apply {
        recorder?.log(op, fileName, data, x)
    }
}