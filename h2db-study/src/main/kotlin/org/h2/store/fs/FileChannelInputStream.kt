package org.h2.store.fs

import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.experimental.and

/**
 * Allows to read from a file channel like an input stream
 */
/**
 * Create a new file object input stream from the file channel.
 * @param channel the file channel
 * @param closeChannel whether closing the stream should close the channel
 */
class FileChannelInputStream(val channel: FileChannel, val closeChannel: Boolean) : InputStream() {
    private val buffer: ByteBuffer by lazy { ByteBuffer.allocate(1) }
    private var pos: Long = 0L

    @Throws(IOException::class)
    override fun read(): Int {
        buffer.rewind()
        val len: Int = channel.read(buffer, pos++)
        if (len < 0) return -1
        return (buffer[0] and 0xff.toByte()).toInt()
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray): Int = read(b, 0, b.size)

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val buff: ByteBuffer = ByteBuffer.wrap(b, off, len)
        val read: Int = channel.read(buff, pos)
        if (read == -1) return -1
        pos += read
        return read
    }

    @Throws(IOException::class)
    override fun close() {
        if (closeChannel) super.close()
    }
}