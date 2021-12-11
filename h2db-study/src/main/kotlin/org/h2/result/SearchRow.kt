package org.h2.result

import org.h2.value.Value
import org.h2.value.ValueNull

/**
 * The base class for rows stored in a table, and for partial rows stored in the index
 */
abstract class SearchRow: Value() {

    companion object {
        /**
         * Index of a virtual "_ROWID_" column within a row or a table
         */
        const val ROWID_INDEX = -1

        /**
         * If the key is this value, then the key is considered equal to all other
         * keys, when comparing.
         */
        var MATCH_ALL_ROW_KEY = Long.MIN_VALUE + 1

        /**
         * The constant that means "memory usage is unknown and needs to be calculated first".
         */
        const val MEMORY_CALCULATE = -1
    }

    /**
     * The row key.
     */
    protected var key: Long = 0

    /**
     * Get the column count.
     *
     * @return the column count
     */
    abstract fun getColumnCount(): Int

    /**
     * Determine if specified column contains NULL
     * @param index column index
     * @return true if NULL
     */
    open fun isNull(index: Int): Boolean {
        return getValue(index) === ValueNull.INSTANCE
    }

    /**
     * Get the value for the column
     *
     * @param index the column number (starting with 0)
     * @return the value
     */
    abstract fun getValue(index: Int): Value?

}