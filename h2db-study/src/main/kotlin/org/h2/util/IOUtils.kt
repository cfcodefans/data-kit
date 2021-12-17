package org.h2.util

import org.h2.engine.Constants
import org.h2.message.DbException
import org.h2.mvstore.DataUtils
import java.io.BufferedReader
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.Reader
import java.io.StringWriter
import java.io.Writer
import java.nio.charset.StandardCharsets
import kotlin.math.min

/**
 * This utility class contains input/output functions.
 */
object IOUtils {
    /**
     * Close an AutoCloseable without throwing an excepton.
     *
     * @param out the AutoCloseable or null
     */
    fun closeSilently(out: AutoCloseable?): Unit {
        out?.runCatching {
            trace("closeSiliently", null, out)
            this.close()
        }
    }

    /**
     * Skip a number of bytes in an input stream.
     *
     * @param `in` the input stream
     * @param skip the number of bytes to skip
     * @throws EOFException if the end of file has been reached before all bytes
     * could be skipped
     * @throws IOException if an IO exception occurred while skipping
     */
    @Throws(IOException::class)
    fun skipFully(inputStream: InputStream, skip: Long) {
        var toSkip = skip
        try {
            while (toSkip > 0) {
                val skipped = inputStream.skip(toSkip)
                if (skipped <= 0) throw EOFException()
                toSkip -= skipped
            }
        } catch (e: Exception) {
            throw DataUtils.convertToIOException(e)
        }
    }

    /**
     * Skip a number of characters in a reader.
     *
     * @param reader the reader
     * @param skip the number of characters to skip
     * @throws EOFException if the end of file has been reached before all
     * characters could be skipped
     * @throws IOException if an IO exception occurred while skipping
     */
    @Throws(IOException::class)
    fun skipFully(reader: Reader, skip: Long) {
        var skip = skip
        try {
            while (skip > 0) {
                val skipped = reader.skip(skip)
                if (skipped <= 0) {
                    throw EOFException()
                }
                skip -= skipped
            }
        } catch (e: java.lang.Exception) {
            throw DataUtils.convertToIOException(e)
        }
    }

    /**
     * Trace input or output operations if enabled
     *
     * @param method the method from where this method was called
     * @param fileName the file name
     * @param o the object to append to the message
     */
    fun trace(method: String, fileName: String?, o: Any?) {
        //TODO
        println("IOUtils.$method $fileName $o")
    }

    /**
     * Copy all data from the reader to the writer and close the reader.
     * Exceptions while closing are ignored.
     *
     * @param in the reader
     * @param out the writer (null if writing is not required)
     * @param length the maximum number of bytes to copy
     * @return the number of characters copied
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun copyAndCloseInput(`in`: Reader, out: Writer?, length: Long): Long {
        val bufSize: Long = Constants.IO_BUFFER_SIZE.toLong()
        var toCopy = length
        return try {
            var copied: Long = 0
            var len = toCopy.coerceAtMost(bufSize).toInt()
            val buffer = CharArray(len)
            while (toCopy > 0) {
                len = `in`.read(buffer, 0, len)
                if (len < 0) break
                out?.write(buffer, 0, len)
                copied += len.toLong()
                toCopy -= len.toLong()
                len = toCopy.coerceAtMost(bufSize).toInt()
            }
            copied
        } catch (e: java.lang.Exception) {
            throw DataUtils.convertToIOException(e)
        } finally {
            `in`.close()
        }
    }

    /**
     * Read a number of bytes from an input stream and close the stream
     *
     * @param in the input stream
     * @param length the maximum number of bytes to read, or -1 to read until
     *                  the end of file
     * @return the bytes read
     */
    @Throws(IOException::class)
    fun readBytesAndClose(`in`: InputStream, length: Int): ByteArray {
        `in`.use {
            try {
                if (length <= 0) return `in`.readAllBytes()
                val buf: ByteArray = ByteArray(length)
                `in`.readNBytes(buf, 0, length)
                return buf
            } catch (e: Exception) {
                throw DbException.convertToIOException(e)
            }
        }
    }

    /**
     * Read a number of characters from a reader and close it.
     *
     * @param in the reader
     * @param length the maximum number of characters to read, or -1 to read
     * until the end of file
     * @return the string read
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun readStringAndClose(`in`: Reader, length: Int): String {
        var len = if (length <= 0) Int.MAX_VALUE else length
        return `in`.use { `in` ->
            val block = Constants.IO_BUFFER_SIZE.coerceAtMost(len)
            val out = StringWriter(block)
            copyAndCloseInput(`in`, out, len.toLong())
            out.toString()
        }
    }

    /**
     * Try to read the given number of bytes to the buffer. This method reads
     * until the maximum number of bytes have been read or until the end of
     * file.
     *
     * @param in the input stream
     * @param buffer the output buffer
     * @param max the number of bytes to read at most
     * @return the number of bytes read, 0 meaning EOF
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun readFully(`in`: InputStream, buffer: ByteArray, max: Int): Int = try {
        var result = 0
        var len = max.coerceAtMost(buffer.size)
        while (len > 0) {
            val l = `in`.read(buffer, result, len)
            if (l < 0) break
            result += l
            len -= l
        }
        result
    } catch (e: java.lang.Exception) {
        throw DataUtils.convertToIOException(e)
    }

    /**
     * Try to read the given number of characters to the buffer. This method
     * reads until the maximum number of characters have been read or until the
     * end of file.
     *
     * @param `in` the reader
     * @param buffer the output buffer
     * @param max the number of characters to read at most
     * @return the number of characters read, 0 meaning EOF
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun readFully(rd: Reader, buffer: CharArray, max: Int): Int = try {
        var result = 0
        var len = max.coerceAtMost(buffer.size)
        while (len > 0) {
            val l = rd.read(buffer, result, len)
            if (l < 0) break
            result += l
            len -= l
        }
        result
    } catch (e: java.lang.Exception) {
        throw DataUtils.convertToIOException(e)
    }

    /**
     * Copy all data from the input stream to the output stream. Both streams
     * are kept open.
     *
     * @param in the input stream
     * @param out the output stream (null if writing is not required)
     * @return the number of bytes copied
     */
    @Throws(IOException::class)
    fun copy(`in`: InputStream, out: OutputStream?): Long = copy(`in`, out, Long.MAX_VALUE)

    /**
     * Copy all data from the input stream to the output stream. Both streams
     * are kept open.
     *
     * @param in the input stream
     * @param out the output stream (null if writing is not required)
     * @param length the maximum number of bytes to copy
     * @return the number of bytes copied
     */
    @Throws(IOException::class)
    fun copy(`in`: InputStream, out: OutputStream?, length: Long): Long {
        if (out == null) return -1
        try {
            return `in`.copyTo(out, min(length.toInt(), Constants.IO_BUFFER_SIZE))
        } catch (e: Exception) {
            throw DbException.convertToIOException(e)
        }
    }

    /**
     * Create a reader to read from an input stream using the UTF-8 format. If
     * the input stream is null, this method returns null. The InputStreamReader
     * that is used here is not exact, that means it may read some additional
     * bytes when buffering.
     *
     * @param in the input stream or null
     * @return the reader
     */
    fun getReader(`in`: InputStream?): Reader? {
        // InputStreamReader may read some more bytes
        return `in`?.let { BufferedReader(InputStreamReader(it, StandardCharsets.UTF_8)) }
    }
}