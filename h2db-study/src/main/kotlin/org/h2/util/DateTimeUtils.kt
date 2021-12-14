package org.h2.util

import org.h2.util.StringUtils.appendTwoDigits
import java.util.Date
import java.util.TimeZone

/**
 * This utility class contians time conversion functions.
 * <p>
 *     Date value: a bit field with bits for the year, month, and day. Absolute day:
 *     the day number (0 means 1970-01-01).
 */
object DateTimeUtils {
    /**
     * The number of milliseconds per day
     */
    const val MILLIS_PER_DAY: Long = 24 * 60 * 60 * 1000L

    /**
     * The number of seconds per day
     */
    const val SECOND_PER_DAY: Long = 24 * 60 * 60

    /**
     * UTC time zone.
     */
    val UTC: TimeZone = TimeZone.getTimeZone("UTC")

    /**
     * The number of nanoseconds per second.
     */
    const val NANOS_PER_SECOND: Long = 1_000_000_000

    /**
     * The number of nanoseconds per minute
     */
    const val NANOS_PER_MINUTE: Long = 60 * NANOS_PER_SECOND

    /**
     * The number of nanoseconds per hour.
     */
    const val NANOS_PER_HOUR: Long = 60 * NANOS_PER_MINUTE

    /**
     * The number of nanoseconds per day.
     */
    const val NANOS_PER_DAY: Long = MILLIS_PER_DAY * 1_000_000

    /**
     * The number of seconds per day.
     */
    const val SECONDS_PER_DAY = (24 * 60 * 60).toLong()

    private const val SHIFT_YEAR: Int = 9
    private const val SHIFT_MONTH: Int = 5

    /**
     * Minimum possible date value.
     */
    const val MIN_DATE_VALUE = (-1000000000L shl SHIFT_YEAR) + (1 shl SHIFT_MONTH) + 1

    /**
     * Maximum possible date value.
     */
    const val MAX_DATE_VALUE = (1000000000L shl SHIFT_YEAR) + (12 shl SHIFT_MONTH) + 31

    /**
     * Gregorian change date for a {@link GregorianCalendar} that represents a
     * proleptic Gregorian calendar.
     */
    val PROLEPTIC_GERGORIAN_CHANGE: Date = Date(Long.MIN_VALUE)

    /**
     * Date value for 1970-01-01
     */
    const val EPOCH_DATE_VALUE: Int = (1970 shl SHIFT_YEAR) + (1 shl SHIFT_MONTH) + 1

    val NORMAL_DAYS_PER_MONTH: kotlin.IntArray = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

    /**
     * Multipliers for [.convertScale].
     */
    private val CONVERT_SCALE_TABLE = intArrayOf(1000000000, 100000000, 10000000, 1000000, 100000, 10000, 1000, 100, 10)

    /**
     * Multipliers for [.convertScale] and
     * [.appendNanos].
     */
    private val FRACTIONAL_SECONDS_TABLE = intArrayOf(1000000000, 100000000, 10000000, 1000000, 100000, 10000, 1000, 100, 10, 1)

    /**
     * Parse nanoseconds.
     *
     * @param s String to parse.
     * @param start Begin position at the string to read.
     * @param end End position at the string to read.
     * @return Parsed nanoseconds.
     */
    fun parseNanos(s: String, start: Int, end: Int): Int {
        var start = start
        require(start < end) { s }
        var nanos = 0
        var mul = 100000000
        do {
            val c = s[start]
            require(!(c < '0' || c > '9')) { s }
            nanos += mul * (c - '0')
            // mul can become 0, but continue loop anyway to ensure that all
            // remaining digits are valid
            mul /= 10
        } while (++start < end)
        return nanos
    }

    /**
     * Append nanoseconds of time, if any.
     *
     * @param builder string builder to append to
     * @param nanos nanoseconds of second
     * @return the specified string builder
     */
    fun appendNanos(builder: StringBuilder, nanos: Int): StringBuilder? {
        if (nanos <= 0) return builder

        var nanos = nanos
        builder.append('.')
        var i = 1
        while (nanos < FRACTIONAL_SECONDS_TABLE[i]) {
            builder.append('0')
            i++
        }
        if (nanos % 1000 == 0) {
            nanos /= 1000
            if (nanos % 1000 == 0) {
                nanos /= 1000
            }
        }
        if (nanos % 10 == 0) {
            nanos /= 10
            if (nanos % 10 == 0) {
                nanos /= 10
            }
        }
        builder.append(nanos)
        return builder
    }

    /**
     * Append a time zone to the string builder.
     *
     * @param builder the target string builder
     * @param tz the time zone offset in seconds
     * @return the specified string builder
     */
    fun appendTimeZone(builder: StringBuilder, tz: Int): StringBuilder {
        var tz = tz
        if (tz < 0) {
            builder.append('-')
            tz = -tz
        } else {
            builder.append('+')
        }
        var rem = tz / 3600
        builder.appendTwoDigits(rem)
        tz -= rem * 3600
        if (tz != 0) {
            rem = tz / 60
            builder.append(':').appendTwoDigits(rem)
            tz -= rem * 60
            if (tz != 0) {
                builder.append(':').appendTwoDigits(tz)
            }
        }
        return builder
    }

    /**
     * Calculate the absolute day for a January, 1 of the specified year.
     *
     * @param year
     * the year
     * @return the absolute day
     */
    fun absoluteDayFromYear(year: Long): Long {
        var a: Long = 365 * year - 719528
        if (year >= 0) {
            a += (year + 3) / 4 - (year + 99) / 100 + (year + 399) / 400
        } else {
            a -= year / -4 - year / -100 + year / -400
        }
        return a
    }

    /**
     * Calculate the absolute day.
     *
     * @param y year
     * @param m month
     * @param d day
     * @return the absolute day
     */
    fun absoluteDay(y: Long, m: Int, d: Int): Long {
        var a: Long = absoluteDayFromYear(y) + (367 * m - 362) / 12 + d - 1
        if (m > 2) {
            a--
            if (y and 3 != 0L || y % 100 == 0L && y % 400 != 0L) {
                a--
            }
        }
        return a
    }

    /**
     * Get the date value from a given date.
     *
     * @param year the year
     * @param month the month (1..12)
     * @param day the day (1..31)
     * @return the date value
     */
    fun dateValue(year: Long, month: Int, day: Int): Long = year shl SHIFT_YEAR or (month shl SHIFT_MONTH).toLong() or day.toLong()

    /**
     * Calculate the encoded date value from an absolute day.
     *
     * @param absoluteDay the absolute day
     * @return the date value
     */
    fun dateValueFromAbsoluteDay(absoluteDay: Long): Long {
        var d = absoluteDay + 719468
        var a: Long = 0
        if (d < 0) {
            a = (d + 1) / 146097 - 1
            d -= a * 146097
            a *= 400
        }
        var y = (400 * d + 591) / 146097
        var day = (d - (365 * y + y / 4 - y / 100 + y / 400)).toInt()
        if (day < 0) {
            y--
            day = (d - (365 * y + y / 4 - y / 100 + y / 400)).toInt()
        }
        y += a
        var m = (day * 5 + 2) / 153
        day -= (m * 306 + 5) / 10 - 1
        if (m >= 10) {
            y++
            m -= 12
        }
        return DateTimeUtils.dateValue(y, m + 3, day)
    }

    /**
     * Get the date value from a given denormalized date with possible out of range
     * values of month and/or day. Used after addition or subtraction month or years
     * to (from) it to get a valid date.
     *
     * @param year the year
     * @param month the month, if out of range month and year will be normalized
     * @param day the day of the month, if out of range it will be saturated
     * @return the date value
     */
    fun dateValueFromDenormalizedDate(year: Long, month: Long, day: Int): Long {
        var day = day

        val mm1 = month - 1
        var yd = mm1 / 12

        if (mm1 < 0 && yd * 12 != mm1) {
            yd--
        }

        val y = (year + yd).toInt()
        val m = (month - yd * 12).toInt()

        if (day < 1) day = 1
        else {
            val max: Int = getDaysInMonth(y, m)
            if (day > max) day = max
        }

        return dateValue(y.toLong(), m, day)
    }

    /**
     * Returns number of days in month.
     *
     * @param year the year
     * @param month the month
     * @return number of days in the specified month
     */
    fun getDaysInMonth(year: Int, month: Int): Int {
        if (month != 2) {
            return NORMAL_DAYS_PER_MONTH[month]
        }
        return if (year and 3 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    }

    /**
     * Get the year from a date value.
     *
     * @param x the date value
     * @return the year
     */
    fun yearFromDateValue(x: Long): Int = (x ushr SHIFT_YEAR).toInt()


    /**
     * Get the month from a date value.
     *
     * @param x the date value
     * @return the month (1..12)
     */
    fun monthFromDateValue(x: Long): Int = (x ushr SHIFT_MONTH).toInt() and 15

    /**
     * Get the day of month from a date value.
     *
     * @param x the date value
     * @return the day (1..31)
     */
    fun dayFromDateValue(x: Long): Int = (x and 31).toInt()

    /**
     * Calculate the absolute day from an encoded date value.
     *
     * @param dateValue the date value
     * @return the absolute day
     */
    fun absoluteDayFromDateValue(dateValue: Long): Long {
        return absoluteDay(yearFromDateValue(dateValue).toLong(), monthFromDateValue(dateValue), dayFromDateValue(dateValue))
    }

    /**
     * Calculates the seconds since epoch for the specified date value,
     * nanoseconds since midnight, and time zone offset.
     * @param dateValue
     * date value
     * @param timeNanos
     * nanoseconds since midnight
     * @param offsetSeconds
     * time zone offset in seconds
     * @return seconds since epoch in UTC
     */
    fun getEpochSeconds(dateValue: Long, timeNanos: Long, offsetSeconds: Int): Long {
        return absoluteDayFromDateValue(dateValue) * SECONDS_PER_DAY + timeNanos / NANOS_PER_SECOND - offsetSeconds
    }

    /**
     * Convert a local seconds to an encoded date.
     *
     * @param localSeconds the seconds since 1970-01-01
     * @return the date value
     */
    fun dateValueFromLocalSeconds(localSeconds: Long): Long {
        var absoluteDay = localSeconds / SECONDS_PER_DAY
        // Round toward negative infinity
        if (localSeconds < 0 && absoluteDay * SECONDS_PER_DAY != localSeconds) {
            absoluteDay--
        }
        return dateValueFromAbsoluteDay(absoluteDay)
    }

    /**
     * Converts scale of nanoseconds.
     *
     * @param nanosOfDay nanoseconds of day
     * @param scale fractional seconds precision
     * @param range the allowed range of values (0..range-1)
     * @return scaled value
     */
    fun convertScale(nanosOfDay: Long, scale: Int, range: Long): Long {
        var nanosOfDay = nanosOfDay
        if (scale >= 9) return nanosOfDay

        val m = FRACTIONAL_SECONDS_TABLE[scale]
        val mod = nanosOfDay % m
        if (mod >= m ushr 1) {
            nanosOfDay += m.toLong()
        }
        var r = nanosOfDay - mod
        if (r >= range) {
            r = range - m
        }
        return r
    }


    /**
     * Convert a time in seconds in local time to the nanoseconds since midnight.
     *
     * @param localSeconds the seconds since 1970-01-01
     * @return the nanoseconds
     */
    fun nanosFromLocalSeconds(localSeconds: Long): Long {
        var localSeconds = localSeconds
        localSeconds %= SECONDS_PER_DAY
        if (localSeconds < 0) localSeconds += SECONDS_PER_DAY
        return localSeconds * NANOS_PER_SECOND
    }

    /**
     * Calculate the normalized nanos of day.
     *
     * @param nanos the nanoseconds (may be negative or larger than one day)
     * @return the nanos of day within a day
     */
    fun normalizeNanosOfDay(nanos: Long): Long {
        var nanos = nanos
        nanos %= NANOS_PER_DAY
        if (nanos < 0) nanos += NANOS_PER_DAY
        return nanos
    }

    /**
     * Return the next date value.
     *
     * @param dateValue
     * the date value
     * @return the next date value
     */
    fun incrementDateValue(dateValue: Long): Long {
        val day = dayFromDateValue(dateValue)
        if (day < 28) return dateValue + 1

        var year = yearFromDateValue(dateValue)
        var month = monthFromDateValue(dateValue)
        if (day < getDaysInMonth(year, month)) return dateValue + 1

        if (month < 12) {
            month++
        } else {
            month = 1
            year++
        }
        return dateValue(year.toLong(), month, 1)
    }
}