package org.h2.result

import org.h2.engine.Session
import org.h2.value.TypeInfo
import org.h2.value.Value

/**
 * The result interface is used by the LocalResult and ResultRemote class.
 * A result may contain rows, or just an update count.
 */
interface ResultInterface : AutoCloseable {
    /**
     * Go to the beginning of the result, that means
     * before the first row.
     */
    fun reset()

    /**
     * Get the current row.
     * @return the row
     */
    fun currentRow(): Array<Value?>?

    /**
     * Go to the next row.
     * @return true if a row exists
     */
    operator fun next(): Boolean

    /**
     * Get the current row id, starting with 0.
     * -1 is returned when next() was not called yet.
     * @return the row id
     */
    fun getRowId(): Long

    /**
     * Check if the current position is after last row.
     * @return true if after last
     */
    fun isAfterLast(): Boolean

    /**
     * Get the number of visible columns.
     * More columns may exist internally for sorting or grouping.
     * @return the number of columns
     */
    fun getVisibleColumnCount(): Int

    /**
     * Get the number of rows in this object.
     * @return the number of rows
     */
    fun getRowCount(): Long

    /**
     * Check if this result has more rows to fetch.
     * @return true if it has
     */
    operator fun hasNext(): Boolean

    /**
     * Check if this result set should be closed, for example because it is
     * buffered using a temporary file.
     * @return true if close should be called.
     */
    fun needToClose(): Boolean

    /**
     * Close the result and delete any temporary files
     */
    override fun close()

    /**
     * Get the column alias name for the column.
     * @param i the column number (starting with 0)
     * @return the alias name
     */
    fun getAlias(i: Int): String?

    /**
     * Get the schema name for the column, if one exists.
     * @param i the column number (starting with 0)
     * @return the schema name or null
     */
    fun getSchemaName(i: Int): String?

    /**
     * Get the table name for the column, if one exists.
     * @param i the column number (starting with 0)
     * @return the table name or null
     */
    fun getTableName(i: Int): String?

    /**
     * Get the column name.
     *
     * @param i the column number (starting with 0)
     * @return the column name
     */
    fun getColumnName(i: Int): String?

    /**
     * Get the column data type.
     *
     * @param i the column number (starting with 0)
     * @return the column data type
     */
    fun getColumnType(i: Int): TypeInfo?

    /**
     * Check if this is an identity column.
     *
     * @param i the column number (starting with 0)
     * @return true for identity columns
     */
    fun isIdentity(i: Int): Boolean

    /**
     * Check if this column is nullable.
     *
     * @param i the column number (starting with 0)
     * @return Column.NULLABLE_*
     */
    fun getNullable(i: Int): Int

    /**
     * Set the fetch size for this result set.
     *
     * @param fetchSize the new fetch size
     */
    fun setFetchSize(fetchSize: Int)

    /**
     * Get the current fetch size for this result set.
     *
     * @return the fetch size
     */
    fun getFetchSize(): Int

    /**
     * Check if this a lazy execution result.
     *
     * @return true if it is a lazy result
     */
    fun isLazy(): Boolean

    /**
     * Check if this result set is closed.
     *
     * @return true if it is
     */
    fun isClosed(): Boolean

    /**
     * Create a shallow copy of the result set. The data and a temporary table
     * (if there is any) is not copied.
     *
     * @param targetSession the session of the copy
     * @return the copy if possible, or null if copying is not possible
     */
    fun createShallowCopy(targetSession: Session?): ResultInterface?
}