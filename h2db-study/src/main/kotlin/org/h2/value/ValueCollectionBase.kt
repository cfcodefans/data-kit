package org.h2.value

import org.h2.api.ErrorCode
import org.h2.engine.CastDataProvider
import org.h2.engine.Constants
import org.h2.message.DbException

/**
 * Base class for ARRAY and ROW values.
 */
abstract class ValueCollectionBase(val values: Array<Value?>) : Value() {
    private var hash: Int = 0

    fun getList(): Array<Value?>? = values

    override fun hashCode(): Int {
        if (hash != 0) return hash

        var h = getValueType()
        for (v in values!!) h = h * 31 + v.hashCode()
        hash = h
        return h
    }

    override fun containsNull(): Boolean = values?.any { it?.containsNull() == true }

    override fun getMemory(): Int = (72 + values.size * Constants.MEMORY_POINTER) + values.sumOf { it!!.getMemory() }

    override fun compareWithNull(v: Value, forEquality: Boolean, provider: CastDataProvider?, compareMode: CompareMode?): Int {
        if (v === ValueNull.INSTANCE) return Int.MIN_VALUE

        val l = this
        val leftType = l.getValueType()
        val rightType = v.getValueType()
        if (rightType != leftType) throw v.getDataConversionError(leftType)

        val r = v as ValueCollectionBase
        val leftArray = l.values
        val leftLength = leftArray!!.size

        val rightArray = r.values
        val rightLength = rightArray!!.size

        if (leftLength != rightLength) {
            if (leftType == ROW) throw DbException.get(ErrorCode.COLUMN_COUNT_DOES_NOT_MATCH)
            if (forEquality) return 1
        }

        if (forEquality) {
            var hasNull = false
            for (i in 0 until leftLength) {
                val v1: Value = leftArray[i]!!
                val v2: Value = rightArray[i]!!
                val comp: Int = v1.compareWithNull(v2, forEquality, provider, compareMode)
                if (comp != 0) {
                    if (comp != Int.MIN_VALUE) return comp
                    hasNull = true
                }
            }
            return if (hasNull) Int.MIN_VALUE else 0
        }
        val len = Math.min(leftLength, rightLength)
        for (i in 0 until len) {
            val v1 = leftArray[i]!!
            val v2 = rightArray[i]!!
            val comp: Int = v1.compareWithNull(v2, forEquality, provider, compareMode)
            if (comp != 0) {
                return comp
            }
        }
        return leftLength.compareTo(rightLength)
    }
}