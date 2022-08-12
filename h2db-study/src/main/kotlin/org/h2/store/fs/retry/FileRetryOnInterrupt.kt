package org.h2.store.fs.retry

import org.h2.store.fs.FileBase
import org.h2.store.fs.FileUtils
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.ClosedChannelException
import java.nio.channels.FileChannel
import java.nio.channels.FileLock

/**
 * A file object that re-opens and re-tries the operation if the file was closed.
 */
open class FileRetryOnInterrupt(private val fileName: String,
                                private val mode: String) : FileBase() {

    companion object {
        /**
         * A wrapped file lock.
         */
        internal class FileLockRetry(
            /**
             * The base lock.
             */
            var base: FileLock,
            channel: FileChannel?) : FileLock(channel, base.position(), base.size(), base.isShared) {

            override fun isValid(): Boolean = base.isValid

            @Throws(IOException::class)
            override fun release() = base.release()
        }
    }

    private var channel: FileChannel? = null
    private var lock: FileLockRetry? = null

    init {
        open()
    }

    @Throws(IOException::class)
    private fun open() {
        channel = FileUtils.open(fileName, mode)
    }

    @Throws(IOException::class)
    private fun reLock() {
        if (lock == null) return
        try {
            lock!!.base.release()
        } catch (e: IOException) {
            // ignore
        }
        val l2: FileLock = channel!!.tryLock(lock!!.position(), lock!!.size(), lock!!.isShared) ?: throw IOException("Re-locking failed")
        lock!!.base = l2
    }

    @Throws(IOException::class)
    private fun reopen(i: Int, e: IOException) {
        if (i > 20) throw e

        if (e !is ClosedByInterruptException
            && e !is ClosedChannelException) {
            throw e
        }
        // clear the interrupt flag, to avoid re-opening many times
        Thread.interrupted()
        val before = channel!!
        // ensure we don't re-open concurrently;
        // sometimes we don't re-open, which is fine,
        // as this method is called in a loop
        synchronized(this) {
            if (before === channel) {
                open()
                reLock()
            }
        }
    }

    @Throws(IOException::class)
    override fun implCloseChannel() {
        try {
            channel!!.close()
        } catch (e: IOException) {
            // ignore
        }
    }

    @Throws(IOException::class)
    private inline fun <T> retryOp(op: () -> T): T? {
        var i: Int = 0
        while (true) {
            try {
                return op()
            } catch (e: IOException) {
                reopen(i, e)
            }
            i++
        }
    }

    @Throws(IOException::class)
    override fun position(): Long = retryOp { channel!!.position() }!!

    @Throws(IOException::class)
    override fun size(): Long = retryOp { channel!!.size() }!!

    @Throws(IOException::class)
    override fun read(dst: ByteBuffer?): Int {
        val pos = position()

        var i = 0; while (true) {
            try {
                return channel!!.read(dst)
            } catch (e: IOException) {
                reopen(i, e)
                position(pos)
            }; i++
        }
    }

    @Throws(IOException::class)
    override fun read(dst: ByteBuffer?, position: Long): Int = retryOp { channel!!.read(dst, position) }!!

    @Throws(IOException::class)
    override fun position(pos: Long): FileChannel = retryOp {
        apply { channel!!.position(pos) }
    }!!


    @Throws(IOException::class)
    override fun truncate(newLength: Long): FileChannel = retryOp {
        apply { channel!!.truncate(newLength) }
    }!!

    @Throws(IOException::class)
    override fun force(metaData: Boolean) = retryOp { channel!!.force(metaData) }!!

    @Throws(IOException::class)
    override fun write(src: ByteBuffer?): Int {
        val pos = position()

        var i = 0; while (true) {
            try {
                return channel!!.write(src)
            } catch (e: IOException) {
                reopen(i, e)
                position(pos)
            }; i++
        }
    }

    @Throws(IOException::class)
    override fun write(src: ByteBuffer?, position: Long): Int = retryOp { channel!!.write(src, position) }!!

    @Synchronized
    @Throws(IOException::class)
    override fun tryLock(position: Long, size: Long, shared: Boolean): FileLock? {
        val l = channel!!.tryLock(position, size, shared) ?: return null
        lock = FileRetryOnInterrupt.Companion.FileLockRetry(l, this)
        return lock
    }

    override fun toString(): String = "retry:$fileName"
}