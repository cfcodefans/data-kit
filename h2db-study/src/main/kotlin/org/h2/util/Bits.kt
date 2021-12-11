package org.h2.util

import java.util.UUID
import kotlin.math.min

/**
 * Manipulations with bytes and arrays. This class can be overridden in
 * multi-release JAR with more efficient implementation for a newer versions of
 * Java.
 */
object Bits {
    /*
 * Signatures of methods should match with
 * h2/src/java9/src/org/h2/util/Bits.java and precompiled
 * h2/src/java9/precompiled/org/h2/util/Bits.class.
 */

    /**
     * Compare the contents of two char arrays. If the content or length of the
     * first array is smaller than the second array, -1 is returned. If the content
     * or length of the second array is smaller than the first array, 1 is returned.
     * If the contents and lengths are the same, 0 is returned.
     *
     * @param data1
     * the first char array (must not be null)
     * @param data2
     * the second char array (must not be null)
     * @return the result of the comparison (-1, 1 or 0)
     */
    fun compareNotNull(data1: CharArray, data2: CharArray): Int {
        if (data1 === data2) return 0

        val len = min(data1.size, data2.size)
        for (i in 0 until len) {
            val b = data1[i]
            val b2 = data2[i]
            if (b != b2) {
                return if (b > b2) 1 else -1
            }
        }
        return Integer.signum(data1.size - data2.size)
    }

    /**
     * Compare the contents of two byte arrays. If the content or length of the
     * first array is smaller than the second array, -1 is returned. If the content
     * or length of the second array is smaller than the first array, 1 is returned.
     * If the contents and lengths are the same, 0 is returned.
     *
     * This method interprets bytes as signed.
     *
     * @param data1
     * the first byte array (must not be null)
     * @param data2
     * the second byte array (must not be null)
     * @return the result of the comparison (-1, 1 or 0)
     */
    fun compareNotNullSigned(data1: ByteArray, data2: ByteArray): Int {
        if (data1 === data2) return 0

        val len = min(data1.size, data2.size)

        for (i in 0 until len) {
            val b = data1[i]
            val b2 = data2[i]
            if (b != b2) {
                return if (b > b2) 1 else -1
            }
        }
        return Integer.signum(data1.size - data2.size)
    }

    /**
     * Compare the contents of two byte arrays. If the content or length of the
     * first array is smaller than the second array, -1 is returned. If the content
     * or length of the second array is smaller than the first array, 1 is returned.
     * If the contents and lengths are the same, 0 is returned.
     *
     * This method interprets bytes as unsigned.
     *
     * @param data1
     * the first byte array (must not be null)
     * @param data2
     * the second byte array (must not be null)
     * @return the result of the comparison (-1, 1 or 0)
     */
    fun compareNotNullUnsigned(data1: ByteArray, data2: ByteArray): Int {
        if (data1 === data2) return 0

        val len = min(data1.size, data2.size)
        for (i in 0 until len) {
            val b: Int = data1[i].toInt() and 0xff
            val b2: Int = data2[i].toInt() and 0xff
            if (b != b2) {
                return if (b > b2) 1 else -1
            }
        }
        return Integer.signum(data1.size - data2.size)
    }

    /**
     * Reads a int value from the byte array at the given position in big-endian
     * order.
     *
     * @param buff
     * the byte array
     * @param pos
     * the position
     * @return the value
     */
    fun readInt(buff: ByteArray, pos: Int): Int = (buff[pos].toInt() shl 24) +
            (buff[pos + 1].toInt() and 0xff shl 16) +
            (buff[pos + 2].toInt() and 0xff shl 8) +
            (buff[pos + 3].toInt() and 0xff)

    /**
     * Reads a int value from the byte array at the given position in
     * little-endian order.
     *
     * @param buff
     * the byte array
     * @param pos
     * the position
     * @return the value
     */
    fun readIntLE(buff: ByteArray, pos: Int): Int = (buff[pos].toInt() and 0xff) +
            (buff[pos + 1].toInt() and 0xff shl 8) +
            (buff[pos + 2].toInt() and 0xff shl 16) +
            (buff[pos + 3].toInt() shl 24)

    /**
     * Reads a long value from the byte array at the given position in
     * big-endian order.
     *
     * @param buff
     * the byte array
     * @param pos
     * the position
     * @return the value
     */
    fun readLong(buff: ByteArray?, pos: Int): Long = (readInt(buff!!, pos).toLong() shl 32) + (readInt(buff, pos + 4).toLong() and 0xffffffffL)

    /**
     * Reads a long value from the byte array at the given position in
     * little-endian order.
     *
     * @param buff
     * the byte array
     * @param pos
     * the position
     * @return the value
     */
    fun readLongLE(buff: ByteArray?, pos: Int): Long =
            (readIntLE(buff!!, pos).toLong() and 0xffffffffL) + (readIntLE(buff, pos + 4).toLong() shl 32)

    /**
     * Reads a double value from the byte array at the given position in
     * big-endian order.
     *
     * @param buff
     * the byte array
     * @param pos
     * the position
     * @return the value
     */
    fun readDouble(buff: ByteArray?, pos: Int): Double {
        return java.lang.Double.longBitsToDouble(readLong(buff, pos))
    }

    /**
     * Reads a double value from the byte array at the given position in
     * little-endian order.
     *
     * @param buff
     * the byte array
     * @param pos
     * the position
     * @return the value
     */
    fun readDoubleLE(buff: ByteArray?, pos: Int): Double {
        return java.lang.Double.longBitsToDouble(readLongLE(buff, pos))
    }

    /**
     * Converts UUID value to byte array in big-endian order.
     *
     * @param msb
     * most significant part of UUID
     * @param lsb
     * least significant part of UUID
     * @return byte array representation
     */
    fun uuidToBytes(msb: Long, lsb: Long): ByteArray? {
        val buff = ByteArray(16)
        for (i in 0..7) {
            buff[i] = (msb shr 8 * (7 - i) and 0xff).toByte()
            buff[8 + i] = (lsb shr 8 * (7 - i) and 0xff).toByte()
        }
        return buff
    }

    /**
     * Converts UUID value to byte array in big-endian order.
     *
     * @param uuid
     * UUID value
     * @return byte array representation
     */
    fun uuidToBytes(uuid: UUID): ByteArray? {
        return uuidToBytes(uuid.mostSignificantBits, uuid.leastSignificantBits)
    }

    /**
     * Writes a int value to the byte array at the given position in big-endian
     * order.
     *
     * @param buff
     * the byte array
     * @param pos
     * the position
     * @param x
     * the value to write
     */
    fun writeInt(buff: ByteArray, pos: Int, x: Int) {
        var pos = pos
        buff[pos++] = (x shr 24).toByte()
        buff[pos++] = (x shr 16).toByte()
        buff[pos++] = (x shr 8).toByte()
        buff[pos] = x.toByte()
    }

    /**
     * Writes a int value to the byte array at the given position in
     * little-endian order.
     *
     * @param buff
     * the byte array
     * @param pos
     * the position
     * @param x
     * the value to write
     */
    fun writeIntLE(buff: ByteArray, pos: Int, x: Int) {
        var pos = pos
        buff[pos++] = x.toByte()
        buff[pos++] = (x shr 8).toByte()
        buff[pos++] = (x shr 16).toByte()
        buff[pos] = (x shr 24).toByte()
    }

    /**
     * Writes a long value to the byte array at the given position in big-endian
     * order.
     *
     * @param buff
     * the byte array
     * @param pos
     * the position
     * @param x
     * the value to write
     */
    fun writeLong(buff: ByteArray, pos: Int, x: Long) {
        writeInt(buff, pos, (x shr 32).toInt())
        writeInt(buff, pos + 4, x.toInt())
    }

    /**
     * Writes a long value to the byte array at the given position in
     * little-endian order.
     *
     * @param buff
     * the byte array
     * @param pos
     * the position
     * @param x
     * the value to write
     */
    fun writeLongLE(buff: ByteArray, pos: Int, x: Long) {
        writeIntLE(buff, pos, x.toInt())
        writeIntLE(buff, pos + 4, (x shr 32).toInt())
    }

    /**
     * Writes a double value to the byte array at the given position in
     * big-endian order.
     *
     * @param buff
     * the byte array
     * @param pos
     * the position
     * @param x
     * the value to write
     */
    fun writeDouble(buff: ByteArray, pos: Int, x: Double) {
        writeLong(buff, pos, java.lang.Double.doubleToRawLongBits(x))
    }

    /**
     * Writes a double value to the byte array at the given position in
     * little-endian order.
     *
     * @param buff
     * the byte array
     * @param pos
     * the position
     * @param x
     * the value to write
     */
    fun writeDoubleLE(buff: ByteArray, pos: Int, x: Double) {
        writeLongLE(buff, pos, java.lang.Double.doubleToRawLongBits(x))
    }
}