package org.h2.result

import org.h2.engine.Constants
import org.h2.value.Value
import org.h2.value.ValueBigint

/**
 * The default implementation of a row in a table.
 */
open class DefaultRow(val data: Array<Value?>,
                      private var memory: Int = MEMORY_CALCULATE) : Row() {

    constructor(columnCount: Int) : this(data = arrayOfNulls<Value>(columnCount), MEMORY_CALCULATE)

    override fun getMemory(): Int = if (memory != MEMORY_CALCULATE) {
        memory
    } else calculateMemory().also { memory = it }

    /**
     * Calculate the estimated memory used for this row, in bytes.
     *
     * @return the memory
     */
    protected open fun calculateMemory(): Int {
        var m = Constants.MEMORY_ROW + Constants.MEMORY_ARRAY + data.size * Constants.MEMORY_POINTER
        for (v in data) {
            v?.let { m += v.getMemory() }
        }
        return m
    }

    override fun getValue(i: Int): Value? = if (i == ROWID_INDEX) ValueBigint[key] else data[i]

    open fun setValue(i: Int, v: Value) {
        if (i == ROWID_INDEX) key = v.getLong() else data[i] = v
    }

    override fun getColumnCount(): Int = data.size

    override fun getValueList(): Array<Value?> = data

    override fun hasSharedData(other: Row?): Boolean = other is DefaultRow && data == other.data

    override fun toString(): String {
        return "/* key: $key */ ${data.joinToString(", ") { value -> value?.getTraceSQL() ?: "null" }}"
    }

    open fun copyFrom(source: SearchRow) {
        key = source.key
        for (i in 0 until getColumnCount()) {
            setValue(i, source.getValue(i)!!)
        }
    }

    companion object {
        /**
         * The constant that means "memory usage is unknown and needs to be calculated first".
         */
        const val MEMORY_CALCULATE = -1
    }


}