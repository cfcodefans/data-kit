package org.h2.util

import org.h2.util.DateTimeUtils.getEpochSeconds

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
        var CACHE: Array<TimeZoneProvider>

        /**
         * The number of cache elements (needs to be a power of 2).
         */
        private const val CACHE_SIZE = 32

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