package org.h2.result

import org.h2.value.Value

/**
 * Represents a row in a table.
 */
abstract class Row : SearchRow() {
    companion object {
        /**
         * Creates a new row.
         *
         * @param data values of columns, or null
         * @param memory used memory
         * @return the allocated row
         */
        operator fun get(data: Array<Value?>, memory: Int): Row = DefaultRow(data, memory)

        /**
         * Creates a new row with the specified key.
         *
         * @param data values of columns, or null
         * @param memory used memory
         * @param key the key
         * @return the allocated row
         */
        operator fun get(data: Array<Value?>, memory: Int, key: Long): Row {
            return DefaultRow(data, memory).apply { this.key = key }
        }
    }


    /**
     * Get values.
     * @return values
     */
    abstract fun getValueList(): Array<Value?>

    /**
     * Check whether values of this row are equal to values of other row.
     *
     * @param other
     * the other row
     * @return `true` if values are equal,
     * `false` otherwise
     */
    open fun hasSameValues(other: Row): Boolean {
        return getValueList().contentEquals(other.getValueList())
    }

    /**
     * Check whether this row and the specified row share the same underlying
     * data with values. This method must return `false` when values are
     * not equal and may return either `true` or `false` when they
     * are equal. This method may be used only for optimizations and should not
     * perform any slow checks, such as equality checks for all pairs of values.
     *
     * @param other the other row
     * @return `true` if rows share the same underlying data,
     * `false` otherwise or when unknown
     */
    open fun hasSharedData(other: Row?): Boolean = false
}