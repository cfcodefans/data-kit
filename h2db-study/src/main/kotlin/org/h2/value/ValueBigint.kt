package org.h2.value

import org.h2.api.ErrorCode
import org.h2.engine.CastDataProvider
import org.h2.message.DbException
import org.h2.util.Bits
import org.h2.util.HasSQL
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.abs

/**
 * Implementation of the BIGINT data type.
 */
class ValueBigint(val value: Long) : Value() {
    companion object {
        /**
         * Get or create a BIGINT value for the given long.
         *
         * @param i the long
         * @return the value
         */
        operator fun get(i: Long): ValueBigint =
                if (i in 0 until STATIC_SIZE)
                    STATIC_CACHE[i.toInt()]
                else cache(ValueBigint(i)) as ValueBigint

        /**
         * The smallest `ValueLong` value.
         */
        val MIN: ValueBigint = ValueBigint.get(Long.MIN_VALUE)

        /**
         * The largest `ValueLong` value.
         */
        val MAX: ValueBigint = ValueBigint.get(Long.MAX_VALUE)

        /**
         * The largest Long value, as a BigInteger.
         */
        val MAX_BI = BigInteger.valueOf(Long.MAX_VALUE)

        /**
         * The precision in bits.
         */
        const val PRECISION = 64

        /**
         * The approximate precision in decimal digits.
         */
        const val DECIMAL_PRECISION = 19

        /**
         * The maximum display size of a BIGINT.
         * Example: -9223372036854775808
         */
        const val DISPLAY_SIZE = 20

        private const val STATIC_SIZE = 100

        private val STATIC_CACHE: Array<ValueBigint> = (0 until STATIC_SIZE).map {
            ValueBigint(it.toLong())
        }.toTypedArray()

        /**
         * Converts this value to a BIGINT value. May not be called on a NULL value.
         *
         * @param column
         * the column, used for to improve the error message if
         * conversion fails
         * @return the BIGINT value
         */
        fun Value.convertToBigint(column: Any?): ValueBigint = when (getValueType()) {
            BIGINT -> this as ValueBigint
            CHAR, VARCHAR, VARCHAR_IGNORECASE, BOOLEAN, TINYINT, SMALLINT, INTEGER, INTERVAL_YEAR, INTERVAL_MONTH, INTERVAL_DAY, INTERVAL_HOUR, INTERVAL_MINUTE, INTERVAL_SECOND, INTERVAL_YEAR_TO_MONTH, INTERVAL_DAY_TO_HOUR, INTERVAL_DAY_TO_MINUTE, INTERVAL_DAY_TO_SECOND, INTERVAL_HOUR_TO_MINUTE, INTERVAL_HOUR_TO_SECOND, INTERVAL_MINUTE_TO_SECOND, ENUM -> ValueBigint.get(getLong())
            NUMERIC, DECFLOAT -> ValueBigint[convertToLong(getBigDecimal(), column)]
            REAL, DOUBLE -> ValueBigint[convertToLong(getDouble(), column)]
            BINARY, VARBINARY -> {
                val bytes = getBytesNoCopy()
                if (bytes!!.size == 8) ValueBigint[Bits.readLong(bytes, 0)]
                throw getDataConversionError(BIGINT)
            }
            NULL -> throw DbException.getInternalError()
            else -> throw getDataConversionError(BIGINT)
        }
    }

    private fun getOverflow(): DbException = DbException.get(ErrorCode.NUMERIC_VALUE_OUT_OF_RANGE_1, value.toString())

    override fun add(v: Value): Value {
        val x = value
        val y = (v as ValueBigint).value
        val result = x + y
        /*
         * If signs of both summands are different from the sign of the sum there is an
         * overflow.
         */if (x xor result and (y xor result) < 0) throw getOverflow()
        return ValueBigint[result]
    }

    override fun multiply(v: Value): Value {
        val x = value
        val y = (v as ValueBigint).value
        val result = x * y
        // Check whether numbers are large enough to overflow and second value != 0
        if (abs(x) or abs(y) ushr 31 != 0L
                && y != 0L
                && (result / y != x // Also check the special condition that is not handled above
                        || x == Long.MIN_VALUE && y == -1L)) {
            throw getOverflow()
        }
        return ValueBigint[result]
    }

    override fun divide(v: Value, quotientType: TypeInfo?): Value {
        val y = (v as ValueBigint).value
        if (y == 0L) {
            throw DbException.get(ErrorCode.DIVISION_BY_ZERO_1, getTraceSQL()!!)
        }
        val x = value
        if (x == Long.MIN_VALUE && y == -1L) {
            throw getOverflow()
        }
        return ValueBigint[x / y]
    }

    override fun modulus(v: Value): Value {
        val other = v as ValueBigint
        if (other.value == 0L) {
            throw DbException.get(ErrorCode.DIVISION_BY_ZERO_1, getTraceSQL()!!)
        }
        return ValueBigint[value % other.value]
    }

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder {
        return if (sqlFlags and HasSQL.NO_CASTS == 0 && value == value.toInt().toLong()) {
            builder.append("CAST(").append(value).append(" AS BIGINT)")
        } else builder.append(value)
    }

    override val type: TypeInfo
        get() = TypeInfo.TYPE_BIGINT

    override fun getValueType(): Int = BIGINT

    override fun getBytes(): ByteArray {
        val b = ByteArray(8)
        Bits.writeLong(b, 0, getLong())
        return b
    }

    override fun getLong(): Long = value

    override fun getBigDecimal(): BigDecimal = BigDecimal.valueOf(value)

    override fun getFloat(): Float = value.toFloat()

    override fun getDouble(): Double = value.toDouble()

    override fun compareTypeSafe(o: Value, mode: CompareMode?, provider: CastDataProvider?): Int =
            value.compareTo((o as ValueBigint).value)

    override fun getString(): String? = value.toString()

    override fun hashCode(): Int = (value xor (value shr 32)).toInt()

    override fun equals(other: Any?): Boolean = other is ValueBigint && value == other.value

}