package org.h2.util

import org.apache.commons.lang3.StringUtils
import org.h2.api.ErrorCode.HEX_STRING_ODD_1
import org.h2.api.ErrorCode.HEX_STRING_WRONG_1
import org.h2.api.ErrorCode.STRING_FORMAT_ERROR_1
import org.h2.engine.SysProperties
import org.h2.message.DbException
import java.lang.Math.min
import java.lang.ref.SoftReference
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * A few String utility functions
 */
object StringUtils {
    private lateinit var softCache: SoftReference<Array<String?>>
    private var softCacheCreatedNs: Long = 0L

    private val HEX: CharArray = "0123456789abcdef".toCharArray()
    private val HEX_DECODE: kotlin.IntArray = IntArray('f'.toInt() + 1) { _ -> -1 }

    // memory used by this cache:
    // 4 * 1024 * 2 (strings per pair) * 64 * 2 (bytes per char) = 0.5 MB
    private val TO_UPPER_CACHE_LENGTH: Int = 2048
    private val TO_UPPER_CACHE_MAX_ENTRY_LENGTH: Int = 64
    private val TO_UPPER_CACHE: Array<Array<String>?> = arrayOfNulls(TO_UPPER_CACHE_LENGTH)

    init {
        for (i in 0..9) {
            HEX_DECODE[i + '0'.toInt()] = i
        }
        for (i in 0..5) {
            HEX_DECODE[i + 'A'.toInt()] = i + 10
            HEX_DECODE[i + 'a'.toInt()] = HEX_DECODE[i + 'A'.toInt()]
        }
    }

    @JvmStatic
    private fun getCache(): Array<String?>? {
        var cache: Array<String?>? = softCache.get()
        if (cache != null)
            return cache

        //create a new cache at most every 5 seconds
        //so that out of memory exceptions are not delayed
        if (softCacheCreatedNs != 0L
                && System.nanoTime() - softCacheCreatedNs < TimeUnit.SECONDS.toNanos(5)) {
            return null
        }
        try {
            cache = Array(SysProperties.OBJECT_CACHE_SIZE) { _ -> null }
            softCache = SoftReference(cache)
            return cache
        } finally {
            softCacheCreatedNs = System.nanoTime()
        }
    }

    /**
     * Convert a string to uppercase using the English locale.
     * @param s the test to convert
     * @return the uppercase text
     */
    @JvmStatic
    fun toUpperEnglish(s: String): String {
        if (s.length > TO_UPPER_CACHE_MAX_ENTRY_LENGTH) {
            return s.toUpperCase(Locale.ENGLISH)
        }
        val index: Int = s.hashCode() and (TO_UPPER_CACHE_LENGTH - 1)
        var e: Array<String>? = TO_UPPER_CACHE[index]
        if (e != null && s == e[0]) return e[1]
        val s2 = s.toUpperCase(Locale.ENGLISH)
        e = arrayOf(s, s2)
        TO_UPPER_CACHE[index] = e
        return s2
    }

    /**
     * Convert a string to lowercase using the English locale.
     *
     * @param s the text to convert
     * @return the lowercase text
     */
    fun toLowerEnglish(s: String): String = s.toLowerCase(Locale.ENGLISH)

    /**
     * Enclose a string with double quotes. A double quote inside the string is
     * escaped using a double quote.
     *
     * @param s the text
     * @return the double quoted text
     */
    fun quotedIdentifier(s: String): String {
        return quoteIdentifier(StringBuilder(s.length + 2), s).toString()
    }

    /**
     * Enclose a string with double quotes and append it to the specified
     * string builder. A double quote inside the string is escaped using a
     * double quote.
     *
     * @param builder string builder to append to
     * @param s the text
     * @return the specified builder
     */
    fun quoteIdentifier(builder: StringBuilder, s: String): StringBuilder {
        builder.append('"')
        for (c in s) {
            if (c == '"')
                builder.append(c)
            builder.append(c)
        }
        return builder.append('"')
    }

    /**
     * Convert a hex encoded string to a byte array
     * @param s the hex encoded string
     * @return the byte array
     */
    @JvmStatic
    fun convertHexToBytes(s: String): ByteArray {
        var len: Int = s.length
        if (len % 2 != 0) throw DbException.get(HEX_STRING_ODD_1, s)
        len /= 2
        val buff: ByteArray = ByteArray(len)
        var mask: Int = 0
        val hex: kotlin.IntArray = HEX_DECODE
        try {
            for (i in 0..len) {
                val d: Int = hex[s[i + i].toInt()] shl 4 or hex[s[i + i + 1].toInt()]
                mask = mask or d
                buff[i] = d.toByte()
            }
        } catch (e: ArrayIndexOutOfBoundsException) {
            throw DbException.get(HEX_STRING_WRONG_1, s)
        }
        if ((mask and 255.inv()) != 0) {
            throw DbException.get(HEX_STRING_WRONG_1, s)
        }
        return buff
    }

    /**
     * Convert a string to a Java literal using the correct escape sequences.
     * The literal is not enclosed in double quotes. The result can be used in
     * properties files or in Java source code.
     * @param s the text to convert
     * @param buff the Java representation to return
     * @param forSQL true if we embed this inside a STRINGDECODE SQL command
     */
    @JvmStatic
    fun javaEncode(s: String, buff: StringBuilder, forSQL: Boolean): Unit {
        for (c in s) when (c) {
            '\t' -> buff.append("\\t")
            '\n' -> buff.append("\\n")
//                '\f' -> buff.append("\\f")
            '\r' -> buff.append("\\r")
            '"' -> buff.append("\\\"")
            '\'' -> {
                if (forSQL) buff.append('\'')
                buff.append('\'')
            }
            '\\' -> buff.append("\\\\")
            else -> {
                if (c >= ' ' && c < 0x80.toChar()) {
                    buff.append(c)
                } else {
                    buff.append("\\u")
                            .append(HEX[c.toInt() shr 12])
                            .append(HEX[c.toInt() shr 8 and 0xf])
                            .append(HEX[c.toInt() shr 4 and 0xf])
                            .append(HEX[c.toInt() and 0xf])
                }
            }
        }
    }

    /**
     * Add an asterisk ('[*]') at the given position. This format is used to
     * show where parsing failed in a statement.
     * @param s the text
     * @param index the position
     * @return the text with asterisk.
     */
    @JvmStatic
    fun addAsterisk(s: String, index: Int): String {
        return s?.let {
            val len: Int = s.length
            val i: Int = min(index, len)
            StringBuilder(len + 3)
                    .append(s, 0, index)
                    .append("[*]")
                    .append(s, i, len)
                    .toString()
        }
    }

    /**
     * Decode a text that is encoded as a Java string literal. The Java
     * properties file format and Java source code format is supported.
     * @param s the encoded string
     * @return the string
     */
    @JvmStatic
    fun javaDecode(s: String): String {
        val buf: StringBuilder = StringBuilder(s.length)
//        for ((i, c) in s.withIndex()) {
        var i: Int = 0
        while (i < s.length) {
            val c: Char = s[i]
            if (c != '\\') {
                buf.append(c)
                continue
            }
            if (i + 1 >= s.length) throw getFormatException(s, i)
            when (val c1: Char = s[i + 1]) {
                't' -> buf.append('\t')
                'r' -> buf.append('\r')
                'n' -> buf.append('\n')
                'b' -> buf.append('\b')
//                'f' -> buf.append('\f')
                '#' -> buf.append('#') // for properties file
                '=' -> buf.append('=') // for properties file
                ':' -> buf.append(':') // for properties file
                '"' -> buf.append('"')
                '\\' -> buf.append('\\')
                'u' -> {
                    buf.append(try {
                        s.substring(i + 1, i + 5).toInt(16)
                    } catch (e: NumberFormatException) {
                        throw getFormatException(s, i)
                    })
                    i += 4
                }
                else -> {
                    if (c1 >= '0' && c1 <= '9') {
                        buf.append(try {
                            s.substring(i, i + 3).toInt(8)
                        } catch (e: NumberFormatException) {
                            throw getFormatException(s, i)
                        })
                        i += 2
                    } else {
                        throw getFormatException(s, i)
                    }
                }
            }
        }
        return buf.toString()
    }

    private fun getFormatException(s: String, i: Int): DbException {
        return DbException.get(STRING_FORMAT_ERROR_1, addAsterisk(s, i))
    }

    /**
     * Convert a string to the Java literal and enclose it with double quotes.
     * Null will result in "null" (without double quotes).
     * @param s the text to convert
     * @return the Java representation
     */
    @JvmStatic
    fun quoteJavaString(s: String?): String {
        s ?: return "null"
        val builder = java.lang.StringBuilder(s.length + 2).append('"')
        javaEncode(s, builder, false)
        return builder.append('"').toString()
    }

    /**
     * Convert an int array to the Java source code that represents this array.
     * Null will be converted to 'null'.
     * @param array the int array
     * @return the Java source code (including new int[]{})
     */
    @JvmStatic
    fun quoteJavaIntArray(array: IntArray?): String {
        array ?: return "null"
        return "new int[]{${StringUtils.join(array, ", ")}}" //TODO preformance
    }

    /**
     * Remove enclosing '(' and ')' if this text is enclosed.
     * @param s the potentially enclosed string
     * @return the string
     */
    @JvmStatic
    fun unEnclose(s: String): String =
            if (s.startsWith('(') && s.endsWith(')'))
                s.substring(1, s.length - 1)
            else s

    /**
     * Encode the string as a URL
     * @param s the string to encode
     * @return the encoded string
     */
    @JvmStatic
    fun urlEncode(s: String): String = try {
        URLEncoder.encode(s, "UTF-8")
    } catch (e: Exception) {
        throw  DbException.convert(e)
    }

    /**
     * Split a string into an array of strings using the given separator. A null
     * string will result in a null array, and an empty string in a zero element
     * array.
     *
     * @param s the string to split
     * @param separatorChar the separator character
     * @param trim whether each element should be trimmed
     * @return the array list
     */
    @JvmStatic
    fun arraySplit(s: String?, separatorChar: Char, trim: Boolean): Array<String>? {
        if (s == null) return null
        val len: Int = s.length
        if (len == 0) return emptyArray()
        val list: ArrayList<String> = Utils.newSmallArrayList()
        val sb: java.lang.StringBuilder = java.lang.StringBuilder(len)
        for (i in 0..len) {
            val c: Char = s[i]
            if (c == separatorChar) {
                val e: String = sb.toString()
                list += if (trim) e.trim() else e
                sb.setLength(0)
            } else if (c == '\\' && i < len - 1) {
                continue
            } else {
                sb.append(c)
            }
        }
        val e: String = sb.toString()
        list += if (trim) e.trim() else e
        return list.toTypedArray()
    }
}