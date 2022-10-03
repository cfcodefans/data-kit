package org.h2.util

import org.apache.commons.lang3.StringUtils
import org.h2.api.ErrorCode.HEX_STRING_ODD_1
import org.h2.api.ErrorCode.HEX_STRING_WRONG_1
import org.h2.api.ErrorCode.STRING_FORMAT_ERROR_1
import org.h2.engine.SysProperties
import org.h2.message.DbException
import java.lang.ref.SoftReference
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * A few String utility functions
 */
object StringUtils {
    private lateinit var softCache: SoftReference<Array<String?>>
    private var softCacheCreatedNs: Long = 0L

    private val HEX: CharArray = "0123456789abcdef".toCharArray()
    private val HEX_DECODE: kotlin.IntArray = IntArray('f'.code + 1) { _ -> -1 }

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
        if (cache != null) return cache

        //create a new cache at most every 5 seconds
        //so that out of memory exceptions are not delayed
        if (softCacheCreatedNs != 0L
            && System.nanoTime() - softCacheCreatedNs < TimeUnit.SECONDS.toNanos(5)) {
            return null
        }
        try {
            cache = arrayOfNulls<String>(SysProperties.OBJECT_CACHE_SIZE)
            softCache = SoftReference(cache)
            return cache
        } finally {
            softCacheCreatedNs = System.nanoTime()
        }
    }

    /**
     * Get the string from the cache if possible. If the string has not been
     * found, it is added to the cache. If there is such a string in the cache,
     * that one is returned.
     *
     * @param s the original string
     * @return a string with the same content, if possible from the cache
     */
    fun cache(s: String?): String? {
        if (!SysProperties.OBJECT_CACHE) return s
        if (s.isNullOrEmpty()) return s

        val cache = getCache() ?: return s

        val hash = s.hashCode()
        val index = hash and SysProperties.OBJECT_CACHE_SIZE - 1
        val cached = cache[index]

        if (s == cached) return cached
        cache[index] = s

        return s
    }

    /**
     * Convert a string to uppercase using the English locale.
     * @param s the test to convert
     * @return the uppercase text
     */
    @JvmStatic
    fun toUpperEnglish(s: String): String {
        if (s.length > TO_UPPER_CACHE_MAX_ENTRY_LENGTH) return s.uppercase(Locale.ENGLISH)

        val index: Int = s.hashCode() and (TO_UPPER_CACHE_LENGTH - 1)
        var e: Array<String>? = TO_UPPER_CACHE[index]
        if (e != null && s == e[0]) return e[1]
        val s2 = s.uppercase(Locale.ENGLISH)
        e = arrayOf(s, s2)
        TO_UPPER_CACHE[index] = e
        return s2
    }

    /**
     * Replace all occurrences of the before string with the after string. Unlike
     * [String.replaceAll] this method reads `before`
     * and `after` arguments as plain strings and if `before` argument
     * is an empty string this method returns original string `s`.
     *
     * @param s the string
     * @param before the old text
     * @param after the new text
     * @return the string with the before string replaced
     */
    fun replaceAll(s: String?, before: String, after: String): String? {
        if (s.isNullOrEmpty()) return s
        var next = s.indexOf(before)
        if (next < 0 || before.isEmpty()) return s

        val buff = StringBuilder(s.length - before.length + after.length)
        var index = 0
        while (true) {
            buff.append(s, index, next).append(after)
            index = next + before.length
            next = s.indexOf(before, index)
            if (next < 0) {
                buff.append(s, index, s.length)
                break
            }
        }
        return buff.toString()
    }

    /**
     * Convert a string to lowercase using the English locale.
     *
     * @param s the text to convert
     * @return the lowercase text
     */
    fun toLowerEnglish(s: String): String = s.toLowerCase(Locale.ENGLISH)

    /**
     * Convert a string to a SQL literal. Null is converted to NULL. The text is
     * enclosed in single quotes. If there are any special characters, the
     * method STRINGDECODE is used.
     *
     * @param s the text to convert.
     * @return the SQL literal
     */
    fun quoteStringSQL(s: String?): String = if (s == null) "NULL" else quoteStringSQL(StringBuilder(s.length + 2), s).toString()

    /**
     * Convert a string to a SQL character string literal. Null is converted to
     * NULL. If there are any special characters, the Unicode character string
     * literal is used.
     *
     * @param builder
     * string builder to append result to
     * @param s the text to convert
     * @return the specified string builder
     */
    fun quoteStringSQL(builder: StringBuilder, s: String?): StringBuilder {
        return if (s == null)
            builder.append("NULL")
        else
            quoteIdentifierOrLiteral(builder, s, '\'')
    }

    private fun quoteIdentifierOrLiteral(builder: StringBuilder, s: String, q: Char): StringBuilder {
        val builderLength = builder.length
        builder.append(q)
        var i = 0
        val l = s.length
        while (i < l) {
            var cp = s.codePointAt(i)
            i += Character.charCount(cp)

            if (cp < ' '.code || cp > 127) {
                // need to start from the beginning
                builder.setLength(builderLength)
                builder.append("U&").append(q)
                i = 0
                while (i < l) {
                    cp = s.codePointAt(i)
                    i += Character.charCount(cp)
                    if (cp >= ' '.code && cp < 127) {
                        val ch = cp.toChar()
                        if (ch == q || ch == '\\') {
                            builder.append(ch)
                        }
                        builder.append(ch)
                    } else if (cp <= 0xffff) {
                        appendHex(builder.append('\\'), cp.toLong(), 2)
                    } else {
                        appendHex(builder.append("\\+"), cp.toLong(), 3)
                    }
                }
                break
            }

            if (cp == q.code) builder.append(q)
            builder.append(cp.toChar())
        }
        return builder.append(q)
    }

    /**
     * Enclose a string with double quotes. A double quote inside the string is
     * escaped using a double quote.
     *
     * @param s the text
     * @return the double-quoted text
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
            if (c == '"') builder.append(c)
            builder.append(c)
        }
        return builder.append('"')
    }

    /**
     * In a string, replace block comment marks with /++ .. ++/.
     *
     * @param sql the string
     * @return the resulting string
     */
    fun quoteRemarkSQL(sql: String?): String? = replaceAll(sql, "*/", "++/")?.let { replaceAll(it, "/*", "/++") }

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
                val d: Int = hex[s[i + i].code] shl 4 or hex[s[i + i + 1].code]
                mask = mask or d
                buff[i] = d.toByte()
            }
        } catch (e: ArrayIndexOutOfBoundsException) {
            throw DbException.get(HEX_STRING_WRONG_1, s)
        }
        if ((mask and 255.inv()) != 0) throw DbException.get(HEX_STRING_WRONG_1, s)
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
    fun addAsterisk(s: String, index: Int): String = s.let {
        val len: Int = s.length
        val i: Int = kotlin.math.min(index, len)
        StringBuilder(len + 3)
            .append(s, 0, index)
            .append("[*]")
            .append(s, i, len)
            .toString()
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
        throw DbException.convert(e)
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
    fun arraySplit(s: String?, separatorChar: Char, trim: Boolean): Array<String> {
        if (s == null) return emptyArray()
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

    /**
     * Trim a character from a string.
     * @param s the string
     * @param leading if leading characters should be removed
     * @param trailing if trailing characters should be removed
     * @param sp what to remove (only the first character is used) or null for a space
     * @return the trimmed string
     */
    @JvmStatic
    fun trim(s: String, sp: String?, leading: Boolean = true, trailing: Boolean = true): String {
        val space: Char = if (sp.isNullOrEmpty()) ' ' else sp[0]
        var begin = 0
        var end = s.length

        if (leading) {
            while (begin < end && s[begin] == space) begin++
        }
        if (trailing) {
            while (end > begin && s[end - 1] == space) end--
        }
        return s.substring(begin, end)
    }

    /**
     * Convert a byte array to a hex encoded string and appends it to a specified string builder.
     *
     * @param builder string builder to append to
     * @param value the byte array
     * @return the hex encoded string
     */
    fun convertBytesToHex(builder: StringBuilder?, value: ByteArray): java.lang.StringBuilder? {
        return convertBytesToHex(builder!!, value, value.size)
    }

    /**
     * Convert a byte array to a hex encoded string.
     *
     * @param value the byte array
     * @param len the number of bytes to encode
     * @return the hex encoded string
     */
    fun convertBytesToHex(value: ByteArray, len: Int): String {
        val bytes = ByteArray(len * 2)
        val hex = HEX
        var i = 0
        var j = 0
        while (i < len) {
            val c: Int = value[i].toInt() and 0xff
            bytes[j++] = hex[c shr 4].code.toByte()
            bytes[j++] = hex[c and 0xf].code.toByte()
            i++
        }
        return String(bytes, StandardCharsets.ISO_8859_1)
    }

    /**
     * Convert a byte array to a hex encoded string and appends it to a specified string builder.
     *
     * @param builder string builder to append to
     * @param value the byte array
     * @param len the number of bytes to encode
     * @return the hex encoded string
     */
    fun convertBytesToHex(builder: java.lang.StringBuilder, value: ByteArray, len: Int): java.lang.StringBuilder? {
        val hex = HEX
        for (i in 0 until len) {
            val c: Int = value[i].toInt() and 0xff
            builder.append(hex[c ushr 4]).append(hex[c and 0xf])
        }
        return builder
    }

    /**
     * Parses an unsigned 31-bit integer. Neither - nor + signs are allowed.
     *
     * @param s string to parse
     * @param start the beginning index, inclusive
     * @param end the ending index, exclusive
     * @return the unsigned `int` not greater than [Integer.MAX_VALUE].
     */
    fun parseUInt31(s: String, start: Int, end: Int): Int {
        if (end > s.length || start < 0 || start > end) throw IndexOutOfBoundsException()
        if (start == end) throw NumberFormatException("")
        var result = 0
        for (i in start until end) {
            val ch = s[i]
            // Ensure that character is valid and that multiplication by 10 will
            // be performed without overflow
            if (ch < '0' || ch > '9' || result > 214748364) throw NumberFormatException(s.substring(start, end))
            result = result * 10 + ch.code - '0'.code
            // Overflow
            if (result < 0) throw NumberFormatException(s.substring(start, end))
        }
        return result
    }

    /**
     * Append a zero-padded number from 00 to 99 to a string builder.
     *
     * @param builder the string builder
     * @param positiveValue the number to append
     * @return the specified string builder
     */
    fun java.lang.StringBuilder.appendTwoDigits(positiveValue: Int): java.lang.StringBuilder = apply {
        if (positiveValue < 10) this.append('0')
        this.append(positiveValue)
    }

    /**
     * Pad a string. This method is used for the SQL function RPAD and LPAD.
     *
     * @param string the original string
     * @param n the target length
     * @param padding the padding string
     * @param right true if the padding should be appended at the end
     * @return the padded string
     */
    fun pad(string: String, n: Int, padding: String?, right: Boolean): String? {
        val paddingChar: Char = (if (padding.isNullOrEmpty()) ' ' else padding[0])
        return if (right) string.padEnd(n, paddingChar) else string.padStart(n, paddingChar)
    }

    /**
     * Convert a byte array to a hex encoded string.
     *
     * @param value the byte array
     * @return the hex encoded string
     */
    fun convertBytesToHex(value: ByteArray): String = convertBytesToHex(value, value.size)

    /**
     * Appends specified number of trailing bytes from unsigned long value to a
     * specified string builder.
     *
     * @param builder string builder to append to
     * @param x value to append
     * @param bytes number of bytes to append
     * @return the specified string builder
     */
    fun appendHex(builder: StringBuilder, x: Long, bytes: Int): StringBuilder {
        var i = bytes * 8
        while (i > 0) {
            builder.append(HEX[(x shr 4.let { i -= it; i }).toInt() and 0xf])
                .append(HEX[(x shr 4.let { i -= it; i }).toInt() and 0xf])
        }
        return builder
    }

    /**
     * Append a zero-padded number to a string builder.
     *
     * @param builder the string builder
     * @param length the number of characters to append
     * @param positiveValue the number to append
     * @return the specified string builder
     */
    fun appendZeroPadded(builder: StringBuilder, length: Int, positiveValue: Long): StringBuilder? {
        val s = positiveValue.toString()
        return builder.append(s.padEnd(length - s.length, '0'))
    }

    fun <T> StringBuilder.appends(
        iterable: Iterable<T>,
        separator: String = ", ",
        prefix: String = "",
        postfix: String = "",
        truncated: CharSequence = "...",
        limit: Int = -1,
        transform: ((StringBuilder, T) -> Unit)? = null): StringBuilder = apply {
        this.append(prefix)
        var count: Int = 0
        for (element in iterable) {
            if (++count > 1) append(separator)
            if (limit < 0 || count <= limit) {
                transform?.invoke(this, element)
            } else break
        }
        if (limit >= 0 && count > limit) append(truncated)
        append(postfix)
    }

    /**
     * Indents a string with spaces and appends it to a specified builder.
     *
     * @param builder string builder to append to
     * @param s the string
     * @param spaces the number of spaces
     * @param newline append a newline if there is none
     * @return the specified string builder
     */
    open fun indent(builder: StringBuilder, s: String, spaces: Int, newline: Boolean): StringBuilder {
        var i = 0
        val length = s.length
        while (i < length) {
            for (j in 0 until spaces) builder.append(' ')

            var n = s.indexOf('\n', i)
            n = if (n < 0) length else n + 1
            builder.append(s, i, n)
            i = n
        }
        if (newline && !s.endsWith("\n")) builder.append('\n')
        return builder
    }

    /**
     * Enclose a string with double quotes. A double quote inside the string is
     * escaped using a double quote.
     *
     * @param s the text
     * @return the double quoted text
     */
    fun quoteIdentifier(s: String): String? {
        return quoteIdentifierOrLiteral(StringBuilder(s.length + 2), s, '"').toString()
    }
}