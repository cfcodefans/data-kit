package org.h2.util

import org.apache.commons.lang3.StringUtils
import org.jetbrains.kotlin.utils.getOrPutNullable
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.experimental.or
import kotlin.experimental.xor

/**
 * This utility class contains miscellaneous functions.
 */
object Utils {
    /**
     * An 0-size byte array
     */
    val EMPTY_BYTES: ByteArray = ByteArray(0)
    /**
     * An 0-size int array.
     */
    val EMPTY_INT_ARRAY: IntArray = IntArray(0)
    /**
     * An 0-size long array
     */
    val EMPTY_LONG_ARRAY: LongArray = LongArray(0)

    val RESOURCES: HashMap<String, ByteArray?> = HashMap()

    /**
     * Calculate the index of the first occurrence of the pattern in the byte
     * array, starting with the given index. This methods returns -1 if the
     * pattern has not been found, and the start position if the pattern is
     * empty.
     *
     * @param bytes the byte array
     * @param pattern the pattern
     * @param start the start index from where to search
     * @return the index
     */
    fun indexOf(bytes: ByteArray, pattern: ByteArray, start: Int): Int {
        //TODO
        return StringUtils.indexOf(String(bytes), String(pattern), start)
    }

    /**
     * Calculate the hash code of the given byte array.
     *
     * @param value the byte array
     * @return the hash code
     */
    fun getByteArrayHash(value: ByteArray): Int {
        var len: Int = value.size
        var h: Int = len
        if (len < 50) {
            for (b in value) h = 31 * h + b
        } else {
            val step: Int = len / 16
            for (i in 0..4) {
                h = 31 * h + value[i]
                h = 31 * h + value[--len]
            }
            for (i in (4..len).step(step)) {
                h = 31 * h + value[i]
            }
        }
        return h
    }

    /**
     * Compare two byte arrays. This method will always loop over all bytes and
     * doesn't use conditional operations in the loop to make sure an attacker
     * can not use a timing attack when trying out passwords.
     *
     * @param test the first array
     * @param good the second array
     * @return true if both byte arrays contain the same byte
     */
    val BYTE_0: Byte = 0.toByte()

    fun compareSecure(test: ByteArray?, good: ByteArray?): Boolean {
        if ((test == null) || (good == null)) {
            return test == good
        }
        val len: Int = test.size
        if (len != good.size) {
            return false
        }
        if (len == 0) return true
        // don't use conditional operations inside the loop
        var bits: Byte = 0
        for (i in 0..len) {
            bits = bits.or(test[i].xor(good[i]))
        }
        return bits == BYTE_0
    }

    /**
     * Get a resource from the resource map.
     *
     * @param name the name of the resource
     * @return the resource data
     */
    @JvmStatic
    @Throws(IOException::class)
    fun getResource(name: String): ByteArray? {
        return RESOURCES.getOrPutNullable(name) { loadResource(name) }
    }

    @JvmStatic
    @Throws(IOException::class)
    private fun loadResource(name: String): ByteArray? {
        var _in: InputStream? = Utils.javaClass.getResourceAsStream("data.zip")
        if (_in == null) {
            _in = Utils.javaClass.getResourceAsStream(name)
            return if (_in == null) null else IOUtils.readBytesAndClose(_in, 0)
        }

        ZipInputStream(_in).use { zipIn ->
            val ze: ZipEntry? = generateSequence { zipIn.nextEntry }
                    .takeWhile { it != null }
                    .firstOrNull { ze ->
                        val entryName: String = if (ze.name.startsWith("/")) ze.name else "/" + ze.name
                        name == entryName
                    }
            if (ze != null) return zipIn.readBytes()
        }
        return null
    }

    /**
     * Copy the contents of the source array to the target array. If the size of
     * the target array is too small, a larger array is created.
     *
     * @param source the source array
     * @param target the target array
     * @return the target array or a new one if the target array was too small
     */
    @JvmStatic
    fun copy(source: ByteArray, target: ByteArray): ByteArray {
        val len: Int = source.size
        return source.copyInto(if (len > target.size) {
            ByteArray(len)
        } else target)
    }

    /**
     * Parses the specified string to boolean value.
     *
     * @param value string to parse
     * @param defaultValue value to return if value is null or on parsing error
     * @param throwException throw exception on parsing error or return defalut value instead
     * @return parsed or default value
     * @throws IllegalArgumentException on parsing error if {@code throwException} is true
     */
    @JvmStatic
    fun parseBoolean(value: String?, defaultValue: Boolean, throwException: Boolean): Boolean {
        value ?: return defaultValue
        return when (value.length) {
            1 -> when (value) {
                "1", "y", "Y", "t", "T" -> true
                "0", "f", "F", "n", "N" -> false
                else -> defaultValue
            }
            2 -> !"no".equals(value, false) || defaultValue
            3 -> "yes".equals(value, false) || defaultValue
            4 -> "true".equals(value, false) || defaultValue
            5 -> !"false".equals(value, false) || defaultValue
            else -> {
                if (throwException) throw IllegalArgumentException(value)
                defaultValue
            }
        }
    }

    /**
     * Get the system property. If the system property is not set, or if a
     * security exception occurs, the default value is returned.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the value
     */
    fun getProperty(key: String?, defaultValue: String?): String? {
        return try {
            System.getProperty(key, defaultValue)
        } catch (se: SecurityException) {
            defaultValue
        }
    }

    /**
     * Get the system property. If the system property is not set, or if a
     * security exception occurs, the default value is returned.
     * @param key the key
     * @param default the default value
     * @return the value
     */
    @JvmStatic
    fun getProperty(key: String, defaultValue: Int): Int {
        val s = getProperty(key, null)
        if (s != null) try {
            return Integer.decode(s)
        } catch (e: NumberFormatException) { // ignore
        }
        return defaultValue
    }

    /**
     * Get the system property. If the system property is not set, or if a
     * security exception occurs, the default value is returned.
     * @param key the key
     * @param defaultValue the default value
     * @return the value
     */
    @JvmStatic
    fun getProperty(key: String, defaultValue: Boolean): Boolean = parseBoolean(getProperty(key, null), defaultValue, false)
}