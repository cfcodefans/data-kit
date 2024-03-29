package org.h2.result

import org.h2.value.Value

/**
 * A object where rows are written to.
 */
interface ResultTarget {
    /**
     * Add the row to the result set.
     *
     * @param values the values
     */
    fun addRow(vararg values: Value?)

    /**
     * Get the number of rows.
     *
     * @return the number of rows
     */
    fun getRowCount(): Long

    /**
     * A hint that sorting, offset and limit may be ignored by this result
     * because they were applied during the query. This is useful for WITH TIES
     * clause because result may contain tied rows above limit.
     */
    fun limitsWereApplied()
}