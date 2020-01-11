package org.h2.util

import org.h2.engine.Constants
import org.h2.message.DbException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
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
                return `in`.readNBytes(if (length <= 0) Int.MAX_VALUE else length)
            } catch (e: Exception) {
                throw DbException.convertToIOException(e)
            }
        }
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
    fun copy(`in`: InputStream, out: OutputStream?): Long {
        return copy(`in`, out, Long.MAX_VALUE)
    }

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
}