package org.h2.value

import org.h2.api.ErrorCode
import org.h2.engine.CastDataProvider
import org.h2.message.DbException
import org.h2.util.DateTimeUtils


/**
 * Implementation of the TIMESTAMP WITH TIME ZONE data type.
 */
class ValueTimestampTimeZone(
        /**
         * A bit field with bits for the year, month, and day (see DateTimeUtils for
         * encoding)
         */
        val dateValue: Long = 0,
        /**
         * The nanoseconds since midnight.
         */
        val timeNanos: Long = 0,
        /**
         * Time zone offset from UTC in seconds, range of -18 hours to +18 hours. This
         * range is compatible with OffsetDateTime from JSR-310.
         */
        val timeZoneOffsetSeconds: Int = 0) : Value() {

    init {
        require(dateValue >= DateTimeUtils.MIN_DATE_VALUE && dateValue <= DateTimeUtils.MAX_DATE_VALUE) { "dateValue out of range $dateValue" }
        require(timeNanos >= 0 && timeNanos < DateTimeUtils.NANOS_PER_DAY) { "timeNanos out of range $timeNanos" }
        /*
         * Some current and historic time zones have offsets larger than 12 hours.
         * JSR-310 determines 18 hours as maximum possible offset in both directions, so
         * we use this limit too for compatibility.
         */
        /*
         * Some current and historic time zones have offsets larger than 12 hours.
         * JSR-310 determines 18 hours as maximum possible offset in both directions, so
         * we use this limit too for compatibility.
         */require(timeZoneOffsetSeconds >= -SEC_18_HOURS && timeZoneOffsetSeconds <= SEC_18_HOURS) { "timeZoneOffsetSeconds out of range $timeZoneOffsetSeconds" }
    }

    companion object {
        const val SEC_18_HOURS: Int = 18 * 60 * 60

        /**
         * The default precision and display size of the textual representation of a timestamp.
         * Example: 2001-01-01 23:59:59.123456+10:00
         */
        const val DEFAULT_PRECISION = 32

        /**
         * The maximum precision and display size of the textual representation of a timestamp.
         * Example: 2001-01-01 23:59:59.123456789+10:00
         */
        const val MAXIMUM_PRECISION = 35

        /**
         * Get or create a date value for the given date.
         *
         * @param dateValue the date value, a bit field with bits for the year,
         * month, and day
         * @param timeNanos the nanoseconds since midnight
         * @param timeZoneOffsetSeconds the timezone offset in seconds
         * @return the value
         */
        fun fromDateValueAndNanos(dateValue: Long, timeNanos: Long, timeZoneOffsetSeconds: Int): ValueTimestampTimeZone =
                cache(ValueTimestampTimeZone(dateValue, timeNanos, timeZoneOffsetSeconds)) as ValueTimestampTimeZone

        /**
         * Parse a string to a ValueTimestamp. This method supports the format
         * +/-year-month-day hour:minute:seconds.fractional and an optional timezone
         * part.
         *
         * @param s the string to parse
         * @param provider
         * the cast information provider, may be `null` for
         * literals with time zone
         * @return the date
         */
        fun parse(s: String?, provider: CastDataProvider?): ValueTimestampTimeZone = try {
            DateTimeUtils.parseTimestamp(s!!, provider, true) as ValueTimestampTimeZone
        } catch (e: Exception) {
            throw DbException.get(ErrorCode.INVALID_DATETIME_CONSTANT_2, e, "TIMESTAMP WITH TIME ZONE", s)
        }

        fun Value.convertToTimestampTimeZone(targetType: TypeInfo, provider: CastDataProvider,
                                             conversionMode: Int): ValueTimestampTimeZone? {
            var v: ValueTimestampTimeZone = when (getValueType()) {
                TIMESTAMP_TZ -> this as ValueTimestampTimeZone
                TIME -> {
                    val dateValue = provider.currentTimestamp().dateValue
                    val timeNanos = (this as ValueTime).nanos
                    fromDateValueAndNanos(dateValue, timeNanos, provider.currentTimeZone().getTimeZoneOffsetLocal(dateValue, timeNanos))
                }
                TIME_TZ -> {
                    val t = this as ValueTimeTimeZone
                    fromDateValueAndNanos(provider.currentTimestamp().dateValue, t.nanos, t.timeZoneOffsetSeconds)
                }
                DATE -> {
                    val dateValue = (this as ValueDate).dateValue
                    // Scale is always 0
                    return fromDateValueAndNanos(dateValue, 0L, provider.currentTimeZone().getTimeZoneOffsetLocal(dateValue, 0L))
                }
                TIMESTAMP -> {
                    val ts = this as ValueTimestamp
                    val dateValue: Long = ts.dateValue
                    val timeNanos: Long = ts.timeNanos
                    fromDateValueAndNanos(dateValue, timeNanos, provider.currentTimeZone().getTimeZoneOffsetLocal(dateValue, timeNanos))
                }
                VARCHAR, VARCHAR_IGNORECASE, CHAR -> parse(getString()!!.trim { it <= ' ' }, provider)
                else -> throw getDataConversionError(TIMESTAMP_TZ)
            }

            if (conversionMode != CONVERT_TO) {
                val targetScale: Int = targetType.scale
                if (targetScale < ValueTimestamp.MAXIMUM_SCALE) {
                    var dv: Long = v.dateValue
                    val n: Long = v.timeNanos
                    var n2 = DateTimeUtils.convertScale(n,
                            targetScale,
                            if (dv == DateTimeUtils.MAX_DATE_VALUE) DateTimeUtils.NANOS_PER_DAY else Long.MAX_VALUE)
                    if (n2 != n) {
                        if (n2 >= DateTimeUtils.NANOS_PER_DAY) {
                            n2 -= DateTimeUtils.NANOS_PER_DAY
                            dv = DateTimeUtils.incrementDateValue(dv)
                        }
                        v = fromDateValueAndNanos(dv, n2, v.timeZoneOffsetSeconds)
                    }
                }
            }
            return v
        }
    }

    override val type: TypeInfo = TypeInfo.TYPE_TIMESTAMP_TZ

    override fun getValueType(): Int = TIMESTAMP_TZ

    override fun getMemory(): Int = 40// Java 11 with -XX:-UseCompressedOops

    private fun toString(builder: StringBuilder, iso: Boolean): StringBuilder {
        DateTimeUtils.appendDate(builder, dateValue).append(if (iso) 'T' else ' ')
        DateTimeUtils.appendTime(builder, timeNanos)
        return DateTimeUtils.appendTimeZone(builder, timeZoneOffsetSeconds)
    }

    override fun getString(): String = toString(StringBuilder(MAXIMUM_PRECISION), false).toString()

    /**
     * Returns value as string in ISO format.
     *
     * @return value as string in ISO format
     */
    fun getISOString(): String = toString(StringBuilder(MAXIMUM_PRECISION), true).toString()

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder =
            toString(builder.append("TIMESTAMP WITH TIME ZONE '"), false).append('\'')

    override fun compareTypeSafe(o: Value, mode: CompareMode?, provider: CastDataProvider?): Int {
        val t = o as ValueTimestampTimeZone
        // Maximum time zone offset is +/-18 hours so difference in days between local
        // and UTC cannot be more than one day
        var dateValueA = dateValue
        var timeA = timeNanos - timeZoneOffsetSeconds * DateTimeUtils.NANOS_PER_SECOND
        if (timeA < 0) {
            timeA += DateTimeUtils.NANOS_PER_DAY
            dateValueA = DateTimeUtils.decrementDateValue(dateValueA)
        } else if (timeA >= DateTimeUtils.NANOS_PER_DAY) {
            timeA -= DateTimeUtils.NANOS_PER_DAY
            dateValueA = DateTimeUtils.incrementDateValue(dateValueA)
        }
        var dateValueB = t.dateValue
        var timeB = t.timeNanos - t.timeZoneOffsetSeconds * DateTimeUtils.NANOS_PER_SECOND
        if (timeB < 0) {
            timeB += DateTimeUtils.NANOS_PER_DAY
            dateValueB = DateTimeUtils.decrementDateValue(dateValueB)
        } else if (timeB >= DateTimeUtils.NANOS_PER_DAY) {
            timeB -= DateTimeUtils.NANOS_PER_DAY
            dateValueB = DateTimeUtils.incrementDateValue(dateValueB)
        }
        val cmp = java.lang.Long.compare(dateValueA, dateValueB)
        return if (cmp != 0) {
            cmp
        } else java.lang.Long.compare(timeA, timeB)
    }

    override fun equals(other: Any?): Boolean = (other === this)
            || (other is ValueTimestampTimeZone
            && dateValue == other.dateValue
            && timeNanos == other.timeNanos
            && timeZoneOffsetSeconds == other.timeZoneOffsetSeconds)

    override fun hashCode(): Int = (dateValue xor (dateValue ushr 32)
            xor timeNanos xor (timeNanos ushr 32)
            xor timeZoneOffsetSeconds.toLong()).toInt()
}