package org.h2.util

import org.h2.api.ErrorCode
import org.h2.api.IntervalQualifier
import org.h2.api.IntervalQualifier.*
import org.h2.message.DbException
import org.h2.util.DateTimeUtils.NANOS_PER_DAY
import org.h2.util.DateTimeUtils.NANOS_PER_MINUTE
import org.h2.util.DateTimeUtils.NANOS_PER_SECOND
import org.h2.util.DateTimeUtils.parseNanos
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
        if (!s.regionMatches(i, "INTERVAL", 0, 8, true)) {

        }
    }

    private fun parseInterval2(qualifier: IntervalQualifier,
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

    private fun intervalToAbsolute(interval: ValueInterval,
                                   multiplier: BigInteger,
                                   totalMultiplier: BigInteger): BigInteger? {
        return intervalToAbsolute(interval, multiplier)!!.multiply(totalMultiplier)
    }

    private fun intervalToAbsolute(interval: ValueInterval,
                                   multiplier: BigInteger): BigInteger? {
        return BigInteger.valueOf(interval.leading)
                .multiply(multiplier)
                .add(BigInteger.valueOf(interval.remaining))
    }

    private fun intervalFromAbsolute(qualifier: IntervalQualifier,
                                     absolute: BigInteger,
                                     divisor: BigInteger): ValueInterval? {
        val dr = absolute.divideAndRemainder(divisor)
        return ValueInterval.from(qualifier, absolute.signum() < 0, leadingExact(dr[0]), Math.abs(dr[1].toLong()))
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
}