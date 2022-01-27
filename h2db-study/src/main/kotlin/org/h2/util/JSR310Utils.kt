package org.h2.util

import org.h2.api.ErrorCode
import org.h2.engine.CastDataProvider
import org.h2.message.DbException
import org.h2.util.DateTimeUtils.absoluteDayFromDateValue
import org.h2.util.DateTimeUtils.dayFromDateValue
import org.h2.util.DateTimeUtils.monthFromDateValue
import org.h2.util.DateTimeUtils.yearFromDateValue
import org.h2.util.IntervalUtils.monthsFromInterval
import org.h2.util.IntervalUtils.yearsFromInterval
import org.h2.value.DataType
import org.h2.value.TypeInfo
import org.h2.value.Value
import org.h2.value.ValueDate.Companion.convertToDate
import org.h2.value.ValueInterval
import org.h2.value.ValueTime
import org.h2.value.ValueTimeTimeZone
import org.h2.value.ValueTimestamp
import org.h2.value.ValueTimestampTimeZone
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Period
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * This utility class provides access to JSR 310 classes.
 */
object JSR310Utils {
    private const val MIN_DATE_VALUE = ((-999999999L shl DateTimeUtils.SHIFT_YEAR)
            + (1 shl DateTimeUtils.SHIFT_MONTH) + 1)

    private const val MAX_DATE_VALUE = ((999999999L shl DateTimeUtils.SHIFT_YEAR)
            + (12 shl DateTimeUtils.SHIFT_MONTH) + 31)

    private const val MIN_INSTANT_SECOND = -31557014167219200L

    private const val MAX_INSTANT_SECOND = 31556889864403199L

    private val DATE_RANGE: LongRange = LongRange(MIN_DATE_VALUE, MAX_DATE_VALUE)

    /**
     * Converts a value to a LocalDate.
     *
     * This method should only be called from Java 8 or later version.
     *
     * @param value the value to convert
     * @param provider the cast information provider
     * @return the LocalDate
     */
    fun valueToLocalDate(value: Value, provider: CastDataProvider): Any? {
        val dateValue: Long = value.convertToDate(provider).dateValue.coerceIn(DATE_RANGE)
        return LocalDate.of(yearFromDateValue(dateValue),
                monthFromDateValue(dateValue),
                dayFromDateValue(dateValue))
    }

    /**
     * Converts a value to a LocalTime.
     *
     * This method should only be called from Java 8 or later version.
     *
     * @param value the value to convert
     * @param provider the cast information provider
     * @return the LocalTime
     */
    fun valueToLocalTime(value: Value, provider: CastDataProvider?): Any? {
        return LocalTime.ofNanoOfDay((value.convertTo(TypeInfo.TYPE_TIME, provider) as ValueTime).nanos)
    }

    /**
     * Converts a value to a LocalDateTime.
     *
     * This method should only be called from Java 8 or later version.
     *
     * @param value the value to convert
     * @param provider the cast information provider
     * @return the LocalDateTime
     */
    fun valueToLocalDateTime(value: Value, provider: CastDataProvider?): Any? {
        val valueTimestamp = value.convertTo(TypeInfo.TYPE_TIMESTAMP, provider) as ValueTimestamp
        return localDateTimeFromDateNanos(valueTimestamp.dateValue, valueTimestamp.timeNanos)
    }

    /**
     * Converts a value to a Instant.
     *
     * This method should only be called from Java 8 or later version.
     *
     * @param value the value to convert
     * @param provider the cast information provider
     * @return the Instant
     */
    fun valueToInstant(value: Value, provider: CastDataProvider?): Any? {
        val valueTimestampTimeZone = value.convertTo(TypeInfo.TYPE_TIMESTAMP_TZ, provider) as ValueTimestampTimeZone
        var timeNanos: Long = valueTimestampTimeZone.timeNanos
        var epochSecond: Long = (absoluteDayFromDateValue( //
                valueTimestampTimeZone.dateValue) * DateTimeUtils.SECONDS_PER_DAY //
                + timeNanos / DateTimeUtils.NANOS_PER_SECOND //
                - valueTimestampTimeZone.timeZoneOffsetSeconds)
        timeNanos %= DateTimeUtils.NANOS_PER_SECOND

        if (epochSecond > MAX_INSTANT_SECOND) {
            epochSecond = MAX_INSTANT_SECOND
            timeNanos = DateTimeUtils.NANOS_PER_SECOND - 1
        } else if (epochSecond < MIN_INSTANT_SECOND) {
            epochSecond = MIN_INSTANT_SECOND
            timeNanos = 0
        }
        return Instant.ofEpochSecond(epochSecond, timeNanos)
    }

    /**
     * Converts a value to a OffsetDateTime.
     *
     * This method should only be called from Java 8 or later version.
     *
     * @param value the value to convert
     * @param provider the cast information provider
     * @return the OffsetDateTime
     */
    fun valueToOffsetDateTime(value: Value, provider: CastDataProvider): Any? {
        return valueToOffsetDateTime(value, provider, false)
    }

    /**
     * Converts a value to a ZonedDateTime.
     *
     * This method should only be called from Java 8 or later version.
     *
     * @param value the value to convert
     * @param provider the cast information provider
     * @return the ZonedDateTime
     */
    fun valueToZonedDateTime(value: Value, provider: CastDataProvider): Any? {
        return valueToOffsetDateTime(value, provider, true)
    }

    private fun valueToOffsetDateTime(value: Value, provider: CastDataProvider, zoned: Boolean): Any? {
        val valueTimestampTimeZone = value
                .convertTo(TypeInfo.TYPE_TIMESTAMP_TZ, provider) as ValueTimestampTimeZone
        val dateValue: Long = valueTimestampTimeZone.dateValue
        val timeNanos: Long = valueTimestampTimeZone.timeNanos
        val localDateTime = localDateTimeFromDateNanos(dateValue, timeNanos) as LocalDateTime?
        val timeZoneOffsetSeconds: Int = valueTimestampTimeZone.timeZoneOffsetSeconds
        val offset = ZoneOffset.ofTotalSeconds(timeZoneOffsetSeconds)
        return if (zoned) ZonedDateTime.of(localDateTime, offset) else OffsetDateTime.of(localDateTime, offset)
    }

    /**
     * Converts a value to a OffsetTime.
     *
     * This method should only be called from Java 8 or later version.
     *
     * @param value the value to convert
     * @param provider the cast information provider
     * @return the OffsetTime
     */
    fun valueToOffsetTime(value: Value, provider: CastDataProvider?): Any? {
        val valueTimeTimeZone = value.convertTo(TypeInfo.TYPE_TIME_TZ, provider) as ValueTimeTimeZone
        return OffsetTime.of(LocalTime.ofNanoOfDay(valueTimeTimeZone.nanos),
                ZoneOffset.ofTotalSeconds(valueTimeTimeZone.timeZoneOffsetSeconds))
    }

    /**
     * Converts a value to a Period.
     *
     * This method should only be called from Java 8 or later version.
     *
     * @param value the value to convert
     * @return the Period
     */
    fun valueToPeriod(value: Value): Any? {
        var value = value
        if (value !is ValueInterval) {
            value = value.convertTo(TypeInfo.TYPE_INTERVAL_YEAR_TO_MONTH)!!
        }
        if (!DataType.isYearMonthIntervalType(value.getValueType())) {
            throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, (null as Throwable?)!!, value.getString())
        }
        val v = value as ValueInterval
        val qualifier = v.getQualifier()
        val negative = v.isNegative()
        val leading: Long = v.getLeading()
        val remaining: Long = v.getRemaining()
        val y = Value.convertToInt(yearsFromInterval(qualifier, negative, leading, remaining), null)
        val m = Value.convertToInt(monthsFromInterval(qualifier, negative, leading, remaining), null)
        return Period.of(y, m, 0)
    }

    private fun localDateTimeFromDateNanos(dateValue: Long, timeNanos: Long): Any? {
        var dateValue = dateValue
        var timeNanos = timeNanos
        if (dateValue > MAX_DATE_VALUE) {
            dateValue = MAX_DATE_VALUE
            timeNanos = DateTimeUtils.NANOS_PER_DAY - 1
        } else if (dateValue < MIN_DATE_VALUE) {
            dateValue = MIN_DATE_VALUE
            timeNanos = 0
        }
        return LocalDateTime.of(LocalDate.of(yearFromDateValue(dateValue),
                monthFromDateValue(dateValue), dayFromDateValue(dateValue)),
                LocalTime.ofNanoOfDay(timeNanos))
    }
}