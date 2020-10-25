package org.h2.util

import java.util.*

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

    private const val SHIFT_YEAR: Int = 9
    private const val SHIFT_MONTH: Int = 5

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
    private val CONVERT_SCALE_TABLE = intArrayOf(1000000000, 100000000,
            10000000, 1000000, 100000, 10000, 1000, 100, 10)

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
}