package org.h2.store.fs

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel

/**
 * The base class for file implementations.
 */
abstract class FileBase : FileChannel() {

    @Throws(IOException::class)
    @Synchronized
    override fun read(dst: ByteBuffer?, position: Long): Int {
        val oldPos: Long = position()
        position(position)
        val len: Int = read(dst)
        position(oldPos)
        return len
    }

    @Throws(IOException::class)
    @Synchronized
    override fun write(dst: ByteBuffer?, position: Long): Int {
        val oldPos: Long = position()
        position(position)
        val len: Int = write(dst)
        position(oldPos)
        return len
    }

    override fun force(metaData: Boolean) {
    }

    override fun implCloseChannel() {
    }


    override fun lock(position: Long, size: Long, shared: Boolean): FileLock = throw UnsupportedOperationException()

    override fun map(mode: MapMode?, position: Long, size: Long): MappedByteBuffer = throw UnsupportedOperationException()

    override fun read(dsts: Array<out ByteBuffer>?, offset: Int, length: Int): Long = throw UnsupportedOperationException()

    override fun transferFrom(src: ReadableByteChannel?, position: Long, count: Long): Long = throw UnsupportedOperationException()

    override fun transferTo(position: Long, count: Long, target: WritableByteChannel?): Long = throw UnsupportedOperationException()

    override fun tryLock(position: Long, size: Long, shared: Boolean): FileLock = throw UnsupportedOperationException()

    override fun write(srcs: Array<out ByteBuffer>?, offset: Int, length: Int): Long = throw UnsupportedOperationException()
}