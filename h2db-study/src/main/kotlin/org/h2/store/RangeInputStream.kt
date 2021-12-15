package org.h2.store

import org.h2.util.IOUtils
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Input stream that reads only a specified range from the source stream.
 * Creates new instance of range input stream.
 * @param in                source stream
 * @param offset            offset of the range
 * @param limit             length of the range
 * @throws IOException      on I/O exception during seeking to the specified offset
 */
class RangeInputStream(inputStream: InputStream, offset: Long, var limit: Long) : FilterInputStream(inputStream) {
    init {
        IOUtils.skipFully(inputStream, offset)
    }

    @Throws(IOException::class)
    override fun read(): Int {
        if (limit <= 0) return -1
        val b = `in`.read()
        if (b >= 0) limit--
        return b
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (limit <= 0) return -1
        var len = len.coerceAtMost(limit.toInt())

        val cnt = `in`.read(b, off, len)
        if (cnt > 0) limit -= cnt.toLong()
        return cnt
    }

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        var toSkip = n.coerceAtMost(limit.toInt().toLong())
        return `in`.skip(toSkip).also { limit -= toSkip }
    }

    @Throws(IOException::class)
    override fun available(): Int = `in`.available().coerceAtMost(limit.toInt())

    @Throws(IOException::class)
    override fun close() = `in`.close()

    override fun mark(readlimit: Int) {}

    @Synchronized
    @Throws(IOException::class)
    override fun reset(): Unit = throw IOException("mark/reset not supported")

    override fun markSupported(): Boolean = false
}