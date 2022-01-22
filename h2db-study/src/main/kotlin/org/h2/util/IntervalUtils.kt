package org.h2.util

import org.h2.api.ErrorCode
import org.h2.api.IntervalQualifier
import org.h2.api.IntervalQualifier.DAY
import org.h2.api.IntervalQualifier.DAY_TO_HOUR
import org.h2.api.IntervalQualifier.DAY_TO_MINUTE
import org.h2.api.IntervalQualifier.DAY_TO_SECOND
import org.h2.api.IntervalQualifier.HOUR
import org.h2.api.IntervalQualifier.HOUR_TO_MINUTE
import org.h2.api.IntervalQualifier.HOUR_TO_SECOND
import org.h2.api.IntervalQualifier.MINUTE
import org.h2.api.IntervalQualifier.MINUTE_TO_SECOND
import org.h2.api.IntervalQualifier.MONTH
import org.h2.api.IntervalQualifier.SECOND
import org.h2.api.IntervalQualifier.YEAR
import org.h2.api.IntervalQualifier.YEAR_TO_MONTH
import org.h2.message.DbException
import org.h2.util.DateTimeUtils.NANOS_PER_DAY
import org.h2.util.DateTimeUtils.NANOS_PER_MINUTE
import org.h2.util.DateTimeUtils.NANOS_PER_SECOND
import org.h2.util.DateTimeUtils.parseNanos
import org.h2.util.StringUtils.appendTwoDigits
import org.h2.util.StringUtils.parseUInt31
import org.h2.value.ValueInterval
import java.math.BigInteger
import kotlin.math.abs

/**
 * This utility class contains interval conversion functions.
 */
object IntervalUtils {
    private val NANOS_PER_SECOND_BI = BigInteger.valueOf(DateTimeUtils.NANOS_PER_SECOND)

    private val NANOS_PER_MINUTE_BI = BigInteger.valueOf(DateTimeUtils.NANOS_PER_MINUTE)

    private val NANOS_PER_HOUR_BI = BigInteger.valueOf(DateTimeUtils.NANOS_PER_HOUR)

    /**
     * The number of nanoseconds per day as BigInteger.
     */
    val NANOS_PER_DAY_BI = BigInteger.valueOf(DateTimeUtils.NANOS_PER_DAY)

    private val MONTHS_PER_YEAR_BI = BigInteger.valueOf(12)

    private val HOURS_PER_DAY_BI = BigInteger.valueOf(24)

    private val MINUTES_PER_DAY_BI = BigInteger.valueOf(24 * 60.toLong())

    private val MINUTES_PER_HOUR_BI = BigInteger.valueOf(60)

    private val LEADING_MIN = BigInteger.valueOf(-999999999999999999L)

    private val LEADING_MAX = BigInteger.valueOf(999999999999999999L)

    inline fun String.simpleMatches(offset: Int, other: String): Boolean {
        return this.regionMatches(thisOffset = offset, other = other, otherOffset = 0, length = other.length, true)
    }

    /**
     * Parses the specified string as {@code INTERVAL} value.
     *
     * @param qualifier the default qualifier to use if string does not have one
     * @param s the string with type information to parse
     * @return the interval value. Type of value can be different from the
     *         specified qualifier.
     */
    fun parseFormattedInterval(qualifier: IntervalQualifier, s: String): ValueInterval {
        var i: Int = skipWS(s, 0)
        if (!s.simpleMatches(i, "INTERVAL")) {
            return parseInterval(qualifier, false, s)!!
        }
        i = skipWS(s, i + 8)
        var negative: Boolean = false
        var ch = s[i]
        if (ch == '-') {
            negative = true
            i = skipWS(s, i + 1)
            ch = s[i]
        } else if (ch == '+') {
            i = skipWS(s, i + 1)
            ch = s[i]
        }
        require(ch == '\'') { s }

        val start = ++i
        val l = s.length
        while (true) {
            require(i != l) { s }
            if (s[i] == '\'') break
            i++
        }

        val v: String = s.substring(start, i)
        i = skipWS(s, i + 1)

        when {
            s.simpleMatches(i, "YEAR") -> {
                i += 4
                var j = skipWSEnd(s, i)
                if (j == l) return parseInterval(YEAR, negative, v)!!
                if (j > i && s.simpleMatches(j, "TO")) {
                    j += 2
                    i = skipWS(s, j)
                    if (i > j && s.simpleMatches(i, "MONTH")) {
                        if (skipWSEnd(s, i + 5) == l) return parseInterval(YEAR_TO_MONTH, negative, v)!!
                    }
                }
            }
            s.simpleMatches(i, "MONTH") -> {
                if (skipWSEnd(s, i + 5) == l) return parseInterval(MONTH, negative, v)!!
            }
            s.simpleMatches(i, "DAY") -> {
                i += 3
                var j = skipWSEnd(s, i)
                if (j == l) return parseInterval(DAY, negative, v)!!
                if (j > i && s.simpleMatches(j, "TO")) {
                    j += 2
                    i = skipWS(s, j)
                    if (i > j) {
                        if (s.simpleMatches(i, "HOUR")) {
                            if (skipWSEnd(s, i + 4) == l) return parseInterval(DAY_TO_HOUR, negative, v)!!
                        } else if (s.simpleMatches(i, "MINUTE")) {
                            if (skipWSEnd(s, i + 6) == l) return parseInterval(DAY_TO_MINUTE, negative, v)!!
                        } else if (s.simpleMatches(i, "SECOND")) {
                            if (skipWSEnd(s, i + 6) == l) return parseInterval(DAY_TO_SECOND, negative, v)!!
                        }
                    }
                }
            }
            s.simpleMatches(i, "HOUR") -> {
                i += 4
                var j = skipWSEnd(s, i)
                if (j == l) return parseInterval(HOUR, negative, v)!!
                if (j > i && s.simpleMatches(j, "TO")) {
                    j += 2
                    i = skipWS(s, j)
                    if (i > j) {
                        if (s.simpleMatches(i, "MINUTE")) {
                            if (skipWSEnd(s, i + 6) == l) return parseInterval(HOUR_TO_MINUTE, negative, v)!!
                        } else if (s.simpleMatches(i, "SECOND")) {
                            if (skipWSEnd(s, i + 6) == l) return parseInterval(HOUR_TO_SECOND, negative, v)!!
                        }
                    }
                }
            }
            s.simpleMatches(i, "MINUTE") -> {
                i += 6
                var j = skipWSEnd(s, i)
                if (j == l) return parseInterval(MINUTE, negative, v)!!
                if (j > i && s.simpleMatches(j, "TO")) {
                    j += 2
                    i = skipWS(s, j)
                    if (i > j && s.simpleMatches(i, "SECOND")) {
                        if (skipWSEnd(s, i + 6) == l) return parseInterval(MINUTE_TO_SECOND, negative, v)!!
                    }
                }
            }
            s.simpleMatches(i, "SECOND") -> {
                if (skipWSEnd(s, i + 6) == l) return parseInterval(SECOND, negative, v)!!
            }
        }
        throw java.lang.IllegalArgumentException(s)
    }

    private fun parseInterval2(
            qualifier: IntervalQualifier,
            s: String,
            ch: Char,
            max: Int,
            negative: Boolean): ValueInterval? {
        var leading: Long
        val remaining: Long
        val dash = s.indexOf(ch, 1)
        if (dash < 0) {
            leading = parseIntervalLeading(s, 0, s.length, negative)
            remaining = 0
        } else {
            leading = parseIntervalLeading(s, 0, dash, negative)
            remaining = parseIntervalRemaining(s, dash + 1, s.length, max)
        }
        if (leading < 0) {
            leading = if (leading != Long.MIN_VALUE) -leading else 0
        }
        return ValueInterval.from(qualifier, negative, leading, remaining)
    }

    private fun parseIntervalLeading(s: String, start: Int, end: Int, negative: Boolean): Long {
        val leading = s.substring(start, end).toLong()
        if (leading == 0L) {
            return if (negative xor (s[start] == '-')) Long.MIN_VALUE else 0
        }
        return if (negative) -leading else leading
    }

    private fun parseIntervalRemaining(s: String, start: Int, end: Int, max: Int): Long {
        val v: Int = StringUtils.parseUInt31(s, start, end)
        require(v <= max) { s }
        return v.toLong()
    }

    private fun parseIntervalRemainingSeconds(s: String, start: Int): Long {
        val seconds: Int
        val nanos: Int
        val dot = s.indexOf('.', start + 1)
        if (dot < 0) {
            seconds = parseUInt31(s, start, s.length)
            nanos = 0
        } else {
            seconds = parseUInt31(s, start, dot)
            nanos = DateTimeUtils.parseNanos(s, dot + 1, s.length)
        }
        require(seconds <= 59) { s }
        return seconds * DateTimeUtils.NANOS_PER_SECOND + nanos
    }

    /**
     * Parses the specified string as {@code INTERVAL} value.
     *
     * @param qualifier the qualifier of interval
     * @param negative whether the interval is negative
     * @param s the string to parse
     * @return the interval value
     */
    fun parseInterval(qualifier: IntervalQualifier?, negative: Boolean, s: String): ValueInterval? {
        var leading: Long
        var remaining: Long
        when (qualifier) {
            YEAR, MONTH, DAY, HOUR, MINUTE -> {
                leading = parseIntervalLeading(s, 0, s.length, negative)
                remaining = 0
            }
            SECOND -> {
                val dot = s.indexOf('.')
                if (dot < 0) {
                    leading = parseIntervalLeading(s, 0, s.length, negative)
                    remaining = 0
                } else {
                    leading = parseIntervalLeading(s, 0, dot, negative)
                    remaining = parseNanos(s, dot + 1, s.length).toLong()
                }
            }
            YEAR_TO_MONTH -> return parseInterval2(qualifier, s, '-', 11, negative)
            DAY_TO_HOUR -> return parseInterval2(qualifier, s, ' ', 23, negative)
            DAY_TO_MINUTE -> {
                val space = s.indexOf(' ')
                if (space < 0) {
                    leading = parseIntervalLeading(s, 0, s.length, negative)
                    remaining = 0
                } else {
                    leading = parseIntervalLeading(s, 0, space, negative)
                    val colon = s.indexOf(':', space + 1)
                    remaining = if (colon < 0) {
                        parseIntervalRemaining(s, space + 1, s.length, 23) * 60
                    } else {
                        (parseIntervalRemaining(s, space + 1, colon, 23) * 60
                                + parseIntervalRemaining(s, colon + 1, s.length, 59))
                    }
                }
            }
            DAY_TO_SECOND -> {
                val space = s.indexOf(' ')
                if (space < 0) {
                    leading = parseIntervalLeading(s, 0, s.length, negative)
                    remaining = 0
                } else {
                    leading = parseIntervalLeading(s, 0, space, negative)
                    val colon = s.indexOf(':', space + 1)
                    remaining = if (colon < 0) {
                        parseIntervalRemaining(s, space + 1, s.length, 23) * DateTimeUtils.NANOS_PER_HOUR
                    } else {
                        val colon2 = s.indexOf(':', colon + 1)
                        if (colon2 < 0) {
                            (parseIntervalRemaining(s, space + 1, colon, 23) * DateTimeUtils.NANOS_PER_HOUR
                                    + parseIntervalRemaining(s, colon + 1, s.length, 59) * NANOS_PER_MINUTE)
                        } else {
                            parseIntervalRemaining(s, space + 1, colon, 23) * DateTimeUtils.NANOS_PER_HOUR
                            +parseIntervalRemaining(s, colon + 1, colon2, 59) * NANOS_PER_MINUTE
                            +parseIntervalRemainingSeconds(s, colon2 + 1)
                        }
                    }
                }
            }
            HOUR_TO_MINUTE -> return parseInterval2(qualifier, s, ':', 59, negative)
            HOUR_TO_SECOND -> {
                val colon = s.indexOf(':')
                if (colon < 0) {
                    leading = parseIntervalLeading(s, 0, s.length, negative)
                    remaining = 0
                } else {
                    leading = parseIntervalLeading(s, 0, colon, negative)
                    val colon2 = s.indexOf(':', colon + 1)
                    remaining = if (colon2 < 0) {
                        parseIntervalRemaining(s, colon + 1, s.length, 59) * NANOS_PER_MINUTE
                    } else {
                        (parseIntervalRemaining(s, colon + 1, colon2, 59) * NANOS_PER_MINUTE
                                + parseIntervalRemainingSeconds(s, colon2 + 1))
                    }
                }
            }
            MINUTE_TO_SECOND -> {
                val dash = s.indexOf(':')
                if (dash < 0) {
                    leading = parseIntervalLeading(s, 0, s.length, negative)
                    remaining = 0
                } else {
                    leading = parseIntervalLeading(s, 0, dash, negative)
                    remaining = parseIntervalRemainingSeconds(s, dash + 1)
                }
            }
            else -> throw IllegalArgumentException()
        }
        if (leading < 0) {
            leading = if (leading != Long.MIN_VALUE) -leading else 0
        }
        return ValueInterval.from(qualifier, negative, leading, remaining)
    }

    private fun skipWS(s: String, i: Int): Int {
        var i = i
        val l = s.length
        while (true) {
            require(i != l) { s }
            if (!Character.isWhitespace(s[i])) return i
            i++
        }
    }

    private fun skipWSEnd(s: String, i: Int): Int {
        var i = i
        val l = s.length
        while (true) {
            if (i == l) return i
            if (!Character.isWhitespace(s[i])) return i
            i++
        }
    }

    private fun intervalToAbsolute(interval: ValueInterval, multiplier: BigInteger, totalMultiplier: BigInteger): BigInteger {
        return intervalToAbsolute(interval, multiplier).multiply(totalMultiplier)
    }

    private fun intervalToAbsolute(interval: ValueInterval, multiplier: BigInteger): BigInteger {
        return BigInteger.valueOf(interval.leading)
                .multiply(multiplier)
                .add(BigInteger.valueOf(interval.remaining))
    }

    /**
     * Converts absolute value to an interval value.
     *
     * @param qualifier the qualifier of interval
     * @param absolute absolute value in months for year-month intervals, in nanoseconds for day-time intervals
     * @return the interval value
     */
    fun intervalFromAbsolute(qualifier: IntervalQualifier?, absolute: BigInteger): ValueInterval {
        val negative: Boolean = absolute.signum() < 0

        return when (qualifier) {
            YEAR -> ValueInterval.from(qualifier, negative, leadingExact(absolute.divide(MONTHS_PER_YEAR_BI)), 0)
            MONTH -> ValueInterval.from(qualifier, negative, leadingExact(absolute), 0)
            DAY -> ValueInterval.from(qualifier, negative, leadingExact(absolute.divide(NANOS_PER_DAY_BI)), 0)
            HOUR -> ValueInterval.from(qualifier, negative, leadingExact(absolute.divide(NANOS_PER_HOUR_BI)), 0)
            MINUTE -> ValueInterval.from(qualifier, negative, leadingExact(absolute.divide(NANOS_PER_MINUTE_BI)), 0)
            SECOND -> intervalFromAbsolute(qualifier, absolute, NANOS_PER_SECOND_BI)

            YEAR_TO_MONTH -> intervalFromAbsolute(qualifier, absolute, MONTHS_PER_YEAR_BI)
            DAY_TO_HOUR -> intervalFromAbsolute(qualifier, absolute.divide(NANOS_PER_HOUR_BI), HOURS_PER_DAY_BI)
            DAY_TO_MINUTE -> intervalFromAbsolute(qualifier, absolute.divide(NANOS_PER_MINUTE_BI), MINUTES_PER_DAY_BI)
            DAY_TO_SECOND -> intervalFromAbsolute(qualifier, absolute, NANOS_PER_DAY_BI)
            HOUR_TO_MINUTE -> intervalFromAbsolute(qualifier, absolute.divide(NANOS_PER_MINUTE_BI), MINUTES_PER_HOUR_BI)
            HOUR_TO_SECOND -> intervalFromAbsolute(qualifier, absolute, NANOS_PER_HOUR_BI)
            MINUTE_TO_SECOND -> intervalFromAbsolute(qualifier, absolute, NANOS_PER_MINUTE_BI)
            else -> throw IllegalArgumentException()
        }
    }

    fun intervalFromAbsolute(qualifier: IntervalQualifier, absolute: BigInteger, divisor: BigInteger): ValueInterval {
        val dr = absolute.divideAndRemainder(divisor)
        return ValueInterval.from(qualifier, absolute.signum() < 0, leadingExact(dr[0]), abs(dr[1].toLong()))
    }

    private fun leadingExact(absolute: BigInteger): Long {
        if (absolute > LEADING_MAX || absolute < LEADING_MIN) {
            throw DbException.get(ErrorCode.NUMERIC_VALUE_OUT_OF_RANGE_1, absolute.toString())
        }
        return abs(absolute.toLong())
    }

    /**
     * Ensures that all fields in interval are valid.
     *
     * @param qualifier qualifier
     * @param negative whether interval is negative
     * @param leading value of leading field
     * @param remaining values of all remaining fields
     * @return fixed value of negative field
     */
    fun validateInterval(qualifier: IntervalQualifier, negative: Boolean, leading: Long, remaining: Long): Boolean {
        if (leading == 0L && remaining == 0L) return false

        val bound: Long = when (qualifier) {
            YEAR, MONTH, DAY, HOUR, MINUTE -> 1
            SECOND -> NANOS_PER_SECOND
            YEAR_TO_MONTH -> 12
            DAY_TO_HOUR -> 24
            DAY_TO_MINUTE -> 24 * 60
            DAY_TO_SECOND -> NANOS_PER_DAY
            HOUR_TO_MINUTE -> 60
            MINUTE_TO_SECOND -> NANOS_PER_MINUTE
            else -> throw DbException.getInvalidValueException("interval", qualifier)
        }

        if (leading < 0L || leading >= 1000000000000000000L) {
            throw DbException.getInvalidValueException("interval", leading.toString())
        }
        if (remaining < 0L || remaining >= bound) {
            throw DbException.getInvalidValueException("interval", remaining.toString())
        }
        return negative
    }

    /**
     * Formats interval as a string and appends it to a specified string
     * builder.
     *
     * @param buff
     * string builder to append to
     * @param qualifier
     * qualifier of the interval
     * @param negative
     * whether interval is negative
     * @param leading
     * the value of leading field
     * @param remaining
     * the value of all remaining fields
     * @return the specified string builder
     */
    fun appendInterval(buff: StringBuilder, qualifier: IntervalQualifier?, negative: Boolean,
                       leading: Long, remaining: Long): StringBuilder {
        buff.append("INTERVAL '")
        if (negative) {
            buff.append('-')
        }
        when (qualifier) {
            YEAR, MONTH, DAY, HOUR, MINUTE -> buff.append(leading)
            SECOND -> DateTimeUtils.appendNanos(buff.append(leading), remaining.toInt())
            YEAR_TO_MONTH -> buff.append(leading).append('-').append(remaining)
            DAY_TO_HOUR -> {
                buff.append(leading).append(' ')
                buff.appendTwoDigits(remaining.toInt())
            }
            DAY_TO_MINUTE -> {
                buff.append(leading).append(' ')
                val r = remaining.toInt()
                buff.appendTwoDigits(r / 60).append(':')
                buff.appendTwoDigits(r % 60)
            }
            DAY_TO_SECOND -> {
                val nanos = remaining % NANOS_PER_MINUTE
                val r = (remaining / NANOS_PER_MINUTE).toInt()
                buff.append(leading).append(' ')
                buff.appendTwoDigits(r / 60).append(':')
                buff.appendTwoDigits(r % 60).append(':')
                buff.appendTwoDigits((nanos / NANOS_PER_SECOND).toInt())
                DateTimeUtils.appendNanos(buff, (nanos % NANOS_PER_SECOND).toInt())
            }
            HOUR_TO_MINUTE -> {
                buff.append(leading).append(':')
                buff.appendTwoDigits(remaining.toInt())
            }
            HOUR_TO_SECOND -> {
                buff.append(leading).append(':')
                buff.appendTwoDigits((remaining / NANOS_PER_MINUTE).toInt()).append(':')
                val s = remaining % NANOS_PER_MINUTE
                buff.appendTwoDigits((s / NANOS_PER_SECOND).toInt())
                DateTimeUtils.appendNanos(buff, (s % NANOS_PER_SECOND).toInt())
            }
            MINUTE_TO_SECOND -> {
                buff.append(leading).append(':')
                buff.appendTwoDigits((remaining / NANOS_PER_SECOND).toInt())
                DateTimeUtils.appendNanos(buff, (remaining % NANOS_PER_SECOND).toInt())
            }
        }
        return buff.append("' ").append(qualifier)
    }


    /**
     * Returns years value of interval, if any.
     *
     * @param qualifier  qualifier
     * @param negative whether interval is negative
     * @param leading value of leading field
     * @param remaining values of all remaining fields
     * @return minutes, or 0
     */
    fun yearsFromInterval(qualifier: IntervalQualifier, negative: Boolean, leading: Long, remaining: Long): Long =
            if (qualifier == YEAR || qualifier == YEAR_TO_MONTH) {
                if (negative) -leading else leading
            } else 0

    /**
     * Returns months value of interval, if any.
     *
     * @param qualifier  qualifier
     * @param negative whether interval is negative
     * @param leading value of leading field
     * @param remaining values of all remaining fields
     * @return minutes, or 0
     */
    fun monthsFromInterval(qualifier: IntervalQualifier, negative: Boolean, leading: Long, remaining: Long): Long = when (qualifier) {
        MONTH -> leading
        YEAR_TO_MONTH -> remaining
        else -> 0
    }.let { if (negative) -it else it }

    /**
     * Returns days value of interval, if any.
     *
     * @param qualifier  qualifier
     * @param negative whether interval is negative
     * @param leading value of leading field
     * @param remaining values of all remaining fields
     * @return minutes, or 0
     */
    fun daysFromInterval(qualifier: IntervalQualifier?, negative: Boolean, leading: Long, remaining: Long): Long =
            when (qualifier) {
                DAY, DAY_TO_HOUR, DAY_TO_MINUTE, DAY_TO_SECOND -> if (negative) -leading else leading
                else -> 0
            }

    /**
     * Returns hours value of interval, if any.
     *
     * @param qualifier  qualifier
     * @param negative whether interval is negative
     * @param leading value of leading field
     * @param remaining values of all remaining fields
     * @return minutes, or 0
     */
    fun hoursFromInterval(qualifier: IntervalQualifier?, negative: Boolean, leading: Long, remaining: Long): Long = when (qualifier) {
        HOUR, HOUR_TO_MINUTE, HOUR_TO_SECOND -> leading
        DAY_TO_HOUR -> remaining
        DAY_TO_MINUTE -> remaining / 60
        DAY_TO_SECOND -> remaining / DateTimeUtils.NANOS_PER_HOUR
        else -> 0
    }.let { if (negative) -it else it }

    /**
     * Returns minutes value of interval, if any.
     *
     * @param qualifier  qualifier
     * @param negative whether interval is negative
     * @param leading value of leading field
     * @param remaining values of all remaining fields
     * @return minutes, or 0
     */
    fun minutesFromInterval(qualifier: IntervalQualifier?, negative: Boolean, leading: Long, remaining: Long): Long = when (qualifier) {
        MINUTE, MINUTE_TO_SECOND -> leading
        DAY_TO_MINUTE -> remaining % 60
        DAY_TO_SECOND -> remaining / NANOS_PER_MINUTE % 60
        HOUR_TO_MINUTE -> remaining
        HOUR_TO_SECOND -> remaining / NANOS_PER_MINUTE
        else -> 0
    }.let { if (negative) -it else it }

    /**
     * Returns nanoseconds value of interval, if any.
     *
     * @param qualifier qualifier
     * @param negative whether interval is negative
     * @param leading value of leading field
     * @param remaining values of all remaining fields
     * @return nanoseconds, or 0
     */
    fun nanosFromInterval(qualifier: IntervalQualifier?, negative: Boolean, leading: Long, remaining: Long): Long = when (qualifier) {
        SECOND -> leading * NANOS_PER_SECOND + remaining
        DAY_TO_SECOND, HOUR_TO_SECOND -> remaining % NANOS_PER_MINUTE
        MINUTE_TO_SECOND -> remaining
        else -> 0
    }.let { if (negative) -it else it }

    /**
     * Converts interval value to an absolute value.
     *
     * @param interval
     * the interval value
     * @return absolute value in months for year-month intervals, in nanoseconds
     * for day-time intervals
     */
    fun intervalToAbsolute(interval: ValueInterval): BigInteger {
        val r: BigInteger = when (interval.getQualifier()) {
            YEAR -> BigInteger.valueOf(interval.leading).multiply(MONTHS_PER_YEAR_BI)
            MONTH -> BigInteger.valueOf(interval.leading)
            DAY -> BigInteger.valueOf(interval.leading).multiply(NANOS_PER_DAY_BI)
            HOUR -> BigInteger.valueOf(interval.leading).multiply(NANOS_PER_HOUR_BI)
            MINUTE -> BigInteger.valueOf(interval.leading).multiply(NANOS_PER_MINUTE_BI)
            SECOND -> intervalToAbsolute(interval, NANOS_PER_SECOND_BI)
            YEAR_TO_MONTH -> intervalToAbsolute(interval, MONTHS_PER_YEAR_BI)
            DAY_TO_HOUR -> intervalToAbsolute(interval, HOURS_PER_DAY_BI, NANOS_PER_HOUR_BI)
            DAY_TO_MINUTE -> intervalToAbsolute(interval, MINUTES_PER_DAY_BI, NANOS_PER_MINUTE_BI)
            DAY_TO_SECOND -> intervalToAbsolute(interval, NANOS_PER_DAY_BI)
            HOUR_TO_MINUTE -> intervalToAbsolute(interval, MINUTES_PER_HOUR_BI, NANOS_PER_MINUTE_BI)
            HOUR_TO_SECOND -> intervalToAbsolute(interval, NANOS_PER_HOUR_BI)
            MINUTE_TO_SECOND -> intervalToAbsolute(interval, NANOS_PER_MINUTE_BI)
            else -> throw IllegalArgumentException()
        }
        return if (interval.isNegative()) r.negate() else r
    }
}