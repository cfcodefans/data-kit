package org.h2.value

import org.h2.api.ErrorCode
import org.h2.message.DbException
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * Implementation of the DECIMAL data type
 */
class ValueDecimal : Value() {
    private lateinit var value: BigDecimal
    private lateinit var _type: TypeInfo

    private constructor(_value: BigDecimal?) {
        requireNotNull(_value) { "null" }
        if (_value.javaClass != BigDecimal::class.java) {
            throw DbException.get(ErrorCode.INVALID_CLASS_2,
                    BigDecimal::class.java.name, _value.javaClass.name)
        }
        value = _value
    }


    companion object {
        val ZERO = ValueDecimal(BigDecimal.ZERO)
        val ONE = ValueDecimal(BigDecimal.ONE)

        /**
         * The default precision for a decimal value.
         */
        const val DEFAULT_PRECISION: Int = 65535

        /**
         * The default scale for a decimal value.
         */
        const val DEFAULT_SCALE: Int = 32767

        /**
         * The default display size for a decimal value.
         */
        const val DEFAULT_DISPLAY_SIZE: Int = 65535

        /**
         * Get or create big decimal value for the given big decimal.
         *
         * @param dec the big decimal
         * @return the value
         */
        operator fun get(dec: BigDecimal): ValueDecimal? = when {
            BigDecimal.ZERO == dec -> ZERO
            BigDecimal.ONE == dec -> ONE
            else -> cache(ValueDecimal(dec)) as ValueDecimal
        }

        /**
         * Get or create big decimal value for the given big integer.
         *
         * @param bigInteger the big integer
         * @return the value
         */
        operator fun get(bigInteger: BigInteger): ValueDecimal? = when {
            BigInteger.ZERO == bigInteger -> ZERO
            BigInteger.ONE == bigInteger -> ONE
            else -> cache(ValueDecimal(bigInteger.toBigDecimal())) as ValueDecimal
        }

        /**
         * The maximum scale of a BigDecimal value.
         */
        private const val BIG_DECIMAL_SCALE_MAX: Int = 100000

        /**
         * Set the scale of a BigDecimal value.
         *
         * @param bd the BigDecimal value
         * @param scale the new scale
         * @return the scaled value
         */
        fun setScale(bd: BigDecimal, scale: Int): BigDecimal? {
            if (scale > BIG_DECIMAL_SCALE_MAX || scale < -BIG_DECIMAL_SCALE_MAX) {
                throw DbException.getInvalidValueException("scale", scale)
            }
            return bd.setScale(scale, RoundingMode.HALF_UP)
        }

        const val DIVIDE_SCALE_ADD: Int = 25
    }

    override fun equals(other: Any?): Boolean {
        // Two BigDecimal objects are considered equal only if they are equal in
        // value and scale (thus 2.0 is not equal to 2.00 when using equals;
        // however -0.0 and 0.0 are). Can not use compareTo because 2.0 and 2.00
        // have different hash codes
        return other is ValueDecimal && value == other.value
    }

    override fun getMemory(): Int = value.precision() + 120

    override fun add(v: Value?): Value? = ValueDecimal[value.add((v as ValueDecimal).value)]
    override fun subtract(v: Value?): Value? = ValueDecimal[value.subtract((v as ValueDecimal).value)]
}