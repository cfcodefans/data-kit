package org.h2.value

import com.google.common.base.Objects
import org.h2.api.ErrorCode
import org.h2.api.Interval
import org.h2.api.IntervalQualifier
import org.h2.engine.CastDataProvider
import org.h2.message.DbException
import org.h2.util.DateTimeUtils
import org.h2.util.IntervalUtils
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Implementation of the INTERVAL data type.
 */
class ValueInterval private constructor(private val valueType: Int = 0,
                                        val negative: Boolean = false,
                                        val leading: Long = 0,
                                        val remaining: Long = 0) : Value() {

    companion object {
        /**
         * The default leading field precision for intervals.
         */
        const val DEFAULT_PRECISION = 2

        /**
         * The maximum leading field precision for intervals.
         */
        const val MAXIMUM_PRECISION = 18

        /**
         * The default scale for intervals with seconds.
         */
        const val DEFAULT_SCALE = 6

        /**
         * The maximum scale for intervals with seconds.
         */
        const val MAXIMUM_SCALE = 9

        /**
         * Create a ValueInterval instance.
         *
         * @param qualifier  qualifier
         * @param negative whether interval is negative
         * @param leading value of leading field
         * @param remaining values of all remaining fields
         * @return interval value
         */
        fun from(qualifier: IntervalQualifier, negative: Boolean, leading: Long, remaining: Long): ValueInterval {
            var negative = IntervalUtils.validateInterval(qualifier, negative, leading, remaining)
            return cache(ValueInterval(qualifier.ordinal + INTERVAL_YEAR,
                    negative,
                    leading,
                    remaining)) as ValueInterval
        }

        /**
         * Returns display size for the specified qualifier, precision and fractional seconds precision.
         * @param type the value type
         * @param precision leading field precision
         * @param scale fractional seconds precision. Ignored if specified type of
         *              interval does not have seconds.
         * @return display size
         */
        fun getDisplaySize(type: Int, precision: Int, scale: Int): Int = when (type) {
            INTERVAL_YEAR, INTERVAL_HOUR -> 17 + precision
            INTERVAL_MONTH -> 18 + precision
            INTERVAL_DAY -> 16 + precision
            INTERVAL_MINUTE -> 19 + precision
            INTERVAL_SECOND -> if (scale > 0) 20 + precision + scale else 19 + precision
            INTERVAL_YEAR_TO_MONTH -> 29 + precision
            INTERVAL_DAY_TO_HOUR -> 27 + precision
            INTERVAL_DAY_TO_MINUTE -> 32 + precision
            INTERVAL_DAY_TO_SECOND -> if (scale > 0) 36 + precision + scale else 35 + precision
            INTERVAL_HOUR_TO_MINUTE -> 30 + precision
            INTERVAL_HOUR_TO_SECOND -> if (scale > 0) 34 + precision + scale else 33 + precision
            INTERVAL_MINUTE_TO_SECOND -> if (scale > 0) 33 + precision + scale else 32 + precision
            else -> throw DbException.getUnsupportedException(type.toString())
        }

        private val MULTIPLIERS = longArrayOf( // INTERVAL_SECOND
                DateTimeUtils.NANOS_PER_SECOND,  // INTERVAL_YEAR_TO_MONTH
                12,  // INTERVAL_DAY_TO_HOUR
                24,  // INTERVAL_DAY_TO_MINUTE
                (24 * 60).toLong(),  // INTERVAL_DAY_TO_SECOND
                DateTimeUtils.NANOS_PER_DAY,  // INTERVAL_HOUR_TO_MINUTE:
                60,  // INTERVAL_HOUR_TO_SECOND
                DateTimeUtils.NANOS_PER_HOUR,  // INTERVAL_MINUTE_TO_SECOND
                DateTimeUtils.NANOS_PER_MINUTE //
        )

        fun Value.convertToIntervalYearMonth(targetType: TypeInfo, conversionMode: Int, column: Any?): ValueInterval {
            val v = convertToIntervalYearMonth(targetType.valueType, column)
            if (conversionMode != CONVERT_TO) {
                if (!v.checkPrecision(targetType.precision)) throw v.getValueTooLongException(targetType, column)
            }
            return v
        }

        val MONTH12_BD: BigDecimal = BigDecimal.valueOf(12)

        private fun Value.convertToIntervalYearMonth(targetType: Int, column: Any?): ValueInterval {
            var leading: Long
            when (getValueType()) {
                TINYINT, SMALLINT, INTEGER -> leading = getInt().toLong()
                BIGINT -> leading = getLong()
                REAL, DOUBLE -> {
                    if (targetType == INTERVAL_YEAR_TO_MONTH) {
                        return IntervalUtils.intervalFromAbsolute(
                                IntervalQualifier.YEAR_TO_MONTH,
                                getBigDecimal().multiply(MONTH12_BD).setScale(0, RoundingMode.HALF_UP).toBigInteger())
                    }
                    leading = convertToLong(getDouble(), column)
                }
                NUMERIC, DECFLOAT -> {
                    if (targetType == INTERVAL_YEAR_TO_MONTH) {
                        return IntervalUtils.intervalFromAbsolute(IntervalQualifier.YEAR_TO_MONTH,
                                getBigDecimal().multiply(MONTH12_BD).setScale(0, RoundingMode.HALF_UP).toBigInteger())
                    }
                    leading = convertToLong(getBigDecimal(), column)
                }
                VARCHAR, VARCHAR_IGNORECASE, CHAR -> {
                    val s: String? = getString()
                    return try {
                        IntervalUtils
                                .parseFormattedInterval(IntervalQualifier.valueOf(targetType - INTERVAL_YEAR), s!!)
                                .convertTo(targetType) as ValueInterval
                    } catch (e: Exception) {
                        throw DbException.get(ErrorCode.INVALID_DATETIME_CONSTANT_2, e, "INTERVAL", s)
                    }
                }
                INTERVAL_YEAR, INTERVAL_MONTH, INTERVAL_YEAR_TO_MONTH -> return IntervalUtils.intervalFromAbsolute(
                        IntervalQualifier.valueOf(targetType - INTERVAL_YEAR),
                        IntervalUtils.intervalToAbsolute((this as ValueInterval)))
                else -> throw getDataConversionError(targetType)
            }
            var negative = false
            if (leading < 0) {
                negative = true
                leading = -leading
            }
            return from(IntervalQualifier.valueOf(targetType - INTERVAL_YEAR), negative, leading, 0L)
        }

        internal fun Value.convertToIntervalDayTime(targetType: TypeInfo, conversionMode: Int, column: Any?): ValueInterval {
            var v = convertToIntervalDayTime(targetType.valueType, column)
            if (conversionMode != CONVERT_TO) {
                v = v!!.setPrecisionAndScale(targetType, column)
            }
            return v
        }

        private fun Value.convertToIntervalDayTime(bigDecimal: BigDecimal, targetType: Int): ValueInterval {
            val multiplier: Long = when (targetType) {
                INTERVAL_SECOND -> DateTimeUtils.NANOS_PER_SECOND
                INTERVAL_DAY_TO_HOUR, INTERVAL_DAY_TO_MINUTE, INTERVAL_DAY_TO_SECOND -> DateTimeUtils.NANOS_PER_DAY
                INTERVAL_HOUR_TO_MINUTE, INTERVAL_HOUR_TO_SECOND -> DateTimeUtils.NANOS_PER_HOUR
                INTERVAL_MINUTE_TO_SECOND -> DateTimeUtils.NANOS_PER_MINUTE
                else -> throw getDataConversionError(targetType)
            }
            return IntervalUtils.intervalFromAbsolute(
                    IntervalQualifier.valueOf(targetType - INTERVAL_YEAR),
                    bigDecimal
                            .multiply(BigDecimal.valueOf(multiplier))
                            .setScale(0, RoundingMode.HALF_UP)
                            .toBigInteger())
        }

        private fun Value.convertToIntervalDayTime(targetType: Int, column: Any?): ValueInterval {
            var leading: Long
            when (getValueType()) {
                TINYINT, SMALLINT, INTEGER -> leading = getInt().toLong()
                BIGINT -> leading = getLong()
                REAL, DOUBLE -> {
                    if (targetType > INTERVAL_MINUTE) {
                        return convertToIntervalDayTime(getBigDecimal(), targetType)
                    }
                    leading = convertToLong(getDouble(), column)
                }
                NUMERIC, DECFLOAT -> {
                    if (targetType > INTERVAL_MINUTE) {
                        return convertToIntervalDayTime(getBigDecimal(), targetType)
                    }
                    leading = convertToLong(getBigDecimal(), column)
                }
                VARCHAR, VARCHAR_IGNORECASE, CHAR -> {
                    val s: String = getString()!!
                    return try {
                        IntervalUtils
                                .parseFormattedInterval(IntervalQualifier.valueOf(targetType - INTERVAL_YEAR), s)
                                .convertTo(targetType) as ValueInterval
                    } catch (e: java.lang.Exception) {
                        throw DbException.get(ErrorCode.INVALID_DATETIME_CONSTANT_2, e, "INTERVAL", s)
                    }
                }
                INTERVAL_DAY, INTERVAL_HOUR, INTERVAL_MINUTE, INTERVAL_SECOND, INTERVAL_DAY_TO_HOUR, INTERVAL_DAY_TO_MINUTE, INTERVAL_DAY_TO_SECOND, INTERVAL_HOUR_TO_MINUTE, INTERVAL_HOUR_TO_SECOND, INTERVAL_MINUTE_TO_SECOND ->
                    return IntervalUtils.intervalFromAbsolute(IntervalQualifier.valueOf(targetType - INTERVAL_YEAR),
                        IntervalUtils.intervalToAbsolute((this as ValueInterval)))
                else -> throw getDataConversionError(targetType)
            }
            var negative = false
            if (leading < 0) {
                negative = true
                leading = -leading
            }
            return from(IntervalQualifier.valueOf(targetType - INTERVAL_YEAR), negative, leading, 0L)
        }
    }

    override var type: TypeInfo? = TypeInfo.getTypeInfo(valueType)

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder {
        return IntervalUtils.appendInterval(builder, getQualifier(), negative, leading, remaining)
    }

    override fun getValueType(): Int = valueType

    override fun getMemory(): Int = 48// Java 11 with -XX:-UseCompressedOops

    /**
     * Check if the precision is smaller or equal than the given precision.
     *
     * @param prec the maximum precision
     * @return true if the precision of this value is smaller or equal to the given precision
     */
    fun checkPrecision(prec: Long): Boolean {
        if (prec >= MAXIMUM_PRECISION) return true

        val l = leading
        var p: Long = 1
        var precision: Long = 0
        while (l >= p) {
            if (++precision > prec) return false
            p *= 10
        }
        return true
    }

    fun setPrecisionAndScale(targetType: TypeInfo, column: Any?): ValueInterval {
        val targetScale: Int = targetType.scale
        var v = this
        if (targetScale < MAXIMUM_SCALE) run {
            val range: Long = when (valueType) {
                INTERVAL_SECOND -> DateTimeUtils.NANOS_PER_SECOND
                INTERVAL_DAY_TO_SECOND -> DateTimeUtils.NANOS_PER_DAY
                INTERVAL_HOUR_TO_SECOND -> DateTimeUtils.NANOS_PER_HOUR
                INTERVAL_MINUTE_TO_SECOND -> DateTimeUtils.NANOS_PER_MINUTE
                else -> return@run
            }
            var l = leading
            var r = DateTimeUtils.convertScale(nanosOfDay = remaining,
                    scale = targetScale,
                    range = if (l == 999999999999999999L) range else Long.MAX_VALUE)
            if (r != remaining) {
                if (r >= range) {
                    l++
                    r -= range
                }
                v = from(v.getQualifier(), v.isNegative(), l, r)
            }
        }
        if (!v.checkPrecision(targetType.precision)) throw v.getValueTooLongException(targetType, column)

        return v
    }

    /**
     * Returns the interval qualifier.
     * @return the interval qualifier
     */
    fun getQualifier(): IntervalQualifier = IntervalQualifier.valueOf(valueType - INTERVAL_YEAR)

    /**
     * Returns where the interval is negative.
     *
     * @return where the interval is negative
     */
    fun isNegative(): Boolean = negative

    override fun getString(): String = IntervalUtils.appendInterval(StringBuilder(), getQualifier(), negative, leading, remaining).toString()

    override fun getLong(): Long {
        var l = leading
        if (valueType >= INTERVAL_SECOND
                && remaining != 0L
                && remaining >= MULTIPLIERS[valueType - INTERVAL_SECOND] shr 1) {
            l++
        }
        return if (negative) -l else l
    }

    override fun getBigDecimal(): BigDecimal {
        if (valueType < INTERVAL_SECOND || remaining == 0L) return BigDecimal.valueOf(if (negative) -leading else leading)

        val m = BigDecimal.valueOf(MULTIPLIERS[valueType - INTERVAL_SECOND])
        val bd = BigDecimal.valueOf(leading)
                .add(BigDecimal.valueOf(remaining).divide(m, m.precision(), RoundingMode.HALF_DOWN)) //
                .stripTrailingZeros()
        return if (negative) bd.negate() else bd
    }

    override fun getFloat(): Float {
        return if (valueType < INTERVAL_SECOND || remaining == 0L) {
            if (negative) (-leading).toFloat() else leading.toFloat()
        } else getBigDecimal().toFloat()
    }

    override fun getDouble(): Double {
        return if (valueType < INTERVAL_SECOND || remaining == 0L) {
            if (negative) (-leading).toDouble() else leading.toDouble()
        } else getBigDecimal().toDouble()
    }

    /**
     * Returns the interval.
     *
     * @return the interval
     */
    fun getInterval(): Interval = Interval(getQualifier(), negative, leading, remaining)

    override fun hashCode(): Int = Objects.hashCode(valueType, negative, leading, remaining)

    override fun equals(other: Any?): Boolean = (this === other)
            || (other is ValueInterval
            && valueType == other.valueType
            && negative == other.negative
            && leading == other.leading
            && remaining == other.remaining)

    override fun compareTypeSafe(v: Value, mode: CompareMode?, provider: CastDataProvider?): Int {
        val other = v as ValueInterval
        if (negative != other.negative) {
            return if (negative) -1 else 1
        }
        var cmp = leading.compareTo(other.leading)
        if (cmp == 0) {
            cmp = remaining.compareTo(other.remaining)
        }
        return if (negative) -cmp else cmp
    }

    override fun getSignum(): Int = if (negative) -1 else if (leading == 0L && remaining == 0L) 0 else 1

    override fun add(v: Value): Value {
        return IntervalUtils.intervalFromAbsolute(getQualifier(),
                IntervalUtils.intervalToAbsolute(this).add(IntervalUtils.intervalToAbsolute((v as ValueInterval))))
    }

    override fun subtract(v: Value): Value {
        return IntervalUtils.intervalFromAbsolute(getQualifier(),
                IntervalUtils.intervalToAbsolute(this).subtract(IntervalUtils.intervalToAbsolute(v as ValueInterval)))
    }

    override fun negate(): Value = if (leading == 0L && remaining == 0L) this else cache(ValueInterval(valueType, !negative, leading, remaining))
}