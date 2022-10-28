package org.h2.value

import org.h2.api.ErrorCode
import org.h2.engine.CastDataProvider
import org.h2.message.DbException
import org.h2.util.Bits
import java.math.BigDecimal

/**
 * Implementation of the INTEGER data type.
 */
class ValueInteger(val value: Int) : Value() {
    companion object {
        /**
         * The precision in bits.
         */
        const val PRECISION = 32

        /**
         * The approximate precision in decimal digits.
         */
        const val DECIMAL_PRECISION = 10

        /**
         * The maximum display size of an INT.
         * Example: -2147483648
         */
        const val DISPLAY_SIZE = 11

        private const val STATIC_SIZE = 128

        // must be a power of 2
        private const val DYNAMIC_SIZE = 256
        private val STATIC_CACHE = (0..STATIC_SIZE).map { ValueInteger(it) }.toTypedArray()
        private val DYNAMIC_CACHE = arrayOfNulls<ValueInteger>(DYNAMIC_SIZE)

        /**
         * Get or create an INTEGER value for the given int.
         *
         * @param i the int
         * @return the value
         */
        operator fun get(i: Int): ValueInteger {
            if (i >= 0 && i < STATIC_SIZE) return STATIC_CACHE[i]

            var v = DYNAMIC_CACHE[i and DYNAMIC_SIZE - 1]

            if (v == null || v.value != i) {
                v = ValueInteger(i)
                DYNAMIC_CACHE[i and DYNAMIC_SIZE - 1] = v
            }
            return v
        }

        private fun checkRange(x: Long): ValueInteger {
            if (x.toInt().toLong() != x) {
                throw DbException.get(ErrorCode.NUMERIC_VALUE_OUT_OF_RANGE_1, x.toString())
            }
            return ValueInteger[x.toInt()]
        }
    }

    override fun add(v: Value): Value {
        val other = v as ValueInteger
        return checkRange(value.toLong() + other.value.toLong())
    }

    override fun getSignum(): Int = Integer.signum(value)

    override fun negate(): Value = checkRange(-value.toLong())

    override fun subtract(v: Value): Value {
        val other = v as ValueInteger
        return checkRange(value.toLong() - other.value.toLong())
    }

    override fun multiply(v: Value): Value {
        val other = v as ValueInteger
        return checkRange(value.toLong() * other.value.toLong())
    }

    override fun divide(v: Value, quotientType: TypeInfo?): Value {
        val y = (v as ValueInteger).value
        if (y == 0) throw DbException.get(ErrorCode.DIVISION_BY_ZERO_1, getTraceSQL())

        val x = value
        if (x == Int.MIN_VALUE && y == -1) throw DbException.get(ErrorCode.NUMERIC_VALUE_OUT_OF_RANGE_1, "2147483648")
        return ValueInteger[x / y]
    }

    override fun modulus(v: Value): Value {
        val other = v as ValueInteger
        if (other.value == 0) throw DbException.get(ErrorCode.DIVISION_BY_ZERO_1, getTraceSQL())
        return ValueInteger[value % other.value]
    }


    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder = builder.append(value)

    override var type: TypeInfo? = TypeInfo.TYPE_INTEGER

    override fun getValueType(): Int = INTEGER

    override fun getBytes(): ByteArray = ByteArray(4).also { Bits.writeInt(it, 0, getInt()) }

    override fun getInt(): Int = value

    override fun getLong(): Long = value.toLong()

    override fun getBigDecimal(): BigDecimal = BigDecimal.valueOf(value.toLong())

    override fun getFloat(): Float = value.toFloat()

    override fun getDouble(): Double = value.toDouble()

    override fun compareTypeSafe(o: Value, mode: CompareMode?, provider: CastDataProvider?): Int {
        return value.compareTo((o as ValueInteger).value)
    }

    override fun getString(): String = value.toString()

    override fun hashCode(): Int = value

    override fun equals(other: Any?): Boolean = other is ValueInteger && value == other.value
}