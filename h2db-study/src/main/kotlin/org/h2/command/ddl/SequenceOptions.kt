package org.h2.command.ddl

import org.h2.api.ErrorCode
import org.h2.engine.SessionLocal
import org.h2.expression.Expression
import org.h2.expression.ValueExpression
import org.h2.message.DbException
import org.h2.schema.Sequence
import org.h2.schema.Sequence.Cycle
import org.h2.value.TypeInfo
import org.h2.value.Value
import org.h2.value.ValueBigint
import org.h2.value.ValueNull
import kotlin.math.max
import kotlin.math.min

/**
 * Sequence options.
 */
open class SequenceOptions(val oldSequence: Sequence? = null, private var dataType: TypeInfo? = null) {
    companion object {
        /**
         * Get the bounds (min, max) of a data type.
         *
         * @param dataType the data type
         * @return the bounds (an array with 2 elements)
         */
        open fun getBounds(dataType: TypeInfo): LongArray {
            val min: Long
            var max: Long
            when (dataType.valueType) {
                Value.TINYINT -> {
                    min = Byte.MIN_VALUE.toLong()
                    max = Byte.MAX_VALUE.toLong()
                }

                Value.SMALLINT -> {
                    min = Short.MIN_VALUE.toLong()
                    max = Short.MAX_VALUE.toLong()
                }

                Value.INTEGER -> {
                    min = Int.MIN_VALUE.toLong()
                    max = Int.MAX_VALUE.toLong()
                }

                Value.BIGINT -> {
                    min = Long.MIN_VALUE
                    max = Long.MAX_VALUE
                }

                Value.REAL -> {
                    min = -0x1000000
                    max = 0x1000000
                }

                Value.DOUBLE -> {
                    min = -0x20000000000000L
                    max = 0x20000000000000L
                }

                Value.NUMERIC -> {
                    val p: Long = dataType.precision - dataType.scale
                    if (p <= 0) {
                        throw DbException.getUnsupportedException(dataType.getTraceSQL())
                    } else if (p > 18) {
                        min = Long.MIN_VALUE
                        max = Long.MAX_VALUE
                    } else {
                        max = 10
                        var i = 1;while (i < p) {
                            max *= 10
                            i++; }
                        min = -(--max)
                    }
                }

                Value.DECFLOAT -> {
                    val p: Long = dataType.precision
                    if (p > 18) {
                        min = Long.MIN_VALUE
                        max = Long.MAX_VALUE
                    } else {
                        max = 10
                        var i = 1;while (i < p) {
                            max *= 10
                            i++; }
                        min = -max
                    }
                }

                else -> throw DbException.getUnsupportedException(dataType.getTraceSQL())
            }
            return longArrayOf(min, max)
        }

        private fun getLong(session: SessionLocal, expr: Expression?): Long? {
            if (expr == null) return null

            val value = expr.optimize(session)!!.getValue(session)
            return if (value !== ValueNull.INSTANCE) value!!.getLong() else null
        }
    }

    val bounds: LongArray? = dataType?.let { getBounds(it) }

    open fun getDataType(): TypeInfo? {
        if (oldSequence != null) {
            synchronized(oldSequence) { copyFromOldSequence() }
        }
        return dataType
    }


    open fun setDataType(dataType: TypeInfo?) {
        this.dataType = dataType
    }

    private var start: Expression? = null
    private var restart: Expression? = null
    private var increment: Expression? = null
    private var maxValue: Expression? = null
    private var minValue: Expression? = null
    private var cycle: Cycle? = null
    private var cacheSize: Expression? = null

    private fun copyFromOldSequence() {
        val bounds = bounds!!
        var min = max(oldSequence!!.minValue, bounds[0])
        var max = min(oldSequence!!.maxValue, bounds[1])
        if (max < min) {
            min = bounds[0]
            max = bounds[1]
        }
        minValue = ValueExpression.get(ValueBigint[min])
        maxValue = ValueExpression.get(ValueBigint[max])
        var v = oldSequence!!.startValue
        if (v >= min && v <= max) {
            start = ValueExpression.get(ValueBigint[v])
        }
        v = oldSequence!!.baseValue
        if (v >= min && v <= max) {
            restart = ValueExpression.get(ValueBigint[v])
        }
        increment = ValueExpression.get(ValueBigint[oldSequence!!.increment])
        cycle = oldSequence!!.cycle
        cacheSize = ValueExpression.get(ValueBigint[oldSequence!!.cacheSize])
    }

    /**
     * Gets start value.
     *
     * @param session The session to calculate the value.
     * @return start value or `null` if value is not defined.
     */
    open fun getStartValue(session: SessionLocal?): Long? = check(getLong(session!!, start))

    private fun check(value: Long?): Long? = value?.let {
        val bounds = bounds!!
        if (value < bounds[0] || value > bounds[1]) throw DbException.get(ErrorCode.NUMERIC_VALUE_OUT_OF_RANGE_1, value.toString())
        value
    }
}