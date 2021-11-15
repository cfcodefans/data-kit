package org.h2.store.fs

import org.h2.store.fs.FileUtils.writeFully
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * Allows to write to a file channel like an output stream.
 */

class FileChannelOutputStream() : OutputStream() {
    val buffer: ByteArray = ByteArray(1)
    private lateinit var channel: FileChannel

    @Throws(IOException::class)
    constructor(channel: FileChannel, append: Boolean) : this() {
        this.channel = channel
        if (append) {
            channel.position(channel.size())
        } else {
            channel.position(0).truncate(0)
        }
    }

    @Throws(IOException::class)
    override fun write(b: Int) {
        buffer[0] = b.toByte()
        FileUtils.writeFully(channel, ByteBuffer.wrap(buffer))
    }

    @Throws(IOException::class)
    override fun write(b: ByteArray) {
        FileUtils.writeFully(channel, ByteBuffer.wrap(b))
    }

    @Throws(IOException::class)
    override fun write(b: ByteArray, off: Int, len: Int) {
        writeFully(channel, ByteBuffer.wrap(b, off, len))
    }

    @Throws(IOException::class)
    override fun close() = channel.close()
}