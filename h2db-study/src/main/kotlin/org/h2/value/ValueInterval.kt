package org.h2.value

import org.h2.api.IntervalQualifier
import org.h2.message.DbException
import org.h2.util.IntervalUtils

/**
 * Implementation of the INTERVAL data type.
 */
class ValueInterval(
    private val valueType: Int = 0,
    val negative: Boolean = false,
    val leading: Long = 0,
    val remaining: Long = 0,
) : Value() {
    companion object {
        /**
         * The default leading field precision for intervals.
         */
        const val DEFAULT_PRECISION = 2

        /**
         * The maximum leading field precision for intervals.
         */
        const val MAXIMUM_PRECISION = 18

        /**
         * The default scale for intervals with seconds.
         */
        const val DEFAULT_SCALE = 6

        /**
         * The maximum scale for intervals with seconds.
         */
        const val MAXIMUM_SCALE = 9

        /**
         * Create a ValueInterval instance.
         *
         * @param qualifier  qualifier
         * @param negative whether interval is negative
         * @param leading value of leading field
         * @param remaining values of all remaining fields
         * @return interval value
         */
        fun from(qualifier: IntervalQualifier, negative: Boolean, leading: Long, remaining: Long): ValueInterval? {
            var negative = IntervalUtils.validateInterval(qualifier, negative, leading, remaining)
            return cache(
                ValueInterval(
                    qualifier.ordinal + INTERVAL_YEAR,
                    negative,
                    leading,
                    remaining
                )
            ) as ValueInterval
        }

        /**
         * Returns display size for the specified qualifier, precision and fractional seconds precision.
         * @param type the value type
         * @param precision leading field precision
         * @param scale fractional seconds precision. Ignored if specified type of
         *              interval does not have seconds.
         * @return display size
         */
        fun getDisplaySize(type: Int, precision: Int, scale: Int): Int {
            return when (type) {
                INTERVAL_YEAR, INTERVAL_HOUR -> 17 + precision
                INTERVAL_MONTH -> 18 + precision
                INTERVAL_DAY -> 16 + precision
                INTERVAL_MINUTE -> 19 + precision
                INTERVAL_SECOND -> if (scale > 0) 20 + precision + scale else 19 + precision
                INTERVAL_YEAR_TO_MONTH -> 29 + precision
                INTERVAL_DAY_TO_HOUR -> 27 + precision
                INTERVAL_DAY_TO_MINUTE -> 32 + precision
                INTERVAL_DAY_TO_SECOND -> if (scale > 0) 36 + precision + scale else 35 + precision
                INTERVAL_HOUR_TO_MINUTE -> 30 + precision
                INTERVAL_HOUR_TO_SECOND -> if (scale > 0) 34 + precision + scale else 33 + precision
                INTERVAL_MINUTE_TO_SECOND -> if (scale > 0) 33 + precision + scale else 32 + precision
                else -> throw DbException.getUnsupportedException(type.toString())
            }
        }
    }

    private var type: TypeInfo? = null
    override fun getType(): TypeInfo {
        var type = type
        if (type == null) {
            var l = leading
            var precision = 0
            while (l > 0) {
                precision++
                l /= 10
            }
            if (precision == 0) {
                precision = 1
            }
            type = TypeInfo(
                valueType,
                precision.toLong(), 0,
                getDisplaySize(valueType, MAXIMUM_PRECISION, MAXIMUM_SCALE), null
            )
            this.type = type
        }
        return type
    }

    override fun getValueType(): Int {
        TODO("Not yet implemented")
    }

}