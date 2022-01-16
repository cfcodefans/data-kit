package org.h2.value

import org.h2.api.ErrorCode
import org.h2.engine.CastDataProvider
import org.h2.message.DbException
import org.h2.value.ValueTimeTimeZone.Companion.getLocalTimeNanos
import org.h2.util.DateTimeUtils as DTU

/**
 * Implementation of the TIME data type.
 */
class ValueTime(
        /**
         * Nanoseconds since midnight
         */
        val nanos: Long = 0) : Value() {

    companion object {
        /**
         * The default precision and display size of the textual representation of a time.
         * Example: 10:00:00
         */
        const val DEFAULT_PRECISION = 8

        /**
         * The maximum precision and display size of the textual representation of a time.
         * Example: 10:00:00.123456789
         */
        const val MAXIMUM_PRECISION = 18

        /**
         * The default scale for time.
         */
        const val DEFAULT_SCALE = 0

        /**
         * The maximum scale for time.
         */
        const val MAXIMUM_SCALE = 9

        /**
         * Get or create a time value.
         *
         * @param nanos the nanoseconds since midnight
         * @return the value
         */
        fun fromNanos(nanos: Long): ValueTime {
            if (nanos < 0L || nanos >= DTU.NANOS_PER_DAY) {
                throw DbException.get(ErrorCode.INVALID_DATETIME_CONSTANT_2, "TIME",
                        DTU.appendTime(StringBuilder(), nanos).toString())
            }
            return cache(ValueTime(nanos)) as ValueTime
        }

        /**
         * Parse a string to a ValueTime.
         *
         * @param s the string to parse
         * @return the time
         */
        fun parse(s: String): ValueTime = try {
            fromNanos(DTU.parseTimeNanos(s, 0, s.length))
        } catch (e: Exception) {
            throw DbException.get(ErrorCode.INVALID_DATETIME_CONSTANT_2, e, "TIME", s)
        }

        fun Value.convertToTime(targetType: TypeInfo, provider: CastDataProvider, conversionMode: Int): ValueTime {
            var v: ValueTime = when (getValueType()) {
                TIME -> this as ValueTime
                TIME_TZ -> ValueTime.fromNanos(getLocalTimeNanos(provider))
                TIMESTAMP -> ValueTime.fromNanos((this as ValueTimestamp).timeNanos)
                TIMESTAMP_TZ -> {
                    val ts = this as ValueTimestampTimeZone
                    val timeNanos = ts.timeNanos
                    val epochSeconds = org.h2.util.DateTimeUtils.getEpochSeconds(ts.dateValue, timeNanos, ts.timeZoneOffsetSeconds)

                    fromNanos(DTU.nanosFromLocalSeconds(epochSeconds
                            + provider.currentTimeZone().getTimeZoneOffsetUTC(epochSeconds))
                            + timeNanos % org.h2.util.DateTimeUtils.NANOS_PER_SECOND)
                }
                VARCHAR, VARCHAR_IGNORECASE, CHAR -> ValueTime.parse(getString()!!.trim { it <= ' ' })
                else -> throw getDataConversionError(TIME)
            }

            if (conversionMode == CONVERT_TO) return v

            val targetScale: Int = targetType.scale
            if (targetScale < ValueTime.MAXIMUM_SCALE) {
                val n = v.nanos
                val n2: Long = DTU.convertScale(n, targetScale, DTU.NANOS_PER_DAY)
                if (n2 != n) {
                    v = ValueTime.fromNanos(n2)
                }
            }
            return v
        }
    }

    override val type: TypeInfo = TypeInfo.TYPE_TIME

    override fun getValueType(): Int = TIME

    override fun getString(): String? = DTU.appendTime(StringBuilder(MAXIMUM_PRECISION), nanos).toString()

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder =
            DTU.appendTime(builder.append("TIME '"), nanos).append('\'')

    override fun compareTypeSafe(o: Value, mode: CompareMode?, provider: CastDataProvider?): Int =
            nanos.compareTo((o as ValueTime).nanos)

    override fun equals(other: Any?): Boolean = this === other || other is ValueTime && nanos == other.nanos

    override fun hashCode(): Int = (nanos xor (nanos ushr 32)).toInt()

    override fun add(v: Value): Value {
        val t = v as ValueTime
        return fromNanos(nanos + t.nanos)
    }

    override fun subtract(v: Value): Value {
        val t = v as ValueTime
        return fromNanos(nanos - t.nanos)
    }

    override fun multiply(v: Value): Value = fromNanos((nanos * v.getDouble()).toLong())

    override fun divide(v: Value, quotientType: TypeInfo?): Value = fromNanos((nanos / v.getDouble()).toLong())

}