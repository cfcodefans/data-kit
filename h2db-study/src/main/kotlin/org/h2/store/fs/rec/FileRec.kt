package org.h2.store.fs.rec

import org.h2.store.fs.FileBase
import org.h2.store.fs.Recorder
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.util.Arrays

/**
 * A file object that records all write operations and can re-play them.
 */
class FileRec(private val rec: FilePathRec,
              private val channel: FileChannel,
              private val name: String) : FileBase() {

    @Throws(IOException::class)
    override fun implCloseChannel() {
        channel.close()
    }

    @Throws(IOException::class)
    override fun position(): Long = channel.position()

    @Throws(IOException::class)
    override fun size(): Long = channel.size()

    @Throws(IOException::class)
    override fun read(dst: ByteBuffer?): Int = channel.read(dst)

    @Throws(IOException::class)
    override fun read(dst: ByteBuffer?, position: Long): Int = channel.read(dst, position)

    @Throws(IOException::class)
    override fun position(pos: Long): FileChannel = apply {
        channel.position(pos)
    }

    @Throws(IOException::class)
    override fun truncate(newLength: Long): FileChannel = apply {
        rec.log(Recorder.TRUNCATE, name, null, newLength)
        channel.truncate(newLength)
    }

    @Throws(IOException::class)
    override fun force(metaData: Boolean) {
        channel.force(metaData)
    }

    @Throws(IOException::class)
    override fun write(src: ByteBuffer): Int {
        var buff = src.array()
        val len = src.remaining()
        if (src.position() != 0 || len != buff.size) {
            val offset = src.arrayOffset() + src.position()
            buff = Arrays.copyOfRange(buff, offset, offset + len)
        }
        val result = channel.write(src)
        rec.log(Recorder.WRITE, name, buff, channel.position())
        return result
    }

    @Throws(IOException::class)
    @Synchronized
    override fun write(src: ByteBuffer?, position: Long): Int {
        var buff = src!!.array()
        val len = src.remaining()
        if (src.position() != 0 || len != buff.size) {
            val offset = src.arrayOffset() + src.position()
            buff = Arrays.copyOfRange(buff, offset, offset + len)
        }
        val result = channel.write(src, position)
        rec.log(Recorder.WRITE, name, buff, position)
        return result
    }

    @Synchronized
    @Throws(IOException::class)
    override fun tryLock(position: Long, size: Long, shared: Boolean): FileLock = channel.tryLock(position, size, shared)

    override fun toString(): String = name
}