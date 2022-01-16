package org.h2.value

import org.h2.api.ErrorCode
import org.h2.engine.CastDataProvider
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
        fun fromNanos(nanos: Long, timeZoneOffsetSeconds: Int): ValueTimeTimeZone {
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
        fun parse(s: String): ValueTimeTimeZone = try {
            DateTimeUtils.parseTimeWithTimeZone(s, null)
        } catch (e: Exception) {
            throw DbException.get(ErrorCode.INVALID_DATETIME_CONSTANT_2, e, "TIME WITH TIME ZONE", s)
        }

        fun Value.convertToTimeTimeZone(targetType: TypeInfo, provider: CastDataProvider, conversionMode: Int): ValueTimeTimeZone {
            var v: ValueTimeTimeZone = when (getValueType()) {
                TIME_TZ -> this as ValueTimeTimeZone
                TIME -> fromNanos((this as ValueTime).nanos, provider.currentTimestamp().timeZoneOffsetSeconds)
                TIMESTAMP -> {
                    val ts = this as ValueTimestamp
                    val timeNanos = ts.timeNanos
                    fromNanos(timeNanos, provider.currentTimeZone().getTimeZoneOffsetLocal(ts.dateValue, timeNanos))
                }
                TIMESTAMP_TZ -> {
                    val ts = this as ValueTimestampTimeZone
                    fromNanos(ts.timeNanos, ts.timeZoneOffsetSeconds)
                }
                VARCHAR, VARCHAR_IGNORECASE, CHAR -> parse(getString()!!.trim { it <= ' ' })
                else -> throw getDataConversionError(TIME_TZ)
            }

            if (conversionMode == CONVERT_TO) return v

            val targetScale: Int = targetType.scale
            if (targetScale < ValueTime.MAXIMUM_SCALE) {
                val n: Long = v.nanos
                val n2: Long = DateTimeUtils.convertScale(n, targetScale, DateTimeUtils.NANOS_PER_DAY)
                if (n2 != n) {
                    v = fromNanos(n2, v.timeZoneOffsetSeconds)
                }
            }
            return v
        }

        fun Value.getLocalTimeNanos(provider: CastDataProvider): Long {
            val ts = this as ValueTimeTimeZone
            val localOffset = provider.currentTimestamp().timeZoneOffsetSeconds
            return DateTimeUtils.normalizeNanosOfDay(ts.nanos + (ts.timeZoneOffsetSeconds - localOffset) * DateTimeUtils.NANOS_PER_DAY)
        }
    }

    override val type: TypeInfo = TypeInfo.TYPE_TIME_TZ

    override fun getValueType(): Int = TIME_TZ

    override fun getMemory(): Int = 32
    override fun getString(): String? = toString(StringBuilder(MAXIMUM_PRECISION)).toString()
    private fun toString(builder: StringBuilder): StringBuilder {
        return DateTimeUtils.appendTimeZone(DateTimeUtils.appendTime(builder, nanos), timeZoneOffsetSeconds)
    }

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder {
        return toString(builder.append("TIME WITH TIME ZONE '")).append('\'')
    }

    override fun compareTypeSafe(o: Value, mode: CompareMode?, provider: CastDataProvider?): Int {
        val t = o as ValueTimeTimeZone
        return (nanos - timeZoneOffsetSeconds * DateTimeUtils.NANOS_PER_SECOND).compareTo(t.nanos - t.timeZoneOffsetSeconds * DateTimeUtils.NANOS_PER_SECOND)
    }

    override fun equals(other: Any?): Boolean = (this == other)
            || (other is ValueTimeTimeZone
            && nanos == other.nanos
            && timeZoneOffsetSeconds == other.timeZoneOffsetSeconds)

    override fun hashCode(): Int = (nanos xor (nanos ushr 32) xor timeZoneOffsetSeconds.toLong()).toInt()

}