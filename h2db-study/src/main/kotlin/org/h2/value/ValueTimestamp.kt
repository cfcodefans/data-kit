package org.h2.value

import org.h2.api.ErrorCode
import org.h2.engine.CastDataProvider
import org.h2.message.DbException
import org.h2.util.DateTimeUtils
import org.h2.value.ValueTimeTimeZone.Companion.getLocalTimeNanos

/**
 * Implementation of the TIMESTAMP data type.
 */
class ValueTimestamp(val dateValue: Long, val timeNanos: Long) : Value() {

    init {
        require(!(dateValue < DateTimeUtils.MIN_DATE_VALUE
                || dateValue > DateTimeUtils.MAX_DATE_VALUE))
        { "dateValue out of range $dateValue" }
        require(!(timeNanos < 0
                || timeNanos >= DateTimeUtils.NANOS_PER_DAY))
        { "timeNanos out of range $timeNanos" }
    }

    companion object {
        /**
         * The default precision and display size of the textual representation of a timestamp.
         * Example: 2001-01-01 23:59:59.123456
         */
        const val DEFAULT_PRECISION = 26

        /**
         * The maximum precision and display size of the textual representation of a timestamp.
         * Example: 2001-01-01 23:59:59.123456789
         */
        const val MAXIMUM_PRECISION = 29

        /**
         * The default scale for timestamps.
         */
        const val DEFAULT_SCALE = 6

        /**
         * The maximum scale for timestamps.
         */
        const val MAXIMUM_SCALE = 9

        /**
         * Get or create a date value for the given date.
         *
         * @param dateValue the date value, a bit field with bits for the year,
         * month, and day
         * @param timeNanos the nanoseconds since midnight
         * @return the value
         */
        fun fromDateValueAndNanos(dateValue: Long, timeNanos: Long): ValueTimestamp {
            return cache(ValueTimestamp(dateValue, timeNanos)) as ValueTimestamp
        }

        /**
         * Parse a string to a ValueTimestamp, using the given [CastDataProvider].
         * This method supports the format +/-year-month-day[ -]hour[:.]minute[:.]seconds.fractional
         * and an optional timezone part.
         *
         * @param s the string to parse
         * @param provider
         * the cast information provider, may be `null` for
         * literals without time zone
         * @return the date
         */
        fun parse(s: String, provider: CastDataProvider?): ValueTimestamp {
            return try {
                DateTimeUtils.parseTimestamp(s, provider, false) as ValueTimestamp
            } catch (e: Exception) {
                throw DbException.get(ErrorCode.INVALID_DATETIME_CONSTANT_2, e, "TIMESTAMP", s)
            }
        }

        fun Value.convertToTimestamp(targetType: TypeInfo, provider: CastDataProvider, conversionMode: Int): ValueTimestamp {
            var v: ValueTimestamp
            when (getValueType()) {
                TIMESTAMP -> v = this as ValueTimestamp
                TIME -> v = ValueTimestamp.fromDateValueAndNanos(provider.currentTimestamp().dateValue, (this as ValueTime).nanos)
                TIME_TZ -> v = ValueTimestamp.fromDateValueAndNanos(provider.currentTimestamp().dateValue, getLocalTimeNanos(provider))
                DATE ->             // Scale is always 0
                    return ValueTimestamp.fromDateValueAndNanos((this as ValueDate).dateValue, 0)

                TIMESTAMP_TZ -> {
                    val ts = this as ValueTimestampTimeZone
                    val timeNanos = ts.timeNanos
                    var epochSeconds = DateTimeUtils.getEpochSeconds(ts.dateValue, timeNanos, ts.timeZoneOffsetSeconds)
                    epochSeconds += provider.currentTimeZone().getTimeZoneOffsetUTC(epochSeconds).toLong()
                    v = ValueTimestamp.fromDateValueAndNanos(DateTimeUtils.dateValueFromLocalSeconds(epochSeconds),
                        DateTimeUtils.nanosFromLocalSeconds(epochSeconds) + timeNanos % DateTimeUtils.NANOS_PER_SECOND)
                }

                VARCHAR, VARCHAR_IGNORECASE, CHAR -> v = parse(getString()!!.trim { it <= ' ' }, provider)
                else -> throw getDataConversionError(TIMESTAMP)
            }

            if (conversionMode != CONVERT_TO) {
                val targetScale: Int = targetType.scale
                if (targetScale < MAXIMUM_SCALE) {
                    var dv = v.dateValue
                    val n = v.timeNanos
                    var n2 = DateTimeUtils.convertScale(n, targetScale,
                        if (dv == DateTimeUtils.MAX_DATE_VALUE) DateTimeUtils.NANOS_PER_DAY else Long.MAX_VALUE)
                    if (n2 != n) {
                        if (n2 >= DateTimeUtils.NANOS_PER_DAY) {
                            n2 -= DateTimeUtils.NANOS_PER_DAY
                            dv = DateTimeUtils.incrementDateValue(dv)
                        }
                        v = fromDateValueAndNanos(dv, n2)
                    }
                }
            }
            return v
        }
    }

    override var type: TypeInfo? = TypeInfo.TYPE_TIMESTAMP

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder = toString(builder.append("TIMESTAMP '"), false).append('\'')

    override fun getValueType(): Int = TIMESTAMP

    override fun getMemory(): Int = 32

    private fun toString(builder: StringBuilder, iso: Boolean): StringBuilder {
        DateTimeUtils.appendDate(builder, dateValue).append(if (iso) 'T' else ' ')
        return DateTimeUtils.appendTime(builder, timeNanos)
    }

    override fun getString(): String = toString(StringBuilder(MAXIMUM_PRECISION), false).toString()

    override fun compareTypeSafe(o: Value, mode: CompareMode?, provider: CastDataProvider?): Int {
        val t = o as ValueTimestamp
        val c = dateValue.compareTo(t.dateValue)
        return if (c != 0) c else timeNanos.compareTo(t.timeNanos)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return (this === other)
                || (other is ValueTimestamp
                && dateValue == other.dateValue
                && timeNanos == other.timeNanos)
    }

    override fun hashCode(): Int = (dateValue xor (dateValue ushr 32) xor timeNanos xor (timeNanos ushr 32)).toInt()

    override fun add(v: Value): Value {
        val t = v as ValueTimestamp
        var absoluteDay = (DateTimeUtils.absoluteDayFromDateValue(dateValue) + DateTimeUtils.absoluteDayFromDateValue(t.dateValue))
        var nanos = timeNanos + t.timeNanos
        if (nanos >= DateTimeUtils.NANOS_PER_DAY) {
            nanos -= DateTimeUtils.NANOS_PER_DAY
            absoluteDay++
        }
        return fromDateValueAndNanos(DateTimeUtils.dateValueFromAbsoluteDay(absoluteDay), nanos)
    }

    override fun subtract(v: Value): Value {
        val t = v as ValueTimestamp
        var absoluteDay = (DateTimeUtils.absoluteDayFromDateValue(dateValue) - DateTimeUtils.absoluteDayFromDateValue(t.dateValue))
        var nanos = timeNanos - t.timeNanos
        if (nanos < 0) {
            nanos += DateTimeUtils.NANOS_PER_DAY
            absoluteDay--
        }
        return fromDateValueAndNanos(DateTimeUtils.dateValueFromAbsoluteDay(absoluteDay), nanos)
    }

    /**
     * Returns value as string in ISO format.
     *
     * @return value as string in ISO format
     */
    fun getISOString(): String = toString(StringBuilder(ValueTimestampTimeZone.MAXIMUM_PRECISION), true).toString()
}