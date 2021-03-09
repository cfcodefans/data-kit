package org.h2.store.fs

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel

/**
 * Fake file channel to use by in-memory and ZIP file systems.
 */
class FakeFileChannel : FileChannel() {
    @Throws(IOException::class)
    override fun implCloseChannel(): Unit = throw IOException()

    @Throws(IOException::class)
    override fun lock(position: Long, size: Long, shared: Boolean): FileLock? = throw IOException()

    @Throws(IOException::class)
    override fun map(mode: MapMode?, position: Long, size: Long): MappedByteBuffer? = throw IOException()

    @Throws(IOException::class)
    override fun position(): Long = throw IOException()

    @Throws(IOException::class)
    override fun position(newPosition: Long): FileChannel? = throw IOException()

    @Throws(IOException::class)
    override fun read(dst: ByteBuffer?): Int = throw IOException()

    @Throws(IOException::class)
    override fun read(dst: ByteBuffer?, position: Long): Int = throw IOException()

    @Throws(IOException::class)
    override fun read(dsts: Array<ByteBuffer?>?, offset: Int, length: Int): Long = throw IOException()

    @Throws(IOException::class)
    override fun size(): Long = throw IOException()

    @Throws(IOException::class)
    override fun transferFrom(src: ReadableByteChannel?, position: Long, count: Long): Long = throw IOException()

    @Throws(IOException::class)
    override fun transferTo(position: Long, count: Long, target: WritableByteChannel?): Long = throw IOException()

    @Throws(IOException::class)
    override fun truncate(size: Long): FileChannel? = throw IOException()

    @Throws(IOException::class)
    override fun tryLock(position: Long, size: Long, shared: Boolean): FileLock? = throw IOException()

    @Throws(IOException::class)
    override fun write(src: ByteBuffer?): Int = throw IOException()

    @Throws(IOException::class)
    override fun write(src: ByteBuffer?, position: Long): Int = throw IOException()

    @Throws(IOException::class)
    override fun write(srcs: Array<ByteBuffer?>?, offset: Int, len: Int): Long = throw IOException()

    @Throws(IOException::class)
    override fun force(metaData: Boolean): Unit = throw IOException()
}