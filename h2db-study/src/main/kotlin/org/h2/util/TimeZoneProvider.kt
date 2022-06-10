package org.h2.util

import org.h2.util.DateTimeUtils.dayFromDateValue
import org.h2.util.DateTimeUtils.getEpochSeconds
import org.h2.util.DateTimeUtils.monthFromDateValue
import org.h2.util.DateTimeUtils.yearFromDateValue
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Provides access to time zone API.
 */
abstract class TimeZoneProvider {
    /**
     * Calculates the time zone offset in seconds for the specified EPOCH seconds.
     *
     * @param epochSeconds seconds since EPOCH
     * @return time zone offset in minutes
     */
    abstract fun getTimeZoneOffsetUTC(epochSeconds: Long): Int

    /**
     * Calculates the time zone offset in seconds for the specified date value
     * and nanoseconds since midnight in local time.
     *
     * @param dateValue date value
     * @param timeNanos nanoseconds since midnight
     * @return time zone offset in minutes
     */
    abstract fun getTimeZoneOffsetLocal(dateValue: Long, timeNanos: Long): Int

    /**
     * Calculates the epoch seconds from local date and time.
     *
     * @param dateValue date value
     * @param timeNanos nanoseconds since midnight
     * @return the epoch seconds value
     */
    abstract fun getEpochSecondsFromLocal(dateValue: Long, timeNanos: Long): Long

    /**
     * Returns the ID of the time zone.
     * @return the ID of the time zone
     */
    abstract fun getId(): String?

    /**
     * Get the standard time name or daylight saving time name of the time zone.
     *
     * @param epochSeconds
     * seconds since EPOCH
     * @return the standard time name or daylight saving time name of the time
     * zone
     */
    abstract fun getShortId(epochSeconds: Long): String?

    /**
     * Returns whether this is a simple time zone provider with a fixed offset
     * from UTC.
     *
     * @return whether this is a simple time zone provider with a fixed offset
     * from UTC
     */
    open fun hasFixedOffset(): Boolean = false

    companion object {
        /**
         * The UTC time zone provider.
         */
        val UTC: TimeZoneProvider = Simple(0)

        /**
         * A small cache for timezone providers.
         */
        lateinit var CACHE: Array<TimeZoneProvider?>

        /**
         * The number of cache elements (needs to be a power of 2).
         */
        private const val CACHE_SIZE = 32

        /**
         * Returns the time zone provider with the specified offset.
         *
         * @param offset UTC offset in seconds
         * @return the time zone provider with the specified offset
         */
        fun ofOffset(offset: Int): TimeZoneProvider {
            if (offset == 0) return UTC
            require(!(offset < -18 * 60 * 60 || offset > 18 * 60 * 60)) { "Time zone offset $offset seconds is out of range" }
            return Simple(offset)
        }

        /**
         * Returns the time zone provider with the specified name.
         *
         * @param id  the ID of the time zone
         * @return the time zone provider with the specified name
         * @throws RuntimeException if time zone with specified ID isn't known
         */
        @Throws(RuntimeException::class)
        open fun ofId(id: String): TimeZoneProvider {
            val length = id.length
            if (length == 1 && id[0] == 'Z') return UTC

            var index = 0
            if (id.startsWith("GMT") || id.startsWith("UTC")) {
                if (length == 3) return UTC
                index = 3
            }
            if (length > index) {
                var negative = id[index] == '-'
                var c = if (length > index + 1) id[++index] else id[index]
                if (index != 3 && c >= '0' && c <= '9') {
                    var hour = c - '0'
                    if (++index < length) {
                        c = id[index]
                        if (c >= '0' && c <= '9') {
                            hour = hour * 10 + c.code - '0'.code
                            index++
                        }
                    }
                    if (index == length) {
                        val offset = hour * 3600
                        return TimeZoneProvider.ofOffset(if (negative) -offset else offset)
                    }
                    if (id[index] == ':') {
                        if (++index < length) {
                            c = id[index]
                            if (c >= '0' && c <= '9') {
                                var minute = c - '0'
                                if (++index < length) {
                                    c = id[index]
                                    if (c >= '0' && c <= '9') {
                                        minute = minute * 10 + c.code - '0'.code
                                        index++
                                    }
                                }
                                if (index == length) {
                                    val offset = (hour * 60 + minute) * 60
                                    return TimeZoneProvider.ofOffset(if (negative) -offset else offset)
                                }
                                if (id[index] == ':') {
                                    if (++index < length) {
                                        c = id[index]
                                        if (c >= '0' && c <= '9') {
                                            var second = c - '0'
                                            if (++index < length) {
                                                c = id[index]
                                                if (c >= '0' && c <= '9') {
                                                    second = second * 10 + c.code - '0'.code
                                                    index++
                                                }
                                            }
                                            if (index == length) {
                                                val offset = (hour * 60 + minute) * 60 + second
                                                return TimeZoneProvider.ofOffset(if (negative) -offset else offset)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                require(index <= 0) { id }
            }

            val hash = id.hashCode() and CACHE_SIZE - 1
            var cache: Array<TimeZoneProvider?> = CACHE
            if (cache != null) {
                val provider = cache[hash]
                if (provider?.getId() == id) {
                    return provider
                }
            }
            val provider: TimeZoneProvider = WithTimeZone(ZoneId.of(id, ZoneId.SHORT_IDS))
            if (cache == null) {
                cache = arrayOfNulls(CACHE_SIZE)
                CACHE = cache
            }
            cache[hash] = provider
            return provider
        }

        /**
         * Time zone provider with offset.
         */
        private class Simple internal constructor(private val offset: Int) : TimeZoneProvider() {
            @Volatile
            private var id: String? = null

            override fun getId(): String? {
                if (id == null) id = DateTimeUtils.timeZoneNameFromOffsetSeconds(offset)
                return id
            }

            override fun hashCode(): Int = offset + 129607

            override fun equals(obj: Any?): Boolean = (this === obj) || (obj is Simple && obj.offset == offset)

            override fun getTimeZoneOffsetUTC(epochSeconds: Long): Int = offset

            override fun getTimeZoneOffsetLocal(dateValue: Long, timeNanos: Long): Int = offset

            override fun getEpochSecondsFromLocal(dateValue: Long, timeNanos: Long): Long = getEpochSeconds(dateValue, timeNanos, offset)

            override fun getShortId(epochSeconds: Long): String? = id

            override fun hasFixedOffset(): Boolean = true

            override fun toString(): String = "TimeZoneProvider $id"
        }

        /**
         * Time zone provider with time zone.
         */
        internal class WithTimeZone(private val zoneId: ZoneId) : TimeZoneProvider() {
            override fun hashCode(): Int = zoneId.hashCode() + 951689

            override fun equals(obj: Any?): Boolean = (this === obj) || (obj is WithTimeZone && obj.zoneId == zoneId)

            override fun getTimeZoneOffsetUTC(epochSeconds: Long): Int {
                /*
             * Construct an Instant with EPOCH seconds within the range
             * -31,557,014,135,532,000..31,556,889,832,715,999
             * (-999999999-01-01T00:00-18:00..
             * +999999999-12-31T23:59:59.999999999+18:00). Too large and too
             * small EPOCH seconds are replaced with EPOCH seconds within the
             * range using the 400 years period of the Gregorian calendar.
             *
             * H2 has slightly wider range of EPOCH seconds than Instant, and
             * ZoneRules.getOffset(Instant) does not support all Instant values
             * in all time zones.
             */
                return zoneId.rules.getOffset(
                    Instant.ofEpochSecond(
                        epochSeconds + when {
                            epochSeconds > 31556889832715999L -> -SECONDS_PER_PERIOD
                            epochSeconds < -31557014135532000L -> SECONDS_PER_PERIOD
                            else -> 0
                        }
                    )
                ).totalSeconds
            }

            override fun getTimeZoneOffsetLocal(dateValue: Long, timeNanos: Long): Int {
                var second = (timeNanos / DateTimeUtils.NANOS_PER_SECOND).toInt()
                var minute = second / 60
                second -= minute * 60
                val hour = minute / 60
                minute -= hour * 60
                return ZonedDateTime.of(
                    LocalDateTime.of(
                        yearForCalendar(yearFromDateValue(dateValue)),
                        monthFromDateValue(dateValue), dayFromDateValue(dateValue), hour,
                        minute, second
                    ), zoneId
                ).offset.totalSeconds
            }

            override fun getEpochSecondsFromLocal(dateValue: Long, timeNanos: Long): Long {
                var second = (timeNanos / DateTimeUtils.NANOS_PER_SECOND).toInt()
                var minute = second / 60
                second -= minute * 60
                val hour = minute / 60
                minute -= hour * 60
                val year = yearFromDateValue(dateValue)
                val yearForCalendar = yearForCalendar(year)
                val epoch = ZonedDateTime.of(
                    LocalDateTime.of(
                        yearForCalendar, monthFromDateValue(dateValue),
                        dayFromDateValue(dateValue), hour, minute, second
                    ), zoneId
                ).toOffsetDateTime().toEpochSecond()
                return epoch + (year - yearForCalendar) * SECONDS_PER_YEAR
            }

            override fun getId(): String = zoneId.id

            override fun getShortId(epochSeconds: Long): String = ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), zoneId).format(TIME_ZONE_FORMATTER)

            override fun toString(): String = "TimeZoneProvider " + zoneId.id

            companion object {
                /**
                 * Number of seconds in 400 years.
                 */
                const val SECONDS_PER_PERIOD = 146097L * 60 * 60 * 24

                /**
                 * Number of seconds per year.
                 */
                const val SECONDS_PER_YEAR = SECONDS_PER_PERIOD / 400

                //                @Volatile
                val TIME_ZONE_FORMATTER: DateTimeFormatter by lazy { DateTimeFormatter.ofPattern("z", Locale.ENGLISH) }

                /**
                 * Returns a year within the range -999,999,999..999,999,999 for the
                 * given year. Too large and too small years are replaced with years
                 * within the range using the 400 years period of the Gregorian
                 * calendar.
                 *
                 * Because we need them only to calculate a time zone offset, it's safe
                 * to normalize them to such range.
                 *
                 * @param year the year
                 * @return the specified year or the replacement year within the range
                 */
                private fun yearForCalendar(year: Int): Int = year + (if (year > 999999999) -400 else if (year < -999999999) 400 else 0)
            }
        }
    }
}

