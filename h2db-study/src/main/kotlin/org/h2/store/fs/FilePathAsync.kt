package org.h2.store.fs

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.NonWritableChannelException
import java.nio.file.OpenOption
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future


/**
 * File which uses NIO2 AsynchronousFileChannel.
 */
class FileAsync(val name: String) : FileBase() {
    companion object {
        private val R = arrayOf<OpenOption>(StandardOpenOption.READ)
        private val W = arrayOf<OpenOption>(StandardOpenOption.READ,
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE)
        private val RWS = arrayOf<OpenOption>(StandardOpenOption.READ,
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE,
            StandardOpenOption.SYNC)
        private val RWD = arrayOf<OpenOption>(StandardOpenOption.READ,
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE,
            StandardOpenOption.DSYNC)

        @JvmStatic
        @Throws(IOException::class)
        private fun <T : Any?> complete(future: Future<T>): T {
            var interrupted: Boolean = false
            while (true) {
                try {
                    val result: T = future.get()
                    if (interrupted) Thread.currentThread().interrupt()
                    return result
                } catch (e: InterruptedException) {
                    interrupted = true
                } catch (e: ExecutionException) {
                    throw IOException(e.cause)
                }
            }
        }
    }

    private lateinit var channel: AsynchronousFileChannel

    private var position: Long = 0L

    constructor(fileName: String, mode: String) : this(fileName) {
        channel = AsynchronousFileChannel.open(
            Paths.get(fileName),
            *when (mode) {
                "r" -> R
                "w" -> W
                "rws" -> RWS
                "rwd" -> RWD
                else -> throw IllegalArgumentException(mode)
            })
    }

    @Throws(IOException::class)
    override fun implCloseChannel() = channel.close()

    @Throws(IOException::class)
    override fun position(): Long = position

    @Throws(IOException::class)
    override fun size(): Long = channel.size()

    @Throws(IOException::class)
    override fun read(dst: ByteBuffer?): Int {
        val read: Int = complete(channel.read(dst, position))
        if (read > 0) position += read
        return read
    }

    @Throws(IOException::class)
    override fun position(pos: Long): FileChannel {
        if (pos < 0) throw java.lang.IllegalArgumentException("pos is minus")
        position = pos
        return this
    }

    @Throws(IOException::class)
    override fun read(dst: ByteBuffer?, position: Long): Int = complete(channel.read(dst, position))

    @Throws(IOException::class)
    override fun write(src: ByteBuffer?, pos: Long): Int = try {
        complete(channel.write(src, pos))
    } catch (e: NonWritableChannelException) {
        throw IOException("$name is read only")
    }

    @Throws(IOException::class)
    override fun truncate(newLen: Long): FileChannel {
        channel.truncate(newLen)
        if (newLen < position) position = newLen
        return this
    }

    @Throws(IOException::class)
    override fun force(metaData: Boolean) = channel.force(metaData)

    @Throws(IOException::class)
    override fun write(src: ByteBuffer?): Int {
        var written: Int = 0
        try {
            written = complete(channel.write(src, position))
            position += written
        } catch (e: NonWritableChannelException) {
            throw IOException("$name is read only")
        }
        return written
    }

    @Throws(IOException::class)
    override fun tryLock(position: Long, size: Long, shared: Boolean): FileLock = channel.tryLock(position, size, shared)

    override fun toString(): String = "async:$name"
}

/**
 * This file system stores files on disk and uses
 * java.nio.channels.AsynchronousFileChannel to access the files.
 */
class FilePathAsync : FilePathWrapper {
    companion object {
        val AVAILABLE: Boolean = try {
            AsynchronousFileChannel::javaClass.name
            true
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Creates new instance of FilePathAsync
     */
    constructor() {
        if (!AVAILABLE) throw UnsupportedOperationException("NIO2 is not available")
    }

    @Throws(IOException::class)
    override fun open(mode: String): FileChannel = FileAsync(name.substring(scheme.length + 1), mode)

    override val scheme: String = "async"
}