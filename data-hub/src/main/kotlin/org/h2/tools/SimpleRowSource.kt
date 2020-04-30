package org.h2.tools

import java.sql.SQLException

/**
 * This interface is for classes that create rows on demand.
 * It is used together siwth SimpleResultSet to create a dynamic result set.
 */
interface SimpleRowSource : AutoCloseable {

    /**
     * Get the next row. Must return null if no more rows are available.
     * @return the row or null
     */
    @Throws(SQLException::class)
    fun readRow(): Array<Any>

    /**
     * Close the row source.
     */
    override fun close()

    /**
     * Reset the position (before the first row).
     * @throws SQLException if this operation is not supported
     */
    @Throws(SQLException::class)
    fun reset()
}