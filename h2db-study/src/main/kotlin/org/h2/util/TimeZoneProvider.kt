package org.h2.util

import org.h2.util.DateTimeUtils.getEpochSeconds
import java.time.ZoneId

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
    open fun hasFixedOffset(): Boolean {
        return false
    }

    companion object {
        /**
         * The UTC time zone provider.
         */
        val UTC: TimeZoneProvider = TimeZoneProvider.Simple(0.toShort())

        /**
         * A small cache for timezone providers.
         */
        lateinit var CACHE: Array<TimeZoneProvider?>

        /**
         * The number of cache elements (needs to be a power of 2).
         */
        private const val CACHE_SIZE = 32

        /**
         * Returns the time zone provider with the specified name.
         *
         * @param id  the ID of the time zone
         * @return the time zone provider with the specified name
         * @throws RuntimeException if time zone with specified ID isn't known
         */
        @Throws(RuntimeException::class)
        open fun ofId(id: String): TimeZoneProvider? {
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
                if (provider != null && provider.getId() == id) {
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
            var id: String? = null
                get() {
                    var id = field
                    if (id == null) {
                        id = DateTimeUtils.timeZoneNameFromOffsetSeconds(offset)
                        field = id
                    }
                    return id
                }
                private set

            override fun hashCode(): Int {
                return offset + 129607
            }

            override fun equals(obj: Any?): Boolean {
                if (this === obj) {
                    return true
                }
                return if (obj == null || obj.javaClass != Simple::class.java) {
                    false
                } else offset == (obj as Simple).offset
            }

            override fun getTimeZoneOffsetUTC(epochSeconds: Long): Int {
                return offset
            }

            override fun getTimeZoneOffsetLocal(dateValue: Long, timeNanos: Long): Int {
                return offset
            }

            override fun getEpochSecondsFromLocal(dateValue: Long, timeNanos: Long): Long {
                return getEpochSeconds(dateValue, timeNanos, offset)
            }

            override fun getShortId(epochSeconds: Long): String? {
                return id
            }

            override fun hasFixedOffset(): Boolean {
                return true
            }

            override fun toString(): String {
                return "TimeZoneProvider $id"
            }
        }
    }
}