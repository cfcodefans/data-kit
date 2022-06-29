package org.h2.store

import org.h2.util.IOUtils
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.Reader

/**
 * This class is backed by an input stream and supports reading values and
 * variable size data.
 */
open class DataReader(private val `in`: InputStream) : Reader() {

    companion object {
        /**
         * Constructing such an EOF exception is fast, because the stack trace is
         * not filled in. If used in a static context, this will also avoid
         * classloader memory leaks.
         */
        internal class FastEOFException : EOFException() {
            @Synchronized
            override fun fillInStackTrace(): Throwable? = null

            companion object {
                private const val serialVersionUID = 1L
            }
        }
    }

    /**
     * Read a byte.
     * @return the byte
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    open fun readByte(): Byte {
        val x = `in`.read()
        if (x < 0) throw FastEOFException()
        return x.toByte()
    }

    /**
     * Read a variable size integer.
     * @return the value
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    open fun readVarInt(): Int {
        var b = readByte().toInt()
        if (b >= 0) return b

        var x = b and 0x7f
        b = readByte().toInt()
        if (b >= 0) return x or (b shl 7)

        x = x or (b and 0x7f shl 7)
        b = readByte().toInt()
        if (b >= 0) return x or (b shl 14)

        x = x or (b and 0x7f shl 14)
        b = readByte().toInt()
        return if (b >= 0) {
            x or (b shl 21)
        } else {
            x or (b and 0x7f shl 21) or (readByte().toInt() shl 28)
        }
    }

    /**
     * Read a variable size long.
     * @return the value
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    open fun readVarLong(): Long {
        var x = readByte().toLong()
        if (x >= 0) return x

        x = x and 0x7fL
        var s = 7
        while (true) {
            val b = readByte().toLong()
            x = x or (b and 0x7fL shl s)
            if (b >= 0) return x
            s += 7
        }
    }

    /**
     * Read a number of bytes.
     * @param buff the target buffer
     * @param len the number of bytes to read
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    open fun readFully(buff: ByteArray?, len: Int) {
        val got = IOUtils.readFully(`in`, buff!!, len)
        if (got < len) {
            throw FastEOFException()
        }
    }

    /**
     * Read a string from the stream.
     * @return the string
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    open fun readString(): String? {
        val len = readVarInt()
        return readString(len)
    }

    @Throws(IOException::class)
    private fun readString(len: Int): String? {
        val chars = CharArray(len)
        for (i in 0 until len) {
            chars[i] = readChar()
        }
        return String(chars)
    }

    /**
     * Read one character from the input stream.
     *
     * @return the character
     */
    @Throws(IOException::class)
    private fun readChar(): Char {
        val x = readByte().toInt() and 0xff
        return if (x < 0x80) {
            x.toChar()
        } else if (x >= 0xe0) {
            ((x and 0xf shl 12) +
                    (readByte().toInt() and 0x3f shl 6) +
                    (readByte().toInt() and 0x3f)).toChar()
        } else {
            ((x and 0x1f shl 6) +
                    (readByte().toInt() and 0x3f)).toChar()
        }
    }

    @Throws(IOException::class)
    override fun close() {
        // ignore
    }

    @Throws(IOException::class)
    override fun read(buff: CharArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        var i = 0
        return try {
            while (i < len) {
                buff[off + i] = readChar()
                i++
            }
            len
        } catch (e: EOFException) {
            if (i == 0) -1 else i
        }
    }
}