package org.h2.value

import org.h2.util.Bits
import java.nio.charset.Charset
import java.text.CollationKey
import java.text.Collator

/**
 * The charset collator sorts strings according to the order in the given charset.
 */
class CharsetCollator(val charset: Charset? = null) : Collator() {
    companion object {
        /**
         * The comparator used to compare byte arrays.
         */
        val COMPARATOR = java.util.Comparator { data1: ByteArray?, data2: ByteArray? -> Bits.compareNotNullSigned(data1!!, data2!!) }
    }

    private inner class CharsetCollationKey internal constructor(source: String?) : CollationKey(source) {
        private val bytes: ByteArray

        init {
            bytes = this@CharsetCollator.toBytes(source!!)
        }

        override fun compareTo(target: CollationKey): Int = COMPARATOR.compare(bytes, target.toByteArray())
        override fun toByteArray(): ByteArray = bytes
    }

    override fun compare(source: String, target: String): Int {
        return COMPARATOR.compare(toBytes(source), toBytes(target))
    }

    /**
     * Convert the source to bytes, using the character set.
     *
     * @param source the source
     * @return the bytes
     */
    private fun toBytes(source: String): ByteArray = (if (strength <= SECONDARY) {
        // TODO perform case-insensitive comparison properly
        source.uppercase()
    } else source).toByteArray(charset!!)

    override fun getCollationKey(source: String?): CollationKey = CharsetCollationKey(source)

    override fun hashCode(): Int = 255
}