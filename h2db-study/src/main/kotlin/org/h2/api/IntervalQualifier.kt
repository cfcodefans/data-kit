package org.h2.api

/**
 * Interval qualifier
 */
enum class IntervalQualifier {
    /**
     * {@code YEAR}
     */
    YEAR,

    /**
     * {@code MONTH}
     */
    MONTH,

    /**
     * {@code DAY}
     */
    DAY,

    /**
     * {@code HOUR}
     */
    HOUR,

    /**
     * {@code MINUTE}
     */
    MINUTE,

    /**
     * {@code SECOND}
     */
    SECOND,

    /**
     * {@code YEAR TO MONTH}
     */
    YEAR_TO_MONTH,

    /**
     * {@code DAY TO HOUR}
     */
    DAY_TO_HOUR,

    /**
     * {@code DAY TO MINUTE}
     */
    DAY_TO_MINUTE,

    /**
     * {@code DAY TO SECOND}
     */
    DAY_TO_SECOND,

    /**
     * {@code HOUR TO MINUTE}
     */
    HOUR_TO_MINUTE,

    /**
     * {@code HOUR TO SECOND}
     */
    HOUR_TO_SECOND,

    /**
     * {@code MINUTE TO SECOND}
     */
    MINUTE_TO_SECOND;

    private var string: String = name.replace('_', ' ').intern()

    override fun toString(): String = string

    companion object {
        /**
         * Returns the interval qualifier with the specified ordinal value.
         * @param ordinal Java ordinal value (0-based)
         * @return interval qualifier with the specified ordinal value
         */
        @JvmStatic
        fun valueOf(ordinal: Int): IntervalQualifier = when (ordinal) {
            0 -> YEAR
            1 -> MONTH
            2 -> DAY
            3 -> HOUR
            4 -> MINUTE
            5 -> SECOND
            6 -> YEAR_TO_MONTH
            7 -> DAY_TO_HOUR
            8 -> DAY_TO_MINUTE
            9 -> DAY_TO_SECOND
            10 -> HOUR_TO_MINUTE
            11 -> HOUR_TO_SECOND
            12 -> HOUR_TO_SECOND
            else -> throw  IllegalArgumentException()
        }
    }

    /**
     * Returns whether interval with this qualifier is a year-month interval.
     *
     * @return whether interval with this qualifier is a year-month interval
     */
    fun isYearMonth(): Boolean = this == YEAR || this == MONTH || this == YEAR_TO_MONTH

    /**
     * Returns whether interval with this qualifier is a day-time interval.
     *
     * @return whether interval with this qualifier is a day-time interval
     */
    fun isDayTime(): Boolean = !isYearMonth()

    /**
     * Returns whether interval with this qualifier has years.
     *
     * @return whether interval with this qualifier has years
     */
    open fun hasYears(): Boolean = this == YEAR || this == YEAR_TO_MONTH

    /**
     * Returns whether interval with this qualifier has months.
     *
     * @return whether interval with this qualifier has months
     */
    open fun hasMonths(): Boolean = this == MONTH || this == YEAR_TO_MONTH

    /**
     * Returns whether interval with this qualifier has days.
     *
     * @return whether interval with this qualifier has days
     */
    open fun hasDays(): Boolean = when (this) {
        DAY, DAY_TO_HOUR, DAY_TO_MINUTE, DAY_TO_SECOND -> true
        else -> false
    }

    /**
     * Returns whether interval with this qualifier has hours.
     *
     * @return whether interval with this qualifier has hours
     */
    open fun hasHours(): Boolean = when (this) {
        HOUR, DAY_TO_HOUR, DAY_TO_MINUTE, DAY_TO_SECOND, HOUR_TO_MINUTE, HOUR_TO_SECOND -> true
        else -> false
    }

    /**
     * Returns whether interval with this qualifier has minutes.
     *
     * @return whether interval with this qualifier has minutes
     */
    open fun hasMinutes(): Boolean = when (this) {
        MINUTE, DAY_TO_MINUTE, DAY_TO_SECOND, HOUR_TO_MINUTE, HOUR_TO_SECOND, MINUTE_TO_SECOND -> true
        else -> false
    }

    /**
     * Returns whether interval with this qualifier has seconds.
     *
     * @return whether interval with this qualifier has seconds
     */
    open fun hasSeconds(): Boolean = when (this) {
        SECOND, DAY_TO_SECOND, HOUR_TO_SECOND, MINUTE_TO_SECOND -> true
        else -> false
    }

    /**
     * Returns whether interval with this qualifier has multiple fields.
     *
     * @return whether interval with this qualifier has multiple fields
     */
    open fun hasMultipleFields(): Boolean = ordinal > 5

    /**
     * Appends full type name to the specified string builder.
     *
     * @param builder string builder
     * @param precision precision, or {@code -1}
     * @param scale fractional seconds precision, or {@code -1}
     * @param qualifierOnly if {@code true}, don't add the INTERVAL prefix
     * @return the specified string builder
     */
    fun getTypeName(builder: StringBuilder, precision: Int, scale: Int, qualifierOnly: Boolean): StringBuilder {
        if (!qualifierOnly) builder.append("INTERVAL ")

        when (this) {
            YEAR, MONTH, DAY, HOUR, MINUTE -> {
                builder.append(string)
                if (precision > 0) builder.append('(').append(precision).append(')')
            }
            SECOND -> {
                builder.append(string)
                if (precision > 0 || scale >= 0) {
                    builder.append('(').append(if (precision > 0) precision else 2)
                    if (scale >= 0) builder.append(", ").append(scale)
                    builder.append(')')
                }
            }
            YEAR_TO_MONTH -> {
                builder.append("YEAR")
                if (precision > 0) builder.append('(').append(precision).append(')')
                builder.append(" TO MONTH")
            }
            DAY_TO_HOUR -> {
                builder.append("DAY")
                if (precision > 0) builder.append('(').append(precision).append(')')
                builder.append(" TO HOUR")
            }
            DAY_TO_MINUTE -> {
                builder.append("DAY")
                if (precision > 0) builder.append('(').append(precision).append(')')
                builder.append(" TO MINUTE")
            }
            DAY_TO_SECOND -> {
                builder.append("DAY")
                if (precision > 0) builder.append('(').append(precision).append(')')
                builder.append(" TO SECOND")
                if (scale >= 0) builder.append('(').append(scale).append(')')
            }
            HOUR_TO_MINUTE -> {
                builder.append("HOUR")
                if (precision > 0) builder.append('(').append(precision).append(')')
                builder.append(" TO MINUTE")
            }
            HOUR_TO_SECOND -> {
                builder.append("HOUR")
                if (precision > 0) builder.append('(').append(precision).append(')')
                builder.append(" TO SECOND")
                if (scale >= 0) builder.append('(').append(scale).append(')')
            }
            MINUTE_TO_SECOND -> {
                builder.append("MINUTE")
                if (precision > 0) builder.append('(').append(precision).append(')')
                builder.append(" TO SECOND")
                if (scale >= 0) builder.append('(').append(scale).append(')')
            }
        }
        return builder
    }
}