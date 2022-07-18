package org.h2.store.fs

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * Default implementation of the slow operations that need synchronization because they
 * involve the file position.
 */
abstract class FileBaseDefault : FileBase() {
    private var position: Long = 0

    @Synchronized
    @Throws(IOException::class)
    override fun position(): Long = position

    @Synchronized
    @Throws(IOException::class)
    override fun position(newPosition: Long): FileChannel = apply {
        require(newPosition >= 0)
        position = newPosition
    }

    @Synchronized
    @Throws(IOException::class)
    override fun read(dst: ByteBuffer?): Int {
        val read = read(dst, position)
        if (read > 0) {
            position += read.toLong()
        }
        return read
    }

    @Synchronized
    @Throws(IOException::class)
    override fun write(src: ByteBuffer?): Int {
        val written = write(src, position)
        if (written > 0) {
            position += written.toLong()
        }
        return written
    }

    @Synchronized
    @Throws(IOException::class)
    override fun truncate(newLength: Long): FileChannel {
        implTruncate(newLength)
        if (newLength < position) {
            position = newLength
        }
        return this
    }

    /**
     * The truncate implementation.
     * @param size the new size
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    protected abstract fun implTruncate(size: Long)
}