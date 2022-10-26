package org.h2.value

import org.h2.api.ErrorCode
import org.h2.engine.CastDataProvider
import org.h2.message.DbException
import org.h2.util.HasSQL
import java.math.BigDecimal

/**
 * Implementation of the SMALLINT data type.
 */
class ValueSmallint(private val value: Short) : Value() {
    companion object {
        /**
         * The precision in bits.
         */
        const val PRECISION = 16

        /**
         * The approximate precision in decimal digits.
         */
        const val DECIMAL_PRECISION = 5

        /**
         * The maximum display size of a SMALLINT.
         * Example: -32768
         */
        const val DISPLAY_SIZE = 6

        /**
         * Get or create a SMALLINT value for the given short.
         *
         * @param i the short
         * @return the value
         */
        operator fun get(i: Short): ValueSmallint = cache(ValueSmallint(i)) as ValueSmallint

        private fun checkRange(x: Int): ValueSmallint {
            if (x.toShort().toInt() != x) {
                throw DbException.get(ErrorCode.NUMERIC_VALUE_OUT_OF_RANGE_1, x.toString())
            }
            return ValueSmallint[x.toShort()]
        }

        /**
         * Converts this value to a SMALLINT value. May not be called on a NULL value.
         *
         * @param column the column, used for to improve the error message if conversion fails
         * @return the SMALLINT value
         */
        fun Value.convertToSmallint(column: Any?): ValueSmallint {
            return when (getValueType()) {
                SMALLINT -> this as ValueSmallint
                CHAR, VARCHAR, VARCHAR_IGNORECASE, BOOLEAN, TINYINT -> get(getShort())
                ENUM, INTEGER -> get(convertToShort(getInt().toLong(), column!!))
                BIGINT, INTERVAL_YEAR, INTERVAL_MONTH, INTERVAL_DAY, INTERVAL_HOUR, INTERVAL_MINUTE, INTERVAL_SECOND, INTERVAL_YEAR_TO_MONTH, INTERVAL_DAY_TO_HOUR, INTERVAL_DAY_TO_MINUTE, INTERVAL_DAY_TO_SECOND, INTERVAL_HOUR_TO_MINUTE, INTERVAL_HOUR_TO_SECOND, INTERVAL_MINUTE_TO_SECOND -> ValueSmallint.get(
                    convertToShort(getLong(), column!!))

                NUMERIC, DECFLOAT -> get(convertToShort(convertToLong(getBigDecimal(), column), column!!))
                REAL, DOUBLE -> get(convertToShort(convertToLong(getDouble(), column), column!!))
                BINARY, VARBINARY -> {
                    val bytes = getBytesNoCopy()
                    if (bytes.size == 2) {
                        return get(((bytes[0].toInt() shl 8) + (bytes[1].toInt() and 0xff)).toShort())
                    }
                    throw getDataConversionError(SMALLINT)
                }

                NULL -> throw DbException.getInternalError()
                else -> throw getDataConversionError(SMALLINT)
            }
        }
    }

    override var type: TypeInfo? = TypeInfo.TYPE_SMALLINT


    override fun equals(other: Any?): Boolean = other is ValueSmallint && value == other.value

    override fun getString(): String = value.toInt().toString()

    override fun compareTypeSafe(o: Value, mode: CompareMode?, provider: CastDataProvider?): Int {
        return value.toInt().compareTo((o as ValueSmallint).value.toInt())
    }

    override fun getShort(): Short = value

    override fun getInt(): Int = value.toInt()

    override fun getLong(): Long = value.toLong()

    override fun getBigDecimal(): BigDecimal = BigDecimal.valueOf(value.toLong())

    override fun getFloat(): Float = value.toFloat()

    override fun getDouble(): Double = value.toDouble()

    override fun getBytes(): ByteArray {
        val value = value
        return byteArrayOf((value.toInt() shr 8).toByte(), value.toByte())
    }

    override fun getValueType(): Int = SMALLINT

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder {
        return if (sqlFlags and HasSQL.NO_CASTS == 0)
            builder.append("CAST(").append(value.toInt()).append(" AS SMALLINT)")
        else
            builder.append(value.toInt())
    }

    override fun modulus(v: Value): Value {
        val other = v as ValueSmallint
        if (other.value.toInt() == 0) {
            throw DbException.get(ErrorCode.DIVISION_BY_ZERO_1, getTraceSQL())
        }
        return ValueSmallint[(value % other.value).toShort()]
    }

    override fun divide(v: Value, quotientType: TypeInfo?): Value {
        val other = v as ValueSmallint
        if (other.value.toInt() == 0) {
            throw DbException.get(ErrorCode.DIVISION_BY_ZERO_1, getTraceSQL())
        }
        return checkRange(value / other.value)
    }

    override fun getSignum(): Int {
        return Integer.signum(value.toInt())
    }

    override fun negate(): Value {
        return checkRange(-value.toInt())
    }

    override fun subtract(v: Value): Value {
        val other = v as ValueSmallint
        return checkRange(value - other.value)
    }

    override fun multiply(v: Value): Value {
        val other = v as ValueSmallint
        return checkRange(value * other.value)
    }

    override fun add(v: Value): Value {
        val other = v as ValueSmallint
        return ValueSmallint.checkRange(value + other.value)
    }

    override fun hashCode(): Int = value.toInt()
}