package org.h2.value

import org.h2.engine.Constants
import org.h2.engine.SysProperties
import org.h2.message.DbException
import org.h2.util.HasSQL
import org.h2.util.StringUtils
import org.h2.util.Utils
import java.nio.charset.StandardCharsets

/**
 * Implementation of the BINARY data type.
 */
class ValueBinary(value: ByteArray) : ValueBytesBase(value) {
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
         * Get or create a VARBINARY value for the given byte array.
         * Do not clone the date.
         *
         * @param b the byte array
         * @return the value
         */
        fun getNoCopy(b: ByteArray): ValueBinary? {
            val obj = ValueBinary(b)
            return if (b.size > SysProperties.OBJECT_CACHE_MAX_PER_ELEMENT_SIZE) {
                obj
            } else cache(obj) as ValueBinary
        }

        /**
         * Get or create a VARBINARY value for the given byte array.
         * Clone the data.
         *
         * @param b the byte array
         * @return the value
         */
        operator fun get(b: ByteArray): ValueBinary? {
            return ValueBinary.getNoCopy(Utils.cloneByteArray(b)!!)
        }
    }

    override var type: TypeInfo? = null
        get() {
            if (field == null)
                field = TypeInfo(BINARY, value.size.toLong(), 0, null)
            return field
        }

    override fun getValueType(): Int = BINARY

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder {
        if (sqlFlags and HasSQL.NO_CASTS == 0) {
            val length = value.size
            return super.getSQL(builder.append("CAST("), sqlFlags).append(" AS BINARY(").append(if (length > 0) length else 1).append("))")
        }
        return super.getSQL(builder, sqlFlags)
    }

    override fun getString(): String? = String(value, StandardCharsets.UTF_8)
}