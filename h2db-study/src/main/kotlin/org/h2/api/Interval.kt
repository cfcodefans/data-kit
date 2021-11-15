package org.h2.api

import org.h2.api.IntervalQualifier.*
import org.h2.message.DbException
import org.h2.util.DateTimeUtils
import org.h2.util.IntervalUtils
import java.lang.Math.abs
import java.util.*

/**
 * INTERVAL representation for result sets.
 */
/**
 * Creates a new interval. Do not use this constructor, use static methods instead.
 * @param qualifier qualifier
 * @param negative whether interval is negative
 * @param leading value of leading field
 * @param remaining combined value of all remaining fields
 */
class Interval(val qualifier: IntervalQualifier,
               var negative: Boolean = false,
               val leading: Long,
               val remaining: Long) {

    init {
        this.negative = try {
            IntervalUtils.validateInterval(qualifier, negative, leading, remaining)
        } catch (e: DbException) {
            throw IllegalArgumentException(e)
        }
    }

    companion object {
        /**
         * Creates a new INTERVAL YEAR.
         *
         * @param years
         * years, |years|&lt;10<sup>18</sup>
         * @return INTERVAL YEAR
         */
        fun ofYears(years: Long): Interval = Interval(qualifier = YEAR,
                negative = years < 0,
                leading = abs(years),
                remaining = 0)

        /**
         * Creates a new INTERVAL MONTH.
         *
         * @param months
         * months, |months|&lt;10<sup>18</sup>
         * @return INTERVAL MONTH
         */
        fun ofMonths(months: Long): Interval = Interval(MONTH, months < 0, abs(months), 0)

        /**
         * Creates a new INTERVAL DAY.
         *
         * @param days
         * days, |days|&lt;10<sup>18</sup>
         * @return INTERVAL DAY
         */
        fun ofDays(days: Long): Interval = Interval(DAY, days < 0, abs(days), 0)

        /**
         * Creates a new INTERVAL HOUR.
         *
         * @param hours
         * hours, |hours|&lt;10<sup>18</sup>
         * @return INTERVAL HOUR
         */
        fun ofHours(hours: Long): Interval = Interval(HOUR, hours < 0, abs(hours), 0)

        /**
         * Creates a new INTERVAL MINUTE.
         *
         * @param minutes
         * minutes, |minutes|&lt;10<sup>18</sup>
         * @return interval
         */
        fun ofMinutes(minutes: Long): Interval = Interval(MINUTE, minutes < 0, abs(minutes), 0)

        /**
         * Creates a new INTERVAL SECOND.
         *
         * @param seconds
         * seconds, |seconds|&lt;10<sup>18</sup>
         * @return INTERVAL SECOND
         */
        fun ofSeconds(seconds: Long): Interval = Interval(SECOND, seconds < 0, abs(seconds), 0)


        /**
         * Creates a new INTERVAL SECOND.
         *
         * If both arguments are not equal to zero they should have the same sign.
         *
         * @param seconds
         * seconds, |seconds|&lt;10<sup>18</sup>
         * @param nanos
         * nanoseconds, |nanos|&lt;1,000,000,000
         * @return INTERVAL SECOND
         */
        fun ofSeconds(seconds: Long, nanos: Int): Interval {
            // Interval is negative if any field is negative
            var seconds = seconds
            var nanos = nanos
            val negative = seconds or nanos.toLong() < 0
            if (negative) {
                // Ensure that all fields are negative or zero
                require(!(seconds > 0 || nanos > 0))
                // Make them positive
                seconds = -seconds
                nanos = -nanos
                // Long.MIN_VALUE and Integer.MIN_VALUE will be rejected by
                // constructor
            }
            return Interval(SECOND, negative, seconds, nanos.toLong())
        }

        /**
         * Creates a new INTERVAL SECOND.
         *
         * @param nanos
         * nanoseconds (including seconds)
         * @return INTERVAL SECOND
         */
        fun ofNanos(nanos: Long): Interval {
            var nanos = nanos
            val negative = nanos < 0
            if (negative) {
                nanos = -nanos
                if (nanos < 0) {
                    // Long.MIN_VALUE = -9_223_372_036_854_775_808L
                    return Interval(SECOND, true, 9223372036L, 854775808)
                }
            }
            return Interval(SECOND, negative, nanos / DateTimeUtils.NANOS_PER_SECOND, nanos % DateTimeUtils.NANOS_PER_SECOND)
        }

        /**
         * Creates a new INTERVAL YEAR TO MONTH.
         *
         * If both arguments are not equal to zero they should have the same sign.
         *
         * @param years
         * years, |years|&lt;10<sup>18</sup>
         * @param months
         * months, |months|&lt;12
         * @return INTERVAL YEAR TO MONTH
         */
        fun ofYearsMonths(years: Long, months: Int): Interval {
            // Interval is negative if any field is negative
            var years = years
            var months = months
            val negative = years or months.toLong() < 0
            if (negative) {
                // Ensure that all fields are negative or zero
                require(!(years > 0 || months > 0))
                // Make them positive
                years = -years
                months = -months
                // Long.MIN_VALUE and Integer.MIN_VALUE will be rejected by
                // constructor
            }
            return Interval(YEAR_TO_MONTH, negative, years, months.toLong())
        }

        /**
         * Creates a new INTERVAL DAY TO HOUR.
         *
         * If both arguments are not equal to zero they should have the same sign.
         *
         * @param days
         * days, |days|&lt;10<sup>18</sup>
         * @param hours
         * hours, |hours|&lt;24
         * @return INTERVAL DAY TO HOUR
         */
        fun ofDaysHours(days: Long, hours: Int): Interval {
            // Interval is negative if any field is negative
            var days = days
            var hours = hours
            val negative = days or hours.toLong() < 0
            if (negative) {
                // Ensure that all fields are negative or zero
                require(!(days > 0 || hours > 0))
                // Make them positive
                days = -days
                hours = -hours
                // Long.MIN_VALUE and Integer.MIN_VALUE will be rejected by
                // constructor
            }
            return Interval(DAY_TO_HOUR, negative, days, hours.toLong())
        }

        /**
         * Creates a new INTERVAL DAY TO MINUTE.
         * Non-zero arguments should have the same sign.
         *
         * @param days
         * days, |days|&lt;10<sup>18</sup>
         * @param hours
         * hours, |hours|&lt;24
         * @param minutes
         * minutes, |minutes|&lt;60
         * @return INTERVAL DAY TO MINUTE
         */
        fun ofDaysHoursMinutes(days: Long, hours: Int, minutes: Int): Interval? {
            // Interval is negative if any field is negative
            var days = days
            var hours = hours
            var minutes = minutes
            val negative = days or hours.toLong() or minutes.toLong() < 0
            if (negative) {
                // Ensure that all fields are negative or zero
                require(!(days > 0 || hours > 0 || minutes > 0))
                // Make them positive
                days = -days
                hours = -hours
                minutes = -minutes
                // Integer.MIN_VALUE
                require(hours or minutes >= 0)
                // days = Long.MIN_VALUE will be rejected by constructor
            }
            // Check only minutes.
            // Overflow in days or hours will be detected by constructor
            require(minutes < 60)
            return Interval(DAY_TO_MINUTE, negative, days, hours * 60L + minutes)
        }

        /**
         * Creates a new INTERVAL DAY TO SECOND.
         *
         *
         *
         * Non-zero arguments should have the same sign.
         *
         *
         * @param days
         * days, |days|&lt;10<sup>18</sup>
         * @param hours
         * hours, |hours|&lt;24
         * @param minutes
         * minutes, |minutes|&lt;60
         * @param seconds
         * seconds, |seconds|&lt;60
         * @return INTERVAL DAY TO SECOND
         */
        fun ofDaysHoursMinutesSeconds(days: Long, hours: Int, minutes: Int, seconds: Int): Interval? {
            return Interval.ofDaysHoursMinutesNanos(days, hours, minutes, seconds * DateTimeUtils.NANOS_PER_SECOND)
        }

        /**
         * Creates a new INTERVAL DAY TO SECOND.
         *
         * Non-zero arguments should have the same sign.
         *
         * @param days
         * days, |days|&lt;10<sup>18</sup>
         * @param hours
         * hours, |hours|&lt;24
         * @param minutes
         * minutes, |minutes|&lt;60
         * @param nanos
         * nanoseconds, |nanos|&lt;60,000,000,000
         * @return INTERVAL DAY TO SECOND
         */
        fun ofDaysHoursMinutesNanos(days: Long, hours: Int, minutes: Int, nanos: Long): Interval? {
            // Interval is negative if any field is negative
            var days = days
            var hours = hours
            var minutes = minutes
            var nanos = nanos
            val negative = days or hours.toLong() or minutes.toLong() or nanos < 0
            if (negative) {
                // Ensure that all fields are negative or zero
                require(!(days > 0 || hours > 0 || minutes > 0 || nanos > 0))
                // Make them positive
                days = -days
                hours = -hours
                minutes = -minutes
                nanos = -nanos
                // Integer.MIN_VALUE, Long.MIN_VALUE
                require(hours or minutes or nanos.toInt() >= 0)
                // days = Long.MIN_VALUE will be rejected by constructor
            }
            // Check only minutes and nanoseconds.
            // Overflow in days or hours will be detected by constructor
            require(!(minutes >= 60 || nanos >= DateTimeUtils.NANOS_PER_MINUTE))
            return Interval(DAY_TO_SECOND, negative, days,
                    (hours * 60L + minutes) * DateTimeUtils.NANOS_PER_MINUTE + nanos)
        }

        /**
         * Creates a new INTERVAL HOUR TO MINUTE.
         *
         * If both arguments are not equal to zero they should have the same sign.
         *
         * @param hours
         * hours, |hours|&lt;10<sup>18</sup>
         * @param minutes
         * minutes, |minutes|&lt;60
         * @return INTERVAL HOUR TO MINUTE
         */
        fun ofHoursMinutes(hours: Long, minutes: Int): Interval? {
            // Interval is negative if any field is negative
            var hours = hours
            var minutes = minutes
            val negative = hours or minutes.toLong() < 0
            if (negative) {
                // Ensure that all fields are negative or zero
                require(!(hours > 0 || minutes > 0))
                // Make them positive
                hours = -hours
                minutes = -minutes
                // Long.MIN_VALUE and Integer.MIN_VALUE will be rejected by
                // constructor
            }
            return Interval(HOUR_TO_MINUTE, negative, hours, minutes.toLong())
        }

        /**
         * Creates a new INTERVAL HOUR TO SECOND.
         *
         * Non-zero arguments should have the same sign.
         *
         * @param hours
         * hours, |hours|&lt;10<sup>18</sup>
         * @param minutes
         * minutes, |minutes|&lt;60
         * @param seconds
         * seconds, |seconds|&lt;60
         * @return INTERVAL HOUR TO SECOND
         */
        fun ofHoursMinutesSeconds(hours: Long, minutes: Int, seconds: Int): Interval? {
            return Interval.ofHoursMinutesNanos(hours, minutes, seconds * DateTimeUtils.NANOS_PER_SECOND)
        }

        /**
         * Creates a new INTERVAL HOUR TO SECOND.
         *
         * Non-zero arguments should have the same sign.
         *
         * @param hours
         * hours, |hours|&lt;10<sup>18</sup>
         * @param minutes
         * minutes, |minutes|&lt;60
         * @param nanos
         * nanoseconds, |seconds|&lt;60,000,000,000
         * @return INTERVAL HOUR TO SECOND
         */
        fun ofHoursMinutesNanos(hours: Long, minutes: Int, nanos: Long): Interval? {
            // Interval is negative if any field is negative
            var hours = hours
            var minutes = minutes
            var nanos = nanos
            val negative = hours or minutes.toLong() or nanos < 0
            if (negative) {
                // Ensure that all fields are negative or zero
                require(!(hours > 0 || minutes > 0 || nanos > 0))
                // Make them positive
                hours = -hours
                minutes = -minutes
                nanos = -nanos
                // Integer.MIN_VALUE, Long.MIN_VALUE
                require(minutes or nanos.toInt() >= 0)
                // hours = Long.MIN_VALUE will be rejected by constructor
            }
            // Check only nanoseconds.
            // Overflow in hours or minutes will be detected by constructor
            require(nanos < DateTimeUtils.NANOS_PER_MINUTE)
            return Interval(HOUR_TO_SECOND, negative, hours, minutes * DateTimeUtils.NANOS_PER_MINUTE + nanos)
        }

        /**
         * Creates a new INTERVAL MINUTE TO SECOND.
         *
         * If both arguments are not equal to zero they should have the same sign.
         *
         * @param minutes
         * minutes, |minutes|&lt;10<sup>18</sup>
         * @param seconds
         * seconds, |seconds|&lt;60
         * @return INTERVAL MINUTE TO SECOND
         */
        fun ofMinutesSeconds(minutes: Long, seconds: Int): Interval? {
            return ofMinutesNanos(minutes, seconds * DateTimeUtils.NANOS_PER_SECOND)
        }

        /**
         * Creates a new INTERVAL MINUTE TO SECOND.
         *
         * If both arguments are not equal to zero they should have the same sign.
         *
         * @param minutes
         * minutes, |minutes|&lt;10<sup>18</sup>
         * @param nanos
         * nanoseconds, |nanos|&lt;60,000,000,000
         * @return INTERVAL MINUTE TO SECOND
         */
        fun ofMinutesNanos(minutes: Long, nanos: Long): Interval? {
            // Interval is negative if any field is negative
            var minutes = minutes
            var nanos = nanos
            val negative = minutes or nanos < 0
            if (negative) {
                // Ensure that all fields are negative or zero
                require(!(minutes > 0 || nanos > 0))
                // Make them positive
                minutes = -minutes
                nanos = -nanos
                // Long.MIN_VALUE will be rejected by constructor
            }
            return Interval(MINUTE_TO_SECOND, negative, minutes, nanos)
        }
    }

    /**
     * Returns years value, if any.
     *
     * @return years, or 0
     */
    fun getYears(): Long = IntervalUtils.yearsFromInterval(qualifier, negative, leading, remaining)

    /**
     * Returns months value, if any.
     *
     * @return months, or 0
     */
    fun getMonths(): Long = IntervalUtils.monthsFromInterval(qualifier, negative, leading, remaining)

    /**
     * Returns days value, if any.
     *
     * @return days, or 0
     */
    fun getDays(): Long = IntervalUtils.daysFromInterval(qualifier, negative, leading, remaining)

    /**
     * Returns hours value, if any.
     *
     * @return hours, or 0
     */
    fun getHours(): Long = IntervalUtils.hoursFromInterval(qualifier, negative, leading, remaining)

    /**
     * Returns minutes value, if any.
     *
     * @return minutes, or 0
     */
    fun getMinutes(): Long = IntervalUtils.minutesFromInterval(qualifier, negative, leading, remaining)

    /**
     * Returns value of integer part of seconds, if any.
     *
     * @return seconds, or 0
     */
    fun getSeconds(): Long = if (qualifier == SECOND) {
        if (negative) -leading else leading
    } else getSecondsAndNanos() / DateTimeUtils.NANOS_PER_SECOND

    /**
     * Returns seconds value measured in nanoseconds, if any.
     *
     * This method returns a long value that cannot fit all possible values of
     * INTERVAL SECOND. For a very large intervals of this type use
     * [.getSeconds] and [.getNanosOfSecond] instead. This
     * method can be safely used for intervals of other day-time types.
     *
     * @return nanoseconds (including seconds), or 0
     */
    fun getSecondsAndNanos(): Long {
        return IntervalUtils.nanosFromInterval(qualifier, negative, leading, remaining)
    }

    override fun hashCode(): Int = Objects.hash(qualifier, negative, leading, remaining)

    override fun equals(other: Any?): Boolean = other is Interval
            && qualifier == other.qualifier
            && negative == other.negative
            && leading == other.leading
            && remaining == other.remaining

    override fun toString(): String {
        return IntervalUtils.appendInterval(StringBuilder(), qualifier, negative, leading, remaining).toString()
    }
}