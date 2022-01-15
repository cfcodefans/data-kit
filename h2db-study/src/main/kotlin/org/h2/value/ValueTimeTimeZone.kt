package org.h2.value

import org.h2.api.ErrorCode
import org.h2.message.DbException
import org.h2.util.DateTimeUtils

/**
 * Implementation of the TIME WITH TIME ZONE data type.
 */
open class ValueTimeTimeZone(
        /**
         * Nanoseconds since midnight
         */
        val nanos: Long,
        /**
         * Time zone offset from UTC in seconds, range of -18 hours to +18 hours.
         * This range is compatible with OffsetTime from JSR-310.
         */
        val timeZoneOffsetSeconds: Int) : Value() {

    companion object {
        /**
         * The default precision and display size of the textual representation of a
         * time. Example: 10:00:00+10:00
         */
        const val DEFAULT_PRECISION = 14

        /**
         * The maximum precision and display size of the textual representation of a
         * time. Example: 10:00:00.123456789+10:00
         */
        const val MAXIMUM_PRECISION = 24

        /**
         * Get or create a time value.
         *
         * @param nanos the nanoseconds since midnight
         * @param timeZoneOffsetSeconds the timezone offset in seconds
         * @return the value
         */
        fun fromNanos(nanos: Long, timeZoneOffsetSeconds: Int): ValueTimeTimeZone? {
            if (nanos < 0L || nanos >= DateTimeUtils.NANOS_PER_DAY) {
                throw DbException.get(ErrorCode.INVALID_DATETIME_CONSTANT_2, "TIME WITH TIME ZONE",
                        DateTimeUtils.appendTime(StringBuilder(), nanos).toString())
            }
            /*
         * Some current and historic time zones have offsets larger than 12
         * hours. JSR-310 determines 18 hours as maximum possible offset in both
         * directions, so we use this limit too for compatibility.
         */require(!(timeZoneOffsetSeconds < -18 * 60 * 60 || timeZoneOffsetSeconds > 18 * 60 * 60)) { "timeZoneOffsetSeconds $timeZoneOffsetSeconds" }
            return cache(ValueTimeTimeZone(nanos, timeZoneOffsetSeconds)) as ValueTimeTimeZone
        }

        /**
         * Parse a string to a ValueTime.
         *
         * @param s the string to parse
         * @return the time
         */
        fun parse(s: String): ValueTimeTimeZone? = try {
            DateTimeUtils.parseTimeWithTimeZone(s, null)
        } catch (e: Exception) {
            throw DbException.get(ErrorCode.INVALID_DATETIME_CONSTANT_2, e, "TIME WITH TIME ZONE", s)
        }
    }

    override val type: TypeInfo = TypeInfo.TYPE_TIME_TZ

    override fun getValueType(): Int = TIME_TZ

    override fun getMemory(): Int = 32
    override fun getString(): String? = toString(StringBuilder(MAXIMUM_PRECISION)).toString()
    private fun toString(builder: StringBuilder): StringBuilder {
        return DateTimeUtils.appendTimeZone(DateTimeUtils.appendTime(builder, nanos), timeZoneOffsetSeconds)
    }
}