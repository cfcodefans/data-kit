package org.h2.value

import org.h2.api.ErrorCode
import org.h2.engine.CastDataProvider
import org.h2.message.DbException
import org.h2.util.HasSQL
import java.math.BigDecimal
import kotlin.math.sign

/**
 * Implementation of the TINYINT data type.
 */
class ValueTinyint(val value: Byte) : Value() {
    companion object {
        /**
         * The precision in bits.
         */
        const val PRECISION = 8

        /**
         * The approximate precision in decimal digits.
         */
        const val DECIMAL_PRECISION = 3

        /**
         * The display size for a TINYINT.
         * Example: -127
         */
        const val DISPLAY_SIZE = 4


        /**
         * Get or create a TINYINT value for the given byte.
         *
         * @param i the byte
         * @return the value
         */
        operator fun get(i: Byte): ValueTinyint = cache(ValueTinyint(i)) as ValueTinyint

        private fun checkRange(x: Int): ValueTinyint {
            if (x.toByte().toInt() != x) {
                throw DbException.get(ErrorCode.NUMERIC_VALUE_OUT_OF_RANGE_1, x.toString())
            }
            return ValueTinyint[x.toByte()]
        }

        /**
         * Converts this value to a TINYINT value. May not be called on a NULL
         * value.
         *
         * @param column
         * the column, used for to improve the error message if
         * conversion fails
         * @return the TINYINT value
         */
        fun Value.convertToTinyint(column: Any?): ValueTinyint = when (getValueType()) {
            TINYINT -> this as ValueTinyint
            CHAR, VARCHAR, VARCHAR_IGNORECASE, BOOLEAN -> ValueTinyint[getByte()]
            SMALLINT, ENUM, INTEGER -> ValueTinyint[Value.convertToByte(getInt().toLong(), column)]
            BIGINT, INTERVAL_YEAR, INTERVAL_MONTH, INTERVAL_DAY, INTERVAL_HOUR, INTERVAL_MINUTE, INTERVAL_SECOND, INTERVAL_YEAR_TO_MONTH, INTERVAL_DAY_TO_HOUR, INTERVAL_DAY_TO_MINUTE, INTERVAL_DAY_TO_SECOND, INTERVAL_HOUR_TO_MINUTE, INTERVAL_HOUR_TO_SECOND, INTERVAL_MINUTE_TO_SECOND -> ValueTinyint.get(Value.convertToByte(getLong(), column))
            NUMERIC, DECFLOAT -> ValueTinyint[Value.convertToByte(Value.convertToLong(getBigDecimal(), column), column)]
            REAL, DOUBLE -> ValueTinyint[Value.convertToByte(Value.convertToLong(getDouble(), column), column)]
            BINARY, VARBINARY -> {
                val bytes = getBytesNoCopy()!!
                if (bytes.size == 1) ValueTinyint[bytes[0]]
                throw getDataConversionError(TINYINT)
            }
            NULL -> throw DbException.getInternalError()
            else -> throw getDataConversionError(TINYINT)
        }
    }

    override fun add(v: Value): Value {
        val other = v as ValueTinyint
        return ValueTinyint.checkRange(value + other.value)
    }

    override fun getSignum(): Int = value.toInt().sign

    override fun negate(): Value = checkRange(-value.toInt())

    override fun subtract(v: Value): Value {
        val other = v as ValueTinyint
        return checkRange(value - other.value)
    }

    override fun multiply(v: Value): Value {
        val other = v as ValueTinyint
        return checkRange(value * other.value)
    }

    override fun divide(v: Value, quotientType: TypeInfo?): Value {
        val other = v as ValueTinyint
        if (other.value.toInt() == 0) {
            throw DbException.get(ErrorCode.DIVISION_BY_ZERO_1, getTraceSQL()!!)
        }
        return checkRange(value / other.value)
    }

    override fun modulus(v: Value): Value {
        val other = v as ValueTinyint
        if (other.value.toInt() == 0) {
            throw DbException.get(ErrorCode.DIVISION_BY_ZERO_1, getTraceSQL()!!)
        }
        return ValueTinyint[(value % other.value).toByte()]
    }

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder {
        return if (sqlFlags and HasSQL.NO_CASTS == 0) {
            builder.append("CAST(").append(value.toInt()).append(" AS TINYINT)")
        } else builder.append(value.toInt())
    }

    override val type: TypeInfo = TypeInfo.TYPE_TINYINT

    override fun getValueType(): Int = TINYINT

    override fun getBytes(): ByteArray = byteArrayOf(value)

    override fun getByte(): Byte = value

    override fun getShort(): Short = value.toShort()

    override fun getInt(): Int = value.toInt()

    override fun getLong(): Long = value.toLong()

    override fun getBigDecimal(): BigDecimal = BigDecimal.valueOf(value.toLong())

    override fun getFloat(): Float = value.toFloat()

    override fun getDouble(): Double = value.toDouble()

    override fun compareTypeSafe(o: Value, mode: CompareMode?, provider: CastDataProvider?): Int {
        return Integer.compare(value.toInt(), (o as ValueTinyint).value.toInt())
    }

    override fun getString(): String? = value.toInt().toString()

    override fun hashCode(): Int = value.toInt()

    override fun equals(other: Any?): Boolean = other is ValueTinyint && value == other.value
}