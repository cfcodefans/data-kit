package org.h2.value

import org.h2.api.IntervalQualifier
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
            return cache(ValueInterval(qualifier.ordinal + INTERVAL_YEAR, negative, leading, remaining)) as ValueInterval
        }
    }

    private var type: TypeInfo? = null
    override fun getType(): TypeInfo {
        TODO("Not yet implemented")
    }

    override fun getValueType(): Int {
        TODO("Not yet implemented")
    }

}