package org.h2.value

import org.h2.engine.Constants
import org.h2.engine.SysProperties
import org.h2.message.DbException
import org.h2.util.MathUtils
import org.h2.util.StringUtils
import org.h2.util.Utils
import java.nio.charset.StandardCharsets
import java.util.Arrays

/**
 * Implementation of the BINARY VARYING data type.
 */
class ValueVarbinary(value: ByteArray) : ValueBytesBase(value) {
    init {
        val length = value.size
        if (length > Constants.MAX_STRING_LENGTH) {
            throw DbException.getValueTooLongException(getTypeName(getValueType()),
                    StringUtils.convertBytesToHex(value, 41),
                    length.toLong())
        }
    }

    companion object {
        /**
         * Empty value.
         */
        val EMPTY = ValueVarbinary(Utils.EMPTY_BYTES)

        /**
         * Get or create a VARBINARY value for the given byte array.
         * Do not clone the date.
         *
         * @param b the byte array
         * @return the value
         */
        fun getNoCopy(b: ByteArray): ValueVarbinary {
            if (b.isEmpty()) return EMPTY

            val obj = ValueVarbinary(b)
            return if (b.size > SysProperties.OBJECT_CACHE_MAX_PER_ELEMENT_SIZE) {
                obj
            } else cache(obj) as ValueVarbinary
        }

        /**
         * Get or create a VARBINARY value for the given byte array.
         * Clone the data.
         *
         * @param b the byte array
         * @return the value
         */
        operator fun get(b: ByteArray): ValueVarbinary = if (b.isEmpty()) EMPTY else getNoCopy(Utils.cloneByteArray(b)!!)

        internal fun Value.convertToVarbinary(targetType: TypeInfo, conversionMode: Int, column: Any): ValueVarbinary {
            val v: ValueVarbinary = if (getValueType() == VARBINARY) {
                this as ValueVarbinary
            } else {
                getNoCopy(getBytesNoCopy()!!)
            }

            if (conversionMode == CONVERT_TO) return v

            val value = v.getBytesNoCopy()
            val length = value.size
            val p = MathUtils.convertLongToInt(targetType.precision)

            if (conversionMode == CAST_TO) return if (length > p) getNoCopy(value.copyOf(p)) else v
//ASSIGN_TO
            if (length > p) throw v.getValueTooLongException(targetType, column)
            return v
        }
    }

    override var type: TypeInfo? = null
        get() = field ?: this.let {
            field = TypeInfo(VARBINARY, value.size.toLong(), 0, null)
            field
        }

    override fun getValueType(): Int = VARBINARY

    override fun getString() = String(value, StandardCharsets.UTF_8)

}