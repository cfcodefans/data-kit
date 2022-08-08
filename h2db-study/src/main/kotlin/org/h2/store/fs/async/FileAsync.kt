package org.h2.store.fs.async

import org.h2.store.fs.FileBaseDefault
import org.h2.store.fs.FileUtils
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.FileLock
import java.nio.file.Paths
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

/**
 * File which uses NIO2 AsynchronousFileChannel.
 */
open class FileAsync(private val name: String,
                     private val channel: AsynchronousFileChannel) : FileBaseDefault() {

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        private fun <T> complete(future: Future<T>): T {
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

    @kotlin.jvm.Throws(IOException::class)
    constructor(filename: String, mode: String)
            : this(name = filename,
        channel = AsynchronousFileChannel.open(Paths.get(filename), FileUtils.modeToOptions(mode), null, *FileUtils.NO_ATTRIBUTES))

    @Throws(IOException::class)
    override fun implCloseChannel() = channel.close()

    @Throws(IOException::class)
    override fun size(): Long = channel.size()

    @Throws(IOException::class)
    override fun read(dst: ByteBuffer?, position: Long): Int {
        return complete(channel.read(dst, position))
    }

    @Throws(IOException::class)
    override fun write(src: ByteBuffer?, position: Long): Int {
        return complete(channel.write(src, position))
    }

    @Throws(IOException::class)
    override fun implTruncate(newLength: Long) {
        channel.truncate(newLength)
    }

    @Throws(IOException::class)
    override fun force(metaData: Boolean) {
        channel.force(metaData)
    }

    @Throws(IOException::class)
    override fun tryLock(position: Long, size: Long, shared: Boolean): FileLock {
        return channel.tryLock(position, size, shared)
    }

    override fun toString(): String = "async:$name"
}