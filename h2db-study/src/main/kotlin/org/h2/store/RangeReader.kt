package org.h2.store

import org.h2.util.IOUtils
import java.io.IOException
import java.io.Reader

/**
 * Reader that reads only a specified range from the source reader.
 *  Creates new instance of range reader.
 * @param r             source reader
 * @param offset offset of the range
 * @param limit length of the range
 * @throws IOException on I/O exception during seeking to the specified offset
 */
class RangeReader : Reader {

    private lateinit var r: Reader
    private var limit: Long = 0

    @Throws(IOException::class)
    constructor(r: Reader, offset: Long, limit: Long) {
        this.r = r
        this.limit = limit
        IOUtils.skipFully(r, offset)
    }

    @Throws(IOException::class)
    override fun read(): Int {
        if (limit <= 0) return -1
        val c = r.read()
        if (c >= 0) limit--
        return c
    }

    @Throws(IOException::class)
    override fun read(cbuf: CharArray, off: Int, len: Int): Int {
        if (limit <= 0) return -1

        val cnt = r.read(cbuf, off, len.coerceAtMost(limit.toInt()))
        if (cnt > 0) limit -= cnt.toLong()
        return cnt
    }

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        val skipped = r.skip(n.coerceAtMost(limit.toInt().toLong()))
        limit -= skipped
        return skipped
    }

    @Throws(IOException::class)
    override fun ready(): Boolean = if (limit > 0) r.ready() else false

    override fun markSupported(): Boolean = false

    @Throws(IOException::class)
    override fun mark(readAheadLimit: Int): Unit = throw IOException("mark() not supported")

    @Throws(IOException::class)
    override fun reset(): Unit = throw IOException("reset() not supported")

    @Throws(IOException::class)
    override fun close() = r.close()
}