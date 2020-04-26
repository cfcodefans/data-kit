package org.h2.store.fs

import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.NonWritableChannelException

/**
 * File which uses NIO FileChannel
 */
class FileNio(val name: String, mode: String) : FileBase() {
    val channel: FileChannel = RandomAccessFile(this.name, mode).channel

    @Throws(IOException::class)
    override fun implCloseChannel() = channel.close()

    @Throws(IOException::class)
    override fun position(): Long = channel.position()

    @Throws(IOException::class)
    override fun size(): Long = channel.size()

    @Throws(IOException::class)
    override fun read(dst: ByteBuffer?): Int = channel.read(dst)

    @Throws(IOException::class)
    override fun position(newPos: Long): FileChannel = apply {
        channel.position(newPos)
    }

    @Throws(IOException::class)
    override fun read(dst: ByteBuffer?, position: Long): Int = channel.read(dst, position)

    @Throws(IOException::class)
    override fun write(dst: ByteBuffer?, position: Long): Int = channel.write(dst, position)

    @Throws(IOException::class)
    override fun truncate(newLen: Long): FileChannel {
        val size: Long = channel.size()
        if (size >= newLen) return this
        val pos: Long = channel.position()
        channel.truncate(newLen)
        val newPos: Long = channel.position()
        if (pos < newLen) {
            // position should stay
            // in theory, this should not be needed
            if (newPos != pos) channel.position(pos)
        } else if (newPos > newLen) {
            // Looks like a bug in this FileChannel implementation,
            // as the documentation says the position needs to be changed
            channel.position(newLen)
        }
        return this
    }

    @Throws(IOException::class)
    override fun force(metaData: Boolean) = channel.force(metaData)

    @Throws(IOException::class)
    override fun write(src: ByteBuffer?): Int = try {
        channel.write(src)
    } catch (e: NonWritableChannelException) {
        throw IOException("read only")
    }

    @Synchronized
    @Throws(IOException::class)
    override fun tryLock(position: Long,
                         size: Long,
                         shared: Boolean): FileLock = channel.tryLock(position, size, shared)

    override fun toString(): String = "nio:${name}"
}

/**
 * This file system stores files on disk and uses java.nio to access the files.
 * This class uses FileChannel
 */
open class FilePathNio : FilePathWrapper() {
    @Throws(IOException::class)
    override fun open(mode: String): FileChannel {
        return FileNio(name.substring(scheme.length + 1), mode)
    }

    override val scheme: String = "nio"
}