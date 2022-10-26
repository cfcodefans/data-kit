package org.h2.value

import org.h2.api.ErrorCode
import org.h2.engine.CastDataProvider
import org.h2.message.DbException
import org.h2.util.Bits
import org.h2.util.JdbcUtils
import org.h2.util.MathUtils
import org.h2.util.StringUtils
import java.util.UUID

/**
 * Implementation of the UUID data type.
 */
class ValueUuid(private val high: Long, private val low: Long) : Value() {
    companion object {
        /**
         * The precision of this value in number of bytes.
         */
        const val PRECISION = 16

        /**
         * The display size of the textual representation of a UUID.
         * Example: cd38d882-7ada-4589-b5fb-7da0ca559d9a
         */
        const val DISPLAY_SIZE = 36

        /**
         * Create a new UUID using the pseudo random number generator.
         *
         * @return the new UUID
         */
        fun getNewRandom(): ValueUuid? {
            var high: Long = MathUtils.secureRandomLong()
            var low: Long = MathUtils.secureRandomLong()
            // version 4 (random)
            high = high and 0xf000L.inv() or 0x4000L
            // variant (Leach-Salz)
            val mask2: Long = 0x800000000000000L shl 4 //TODO
            low = low and 0x3fffffffffffffffL or mask2
            return ValueUuid(high, low)
        }

        /**
         * Get or create a UUID for the given 16 bytes.
         *
         * @param binary the byte array
         * @return the UUID
         */
        operator fun get(binary: ByteArray): ValueUuid {
            val length = binary.size
            if (length != 16) {
                throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, "UUID requires 16 bytes, got $length")
            }
            return get(Bits.readLong(binary, 0), Bits.readLong(binary, 8))
        }

        /**
         * Get or create a UUID for the given high and low order values.
         *
         * @param high the most significant bits
         * @param low the least significant bits
         * @return the UUID
         */
        operator fun get(high: Long, low: Long): ValueUuid = cache(ValueUuid(high, low)) as ValueUuid

        /**
         * Get or create a UUID for the given Java UUID.
         *
         * @param uuid Java UUID
         * @return the UUID
         */
        operator fun get(uuid: UUID): ValueUuid = ValueUuid[uuid.mostSignificantBits, uuid.leastSignificantBits]

        /**
         * Get or create a UUID for the given text representation.
         *
         * @param s the text representation of the UUID
         * @return the UUID
         */
        operator fun get(s: String): ValueUuid? {
            var low: Long = 0
            var high: Long = 0
            var j = 0
            var i = 0
            val length = s.length
            while (i < length) {
                val c = s[i]
                low = if (c >= '0' && c <= '9') low shl 4 or (c - '0').toLong()
                else if (c >= 'a' && c <= 'f') low shl 4 or (c.code - ('a'.code - 0xa)).toLong()
                else if (c == '-') {
                    i++
                    continue
                } else if (c >= 'A' && c <= 'F') low shl 4 or (c.code - ('A'.code - 0xa)).toLong()
                else if (c <= ' ') {
                    i++
                    continue
                } else throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, s)

                if (++j == 16) {
                    high = low
                    low = 0
                }
                i++
            }
            if (j != 32) throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, s)
            return ValueUuid[high, low]
        }

        fun Value.convertToUuid(): ValueUuid = when (getValueType()) {
            UUID -> this as ValueUuid
            BINARY, VARBINARY -> ValueUuid[getBytesNoCopy()]
            JAVA_OBJECT -> JdbcUtils.deserializeUuid(getBytesNoCopy())
            CHAR, VARCHAR, VARCHAR_IGNORECASE -> ValueUuid[getString()!!]!!
            NULL -> throw DbException.getInternalError()
            else -> throw getDataConversionError(UUID)
        }
    }

    override fun hashCode(): Int = (high ushr 32 xor high xor (low ushr 32) xor low).toInt()

    override var type: TypeInfo? = TypeInfo.TYPE_UUID

    override fun getMemory(): Int = 32
    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder = addString(builder.append("UUID '")).append('\'')

    override fun getValueType(): Int = UUID

    private fun addString(builder: StringBuilder): StringBuilder {
        StringUtils.appendHex(builder, high shr 32, 4).append('-')
        StringUtils.appendHex(builder, high shr 16, 2).append('-')
        StringUtils.appendHex(builder, high, 2).append('-')
        StringUtils.appendHex(builder, low shr 48, 2).append('-')
        return StringUtils.appendHex(builder, low, 6)
    }

    override fun equals(other: Any?): Boolean = (other is ValueUuid) && (this.high == other.high && this.low == other.low)
    override fun compareTypeSafe(other: Value, mode: CompareMode?, provider: CastDataProvider?): Int {
        if (other === this) return 0
        val vu: ValueUuid = other as ValueUuid
        val cmp = java.lang.Long.compareUnsigned(high, vu.high)
        return if (cmp != 0) cmp else java.lang.Long.compareUnsigned(low, vu.low)
    }


    /**
     * Returns the UUID.
     * @return the UUID
     */
    fun getUuid(): UUID = UUID(high, low)

    override fun charLength(): Long = DISPLAY_SIZE.toLong()

    override fun octetLength(): Long = PRECISION.toLong()

    override fun getString(): String = addString(StringBuilder(36)).toString()
}

