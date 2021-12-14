package org.h2.value

import org.h2.api.ErrorCode
import org.h2.engine.CastDataProvider
import org.h2.message.DbException
import org.h2.util.HasSQL
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Implementation of the DECIMAL data type
 */
class ValueDecfloat(value: BigDecimal?) : ValueBigDecimalBase(value) {

    private lateinit var _type: TypeInfo

    companion object {
        /**
         * The value 'zero'.
         */
        val ZERO = ValueDecfloat(BigDecimal.ZERO)

        /**
         * The value 'one'.
         */
        val ONE = ValueDecfloat(BigDecimal.ONE)

        /**
         * The positive infinity value.
         */
        val POSITIVE_INFINITY = ValueDecfloat(null)

        /**
         * The negative infinity value.
         */
        val NEGATIVE_INFINITY = ValueDecfloat(null)

        /**
         * The not a number value.
         */
        val NAN = ValueDecfloat(null)

        /**
         * Get or create a DECFLOAT value for the given big decimal.
         *
         * @param dec the big decimal
         * @return the value
         */
        operator fun get(dec: BigDecimal): ValueDecfloat {
            val re = dec.stripTrailingZeros()
            if (BigDecimal.ZERO == re) return ZERO
            if (BigDecimal.ONE == re) return ONE
            return cache(ValueDecfloat(re)) as ValueDecfloat
        }

        /**
         * Divides to [BigDecimal] values and returns a `DECFLOAT`
         * result of the specified data type.
         *
         * @param dividend the dividend
         * @param divisor the divisor
         * @param quotientType the type of quotient
         * @return the quotient
         */
        fun divide(dividend: BigDecimal, divisor: BigDecimal, quotientType: TypeInfo): ValueDecfloat {
            val quotientPrecision: Int = quotientType.precision.toInt()
            var quotient = dividend.divide(divisor, dividend.scale() - dividend.precision() + divisor.precision() - divisor.scale() + quotientPrecision, RoundingMode.HALF_DOWN)

            val precision = quotient.precision()
            if (precision > quotientPrecision) {
                quotient = quotient.setScale(quotient.scale() - precision + quotientPrecision, RoundingMode.HALF_UP)
            }
            return ValueDecfloat[quotient]
        }

        fun Value.convertToDecfloat(targetType: TypeInfo, conversionMode: Int): ValueDecfloat {
            var v: ValueDecfloat
            when (getValueType()) {
                DECFLOAT -> {
                    v = this as ValueDecfloat
                    if (v.value == null) return v
                }
                CHAR, VARCHAR, VARCHAR_IGNORECASE -> {
                    val s = getString()!!.trim { it <= ' ' }
                    v = try {
                        ValueDecfloat.get(BigDecimal(s))
                    } catch (e: NumberFormatException) {
                        return when (s) {
                            "-Infinity" -> ValueDecfloat.NEGATIVE_INFINITY
                            "Infinity", "+Infinity" -> ValueDecfloat.POSITIVE_INFINITY
                            "NaN", "-NaN", "+NaN" -> ValueDecfloat.NAN
                            else -> throw getDataConversionError(DECFLOAT)
                        }
                    }
                }
                BOOLEAN -> v = if (getBoolean()) ValueDecfloat.ONE else ValueDecfloat.ZERO
                REAL -> {
                    val value = getFloat()
                    if (!java.lang.Float.isFinite(value))
                        return when (value) {
                            Float.POSITIVE_INFINITY -> ValueDecfloat.POSITIVE_INFINITY
                            Float.NEGATIVE_INFINITY -> ValueDecfloat.NEGATIVE_INFINITY
                            else -> ValueDecfloat.NAN
                        }
                    v = ValueDecfloat.get(BigDecimal(value.toString()))
                }
                DOUBLE -> {
                    val value = getDouble()
                    if (!java.lang.Double.isFinite(value))
                        return when (value) {
                            Double.POSITIVE_INFINITY -> ValueDecfloat.POSITIVE_INFINITY
                            Double.NEGATIVE_INFINITY -> ValueDecfloat.NEGATIVE_INFINITY
                            else -> ValueDecfloat.NAN
                        }
                    v = ValueDecfloat.get(BigDecimal(value.toString()))
                }
                NULL -> throw DbException.getInternalError()
                else -> v = try {
                    ValueDecfloat[getBigDecimal()]
                } catch (e: DbException) {
                    throw if (e.getErrorCode() == ErrorCode.DATA_CONVERSION_ERROR_1) getDataConversionError(DECFLOAT) else e
                }
            }

            if (conversionMode == CONVERT_TO) return v

            val bd = v.value!!
            val precision = bd.precision()
            val targetPrecision: Int = targetType.precision.toInt()

            if (precision > targetPrecision) {
                v = ValueDecfloat[bd.setScale(bd.scale() - precision + targetPrecision, RoundingMode.HALF_UP)]
            }
            return v
        }
    }

    override fun getString(): String = value?.toString() ?: when {
        this === POSITIVE_INFINITY -> "Infinity"
        this === NEGATIVE_INFINITY -> "-Infinity"
        else -> "NaN"
    }

    override fun getSQL(builder: StringBuilder, sqlFlags: Int) = if (sqlFlags and HasSQL.NO_CASTS == 0) {
        getSQL(builder.append("CAST(")).append(" AS DECFLOAT)")
    } else getSQL(builder)


    override fun getSQL(builder: StringBuilder): StringBuilder = when {
        value != null -> builder.append(value)
        this === POSITIVE_INFINITY -> builder.append("'Infinity'")
        this === NEGATIVE_INFINITY -> builder.append("'-Infinity'")
        else -> builder.append("'NaN'")
    }

    override fun getValueType(): Int = DECFLOAT

    override fun add(v: Value?): Value {
        val value2 = (v as ValueDecfloat).value
        if (value != null) {
            return if (value2 != null) ValueDecfloat[value.add(value2)] else v
        }

        if (value2 != null || this === v) {
            return this
        }
        return NAN
    }

    override fun subtract(v: Value?): Value {
        val value2 = (v as ValueDecfloat).value
        if (value != null) {
            if (value2 != null) {
                return ValueDecfloat[value.subtract(value2)]
            }
            return if (v === POSITIVE_INFINITY) NEGATIVE_INFINITY else if (v === NEGATIVE_INFINITY) POSITIVE_INFINITY else NAN
        } else if (value2 != null) {
            return this
        } else if (this === POSITIVE_INFINITY) {
            if (v === NEGATIVE_INFINITY) {
                return POSITIVE_INFINITY
            }
        } else if (this === NEGATIVE_INFINITY && v === POSITIVE_INFINITY) {
            return NEGATIVE_INFINITY
        }
        return NAN
    }

    override fun negate(): Value {
        if (value != null) return ValueDecfloat[value.negate()]

        return when {
            this === POSITIVE_INFINITY -> NEGATIVE_INFINITY
            this === NEGATIVE_INFINITY -> POSITIVE_INFINITY
            else -> NAN
        }
    }

    override fun multiply(v: Value?): Value {
        val value2 = (v as ValueDecfloat).value

        if (value != null) {
            if (value2 != null) {
                return ValueDecfloat[value.multiply(value2)]
            }
            val s = value.signum()
            if (v === POSITIVE_INFINITY) {
                if (s > 0) {
                    return POSITIVE_INFINITY
                } else if (s < 0) {
                    return NEGATIVE_INFINITY
                }
            } else if (v === NEGATIVE_INFINITY) {
                if (s > 0) {
                    return NEGATIVE_INFINITY
                } else if (s < 0) {
                    return POSITIVE_INFINITY
                }
            }
        } else if (value2 != null) {
            val s = value2.signum()
            if (this === POSITIVE_INFINITY) {
                if (s > 0) {
                    return POSITIVE_INFINITY
                } else if (s < 0) {
                    return NEGATIVE_INFINITY
                }
            } else if (this === NEGATIVE_INFINITY) {
                if (s > 0) {
                    return NEGATIVE_INFINITY
                } else if (s < 0) {
                    return POSITIVE_INFINITY
                }
            }
        } else if (this === POSITIVE_INFINITY) {
            if (v === POSITIVE_INFINITY) {
                return POSITIVE_INFINITY
            } else if (v === NEGATIVE_INFINITY) {
                return NEGATIVE_INFINITY
            }
        } else if (this === NEGATIVE_INFINITY) {
            if (v === POSITIVE_INFINITY) {
                return NEGATIVE_INFINITY
            } else if (v === NEGATIVE_INFINITY) {
                return POSITIVE_INFINITY
            }
        }
        return NAN
    }

    override fun divide(v: Value?, quotientType: TypeInfo?): Value {
        val value2 = (v as ValueDecfloat).value
        if (value2 != null && value2.signum() == 0) {
            throw DbException.get(ErrorCode.DIVISION_BY_ZERO_1, getTraceSQL()!!)
        }

        if (value != null) {
            if (value2 != null) return divide(value, value2, quotientType!!)
            if (v !== NAN) return ZERO
        }

        if (value2 != null && this !== NAN) {
            return if (this === POSITIVE_INFINITY == value2.signum() > 0) POSITIVE_INFINITY else NEGATIVE_INFINITY
        }
        return NAN
    }

    override fun modulus(v: Value?): Value {
        val value2 = (v as ValueDecfloat).value
        if (value2 != null && value2.signum() == 0) {
            throw DbException.get(ErrorCode.DIVISION_BY_ZERO_1, getTraceSQL()!!)
        }

        return when {
            value == null -> NAN
            value2 != null -> ValueDecfloat[value.remainder(value2)]
            v !== NAN -> this
            else -> NAN
        }
    }

    fun compareTypeSafe(o: Value, mode: CompareMode?, provider: CastDataProvider?): Int {
        val value2 = (o as ValueDecfloat).value

        if (value2 != null && value != null) return value.compareTo(value2)
        if (value2 == null && value != null) return if (o == NEGATIVE_INFINITY) 1 else -1
        if (value2 != null) return if (this == NEGATIVE_INFINITY) -1 else 1
        if (this == o) return 0
        if (this == NEGATIVE_INFINITY) return -1
        if (o == NEGATIVE_INFINITY) return 1
        return if (this === POSITIVE_INFINITY) -1 else 1
    }

    override fun getSignum(): Int = value?.signum() ?: (when {
        this === POSITIVE_INFINITY -> 1
        this === NEGATIVE_INFINITY -> -1
        else -> 0
    })

    override fun getBigDecimal(): BigDecimal = value ?: throw getDataConversionError(NUMERIC)

    override fun getFloat(): Float = value?.toFloat() ?: when {
        this === POSITIVE_INFINITY -> Float.POSITIVE_INFINITY
        this === NEGATIVE_INFINITY -> Float.NEGATIVE_INFINITY
        else -> Float.NaN
    }

    override fun getDouble(): Double = value?.toDouble() ?: when {
        this === POSITIVE_INFINITY -> Double.POSITIVE_INFINITY
        this === NEGATIVE_INFINITY -> Double.NEGATIVE_INFINITY
        else -> Double.NaN
    }

    override fun hashCode(): Int =
            if (value != null) javaClass.hashCode() * 31 + value.hashCode()
            else System.identityHashCode(this)

    override fun equals(other: Any?): Boolean {
        // Two BigDecimal objects are considered equal only if they are equal in
        // value and scale (thus 2.0 is not equal to 2.00 when using equals;
        // however -0.0 and 0.0 are). Can not use compareTo because 2.0 and 2.00
        // have different hash codes
        return other is ValueDecfloat && value == other.value
    }

    override fun getMemory(): Int = if (value != null) value.precision() + 120 else 32

    /**
     * Returns `true`, if this value is finite.
     *
     * @return `true`, if this value is finite, `false` otherwise
     */
    fun isFinite(): Boolean = value != null

}