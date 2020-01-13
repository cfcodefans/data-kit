package org.h2.util

import java.lang.ref.SoftReference
import java.util.concurrent.TimeUnit

/**
 * A few String utility functions
 */
object StringUtils {
    private lateinit var softCache: SoftReference<Array<String>>
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
    private fun getCache(): Array<String>? {
        var cache: Array<String>? = softCache?.get()
        if (cache != null)
            return cache

        val time: Long = System.nanoTime()
        if (softCacheCreatedNs != 0L && time - softCacheCreatedNs < TimeUnit.SECONDS.toNanos(5)) {
            return null
        }
        try {
            cache = Array()
        }
    }

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
}