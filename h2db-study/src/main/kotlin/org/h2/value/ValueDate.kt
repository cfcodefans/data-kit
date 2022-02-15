package org.h2.value

import org.h2.api.ErrorCode
import org.h2.engine.CastDataProvider
import org.h2.message.DbException
import org.h2.util.DateTimeUtils as DTU

/**
 * Implementation of the DATE data type.
 */
class ValueDate(val dateValue: Long = 0) : Value() {
    init {
        require(dateValue >= DTU.MIN_DATE_VALUE && dateValue <= DTU.MAX_DATE_VALUE) { "dateValue out of range $dateValue" }
    }

    companion object {
        /**
         * The default precision and display size of the textual representation of a date.
         * Example: 2000-01-02
         */
        const val PRECISION = 10

        /**
         * Get or create a date value for the given date.
         * @param dateValue the date value
         * @return the value
         */
        fun fromDateValue(dateValue: Long): ValueDate = cache(ValueDate(dateValue)) as ValueDate

        /**
         * Parse a string to a ValueDate.
         *
         * @param s the string to parse
         * @return the date
         */
        fun parse(s: String): ValueDate = try {
            fromDateValue(DTU.parseDateValue(s, 0, s.length))
        } catch (e: Exception) {
            throw DbException.get(ErrorCode.INVALID_DATETIME_CONSTANT_2, e, "DATE", s)
        }

        /**
         * Converts this value to a DATE value. May not be called on a NULL value.
         *
         * @param provider
         * the cast information provider
         * @return the DATE value
         */
        fun Value.convertToDate(provider: CastDataProvider): ValueDate = when (getValueType()) {
            DATE -> this as ValueDate
            TIMESTAMP -> fromDateValue((this as ValueTimestamp).dateValue)
            TIMESTAMP_TZ -> {
                val ts = this as ValueTimestampTimeZone
                val timeNanos = ts.timeNanos
                val epochSeconds: Long = DTU.getEpochSeconds(ts.dateValue, timeNanos, ts.timeZoneOffsetSeconds)
                fromDateValue(DTU.dateValueFromLocalSeconds(epochSeconds + provider.currentTimeZone().getTimeZoneOffsetUTC(epochSeconds)))
            }
            VARCHAR, VARCHAR_IGNORECASE, CHAR -> parse(getString()!!.trim { it <= ' ' })
            NULL -> throw DbException.getInternalError()
            else -> throw getDataConversionError(DATE)
        }
    }

    override var type: TypeInfo? = TypeInfo.TYPE_DATE

    override fun getValueType(): Int = DATE

    override fun getString(): String = DTU.appendDate(StringBuilder(PRECISION), dateValue).toString()

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder =
            DTU.appendDate(builder.append("DATE '"), dateValue).append('\'')

    override fun compareTypeSafe(o: Value, mode: CompareMode?, provider: CastDataProvider?): Int =
            dateValue.compareTo((o as ValueDate).dateValue)

    override fun hashCode(): Int = (dateValue xor (dateValue ushr 32)).toInt()

    override fun equals(other: Any?): Boolean = this === other || other is ValueDate && dateValue == other.dateValue

}