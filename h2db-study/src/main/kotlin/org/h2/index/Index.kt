package org.h2.index

import org.h2.api.ErrorCode
import org.h2.command.query.AllColumnsForPlan
import org.h2.engine.Constants
import org.h2.engine.DbObject
import org.h2.engine.Mode.UniqueIndexNullsHandling
import org.h2.engine.SessionLocal
import org.h2.message.DbException
import org.h2.message.Trace
import org.h2.result.Row
import org.h2.result.RowFactory
import org.h2.result.SearchRow
import org.h2.result.SortOrder
import org.h2.schema.SchemaObject
import org.h2.table.Column
import org.h2.table.IndexColumn
import org.h2.table.Table
import org.h2.table.TableFilter
import org.h2.util.HasSQL
import org.h2.util.StringUtils
import org.h2.value.CompareMode
import org.h2.value.Typed
import org.h2.value.Value
import org.h2.value.ValueNull
import kotlin.math.max

/**
 * An index. Indexes are used to speed up searching data.
 */
abstract class Index(
        /**
         * The table.
         */
        protected open val table: Table,
        /**
         * Columns of this index.
         */
        protected var indexColumns: Array<IndexColumn>?,

        /**
         * The index type.
         */
        protected val indexType: IndexType,
        protected val uniqueColumnColumn: Int,
        id: Int,
        name: String?) : SchemaObject(name = name, id = id, traceModuleId = Trace.INDEX, schema = table.schema) {

    /**
     * Table columns used in this index.
     */
    protected var columns: Array<Column>? = indexColumns?.map { it.column }?.toTypedArray()

    /**
     * Identities of table columns.
     */
    protected var columnIds: IntArray? = indexColumns?.map { it.column.columnId }?.toIntArray()

    private var rowFactory: RowFactory? = null

    private var uniqueRowFactory: RowFactory? = null

    init {
        val compareMode: CompareMode = database!!.getCompareMode()!!
        val databaseRowFactory: RowFactory = database!!.rowFactory
        val tableColumns: Array<Typed> = table.columns as Array<Typed>
        rowFactory = databaseRowFactory.createRowFactory(provider = database,
                compareMode = compareMode,
                handler = database,
                columns = tableColumns,
                indexColumns = if (indexType.isScan()) null else indexColumns, storeKeys = true)

        uniqueRowFactory = if (uniqueColumnColumn > 0) {
            if (indexColumns == null || uniqueColumnColumn == indexColumns!!.size) {
                rowFactory
            } else {
                databaseRowFactory.createRowFactory(provider = database,
                        compareMode = compareMode,
                        handler = database,
                        columns = tableColumns,
                        indexColumns?.copyOf<IndexColumn>(uniqueColumnColumn)?.filterNotNull()?.toTypedArray(),
                        storeKeys = true)
            }
        } else {
            null
        }
    }

    /**
     * Remove the index.
     *
     * @param session the session
     */
    abstract fun remove(session: SessionLocal?)

    /**
     * Close this index.
     *
     * @param session the session used to write data
     */
    abstract fun close(session: SessionLocal)

    /**
     * Add a row to the index.
     *
     * @param session the session to use
     * @param row the row to add
     */
    abstract fun add(session: SessionLocal, row: Row)

    /**
     * Remove a row from the index.
     *
     * @param session the session
     * @param row the row
     */
    abstract fun remove(session: SessionLocal, row: Row)

    /**
     * Find a row or a list of rows and create a cursor to iterate over the
     * result.
     *
     * @param session the session
     * @param first the first row, or null for no limit
     * @param last the last row, or null for no limit
     * @return the cursor to iterate over the results
     */
    abstract fun find(session: SessionLocal, first: SearchRow, last: SearchRow): Cursor?

    /**
     * Estimate the cost to search for rows given the search mask.
     * There is one element per column in the search mask.
     * For possible search masks, see IndexCondition.
     *
     * @param session the session
     * @param masks per-column comparison bit masks, null means 'always false',
     * see constants in IndexCondition
     * @param filters all joined table filters
     * @param filter the current table filter index
     * @param sortOrder the sort order
     * @param allColumnsSet the set of all columns
     * @return the estimated cost
     */
    abstract fun getCost(session: SessionLocal,
                         masks: IntArray?,
                         filters: Array<TableFilter>?,
                         filter: Int,
                         sortOrder: SortOrder?,
                         allColumnsSet: AllColumnsForPlan?): Double

    /**
     * Remove all rows from the index.
     *
     * @param session the session
     */
    abstract fun truncate(session: SessionLocal?)

    /**
     * Check if the index needs to be rebuilt.
     * This method is called after opening an index.
     *
     * @return true if a rebuild is required.
     */
    abstract fun needRebuild(): Boolean

    /**
     * Get the row count of this table, for the given session.
     *
     * @param session the session
     * @return the row count
     */
    abstract fun getRowCount(session: SessionLocal): Long

    /**
     * Get the approximated row count for this table.
     *
     * @param session the session
     * @return the approximated row count
     */
    abstract fun getRowCountApproximation(session: SessionLocal): Long

    override fun getType(): Int = DbObject.INDEX

    override fun removeChildrenAndResources(session: SessionLocal) {
        table.removeIndex(this)
        remove(session)
        database!!.removeMeta(session, id)
    }


    override fun isHidden(): Boolean = table.isHidden()

    override fun getCreateSQLForCopy(targetTable: Table, quotedName: String): String {
        val builder = StringBuilder("CREATE ")
        builder.append(indexType.getSQL())
        builder.append(' ')
        if (table.isHidden()) {
            builder.append("IF NOT EXISTS ")
        }
        builder.append(quotedName)
        builder.append(" ON ")
        targetTable.getSQL(builder, HasSQL.DEFAULT_SQL_FLAGS)
        if (comment != null) {
            builder.append(" COMMENT ")
            StringUtils.quoteStringSQL(builder, comment)
        }
        return getColumnListSQL(builder, HasSQL.DEFAULT_SQL_FLAGS).toString()
    }

    /**
     * Get the list of columns as a string.
     *
     * @param sqlFlags formatting flags
     * @return the list of columns
     */
    private fun getColumnListSQL(builder: StringBuilder, sqlFlags: Int): java.lang.StringBuilder {
        builder.append('(')
        val length = indexColumns!!.size
        if (uniqueColumnColumn > 0 && uniqueColumnColumn < length) {
            IndexColumn.writeColumns(builder, indexColumns, 0, uniqueColumnColumn, sqlFlags).append(") INCLUDE(")
            IndexColumn.writeColumns(builder, indexColumns, uniqueColumnColumn, length, sqlFlags)
        } else {
            IndexColumn.writeColumns(builder, indexColumns, 0, length, sqlFlags)
        }
        return builder.append(')')
    }

    override fun getCreateSQL(): String = getCreateSQLForCopy(table, getSQL(HasSQL.DEFAULT_SQL_FLAGS))

    /**
     * Get the message to show in a EXPLAIN statement.
     *
     * @return the plan
     */
    open fun getPlanSQL(): String? = getSQL(HasSQL.TRACE_SQL_FLAGS or HasSQL.ADD_PLAN_INFORMATION)

    /**
     * Update index after row change.
     *
     * @param session the session
     * @param oldRow row before the update
     * @param newRow row after the update
     */
    open fun update(session: SessionLocal, oldRow: Row, newRow: Row) {
        remove(session, oldRow)
        add(session, newRow)
    }

    /**
     * Returns `true` if `find()` implementation performs scan over all
     * index, `false` if `find()` performs the fast lookup.
     *
     * @return `true` if `find()` implementation performs scan over all
     * index, `false` if `find()` performs the fast lookup
     */
    open fun isFindUsingFullTableScan(): Boolean = false

    /**
     * Check if the index can directly look up the lowest or highest value of a
     * column.
     *
     * @return true if it can
     */
    open fun canGetFirstOrLast(): Boolean = false

    /**
     * Check if the index can get the next higher value.
     *
     * @return true if it can
     */
    open fun canFindNext(): Boolean = false

    /**
     * Find a row or a list of rows that is larger and create a cursor to
     * iterate over the result.
     *
     * @param session the session
     * @param higherThan the lower limit (excluding)
     * @param last the last row, or null for no limit
     * @return the cursor
     */
    open fun findNext(session: SessionLocal, higherThan: SearchRow, last: SearchRow): Cursor? {
        throw DbException.getInternalError(toString())
    }

    /**
     * Find the first (or last) value of this index. The cursor returned is
     * positioned on the correct row, or on null if no row has been found.
     *
     * @param session the session
     * @param first true if the first (lowest for ascending indexes) or last
     * value should be returned
     * @return a cursor (never null)
     */
    open fun findFirstOrLast(session: SessionLocal, first: Boolean): Cursor? {
        throw DbException.getInternalError(toString())
    }

    /**
     * Get the used disk space for this index.
     *
     * @return the estimated number of bytes
     */
    open fun getDiskSpaceUsed(): Long = 0L

    /**
     * Compare two rows.
     *
     * @param rowData the first row
     * @param compare the second row
     * @return 0 if both rows are equal, -1 if the first row is smaller,
     * otherwise 1
     */
    fun compareRows(rowData: SearchRow, compare: SearchRow): Int {
        if (rowData === compare) {
            return 0
        }
        var i = 0;
        val len = indexColumns!!.size; while (i < len) {
            val index = columnIds!![i]
            val v1: Value? = rowData.getValue(index)
            val v2: Value? = compare.getValue(index)
            // can't compare further
            if (v1 == null || v2 == null) return 0

            val c = compareValues(v1, v2, indexColumns!![i].sortType)
            if (c != 0) return c
            i++
        }
        return 0
    }

    private fun compareValues(a: Value, b: Value, sortType: Int): Int {
        if (a === b) {
            return 0
        }
        val aNull = a === ValueNull.INSTANCE
        if (aNull || b === ValueNull.INSTANCE) {
            return table.database!!.defaultNullOrdering.compareNull(aNull, sortType)
        }
        var comp = table.compareValues(database, a, b)
        if (sortType and SortOrder.DESCENDING != 0) {
            comp = -comp
        }
        return comp
    }

    /**
     * Get the index of a column in the list of index columns
     *
     * @param col the column
     * @return the index (0 meaning first column)
     */
    open fun getColumnIndex(col: Column): Int = columns!!.indexOfFirst { it == col }

    /**
     * Check if the given column is the first for this index
     *
     * @param column the column
     * @return true if the given columns is the first
     */
    open fun isFirstColumn(column: Column): Boolean = column == columns!![0]


    /**
     * Get the row with the given key.
     *
     * @param session the session
     * @param key the unique key
     * @return the row
     */
    open fun getRow(session: SessionLocal, key: Long): Row? {
        throw DbException.getUnsupportedException(toString())
    }

    /**
     * Create a duplicate key exception with a message that contains the index
     * name.
     *
     * @param key the key values
     * @return the exception
     */
    fun getDuplicateKeyException(key: String?): DbException {
        val builder = java.lang.StringBuilder()
        getSQL(builder, HasSQL.TRACE_SQL_FLAGS).append(" ON ")
        table.getSQL(builder, HasSQL.TRACE_SQL_FLAGS)
        getColumnListSQL(builder, HasSQL.TRACE_SQL_FLAGS)
        if (key != null) {
            builder.append(" VALUES ").append(key)
        }
        val e: DbException = DbException.get(ErrorCode.DUPLICATE_KEY_1, builder.toString())
        e.source = this
        return e
    }

    /**
     * Get "PRIMARY KEY ON &lt;table&gt; [(column)]".
     *
     * @param mainIndexColumn the column index
     * @return the message
     */
    protected fun getDuplicatePrimaryKeyMessage(mainIndexColumn: Int): StringBuilder {
        val builder: StringBuilder = StringBuilder("PRIMARY KEY ON ")
        table.getSQL(builder, HasSQL.TRACE_SQL_FLAGS)
        if (mainIndexColumn >= 0 && mainIndexColumn < indexColumns!!.size) {
            builder.append('(')
            indexColumns!![mainIndexColumn].getSQL(builder, HasSQL.TRACE_SQL_FLAGS).append(')')
        }
        return builder
    }


    /**
     * Calculate the cost for the given mask as if this index was a typical
     * b-tree range index. This is the estimated cost required to search one
     * row, and then iterate over the given number of rows.
     *
     * @param masks the IndexCondition search masks, one for each column in the
     *            table
     * @param rowCount the number of rows in the index
     * @param filters all joined table filters
     * @param filter the current table filter index
     * @param sortOrder the sort order
     * @param isScanIndex whether this is a "table scan" index
     * @param allColumnsSet the set of all columns
     * @return the estimated cost
     */
    protected fun getCostRangeIndex(masks: IntArray?,
                                    rowCount: Long,
                                    filters: Array<TableFilter>?,
                                    filter: Int,
                                    sortOrder: SortOrder?,
                                    isScanIndex: Boolean,
                                    allColumnsSet: AllColumnsForPlan?): Long {
        var rowCount = rowCount
        rowCount += Constants.COST_ROW_OFFSET.toLong()
        var totalSelectivity = 0
        var rowsCost = rowCount
        if (masks != null) {
            var i = 0
            val len = columns!!.size
            var tryAdditional = false
            while (i < len) {
                val column = columns!![i++]
                val index: Int = column.columnId
                val mask = masks[index]
                if (mask and IndexCondition.EQUALITY == IndexCondition.EQUALITY) {
                    if (i > 0 && i == uniqueColumnColumn) {
                        rowsCost = 3
                        break
                    }
                    totalSelectivity = 100 - (100 - totalSelectivity) * (100 - column.selectivity) / 100
                    var distinctRows = rowCount * totalSelectivity / 100
                    if (distinctRows <= 0) {
                        distinctRows = 1
                    }
                    rowsCost = (2 + max((rowCount / distinctRows).toDouble(), 1.0)).toLong()
                } else if (mask and IndexCondition.RANGE == IndexCondition.RANGE) {
                    rowsCost = 2 + rowsCost / 4
                    tryAdditional = true
                    break
                } else if (mask and IndexCondition.START == IndexCondition.START) {
                    rowsCost = 2 + rowsCost / 3
                    tryAdditional = true
                    break
                } else if (mask and IndexCondition.END == IndexCondition.END) {
                    rowsCost /= 3
                    tryAdditional = true
                    break
                } else {
                    if (mask == 0) {
                        // Adjust counter of used columns (i)
                        i--
                    }
                    break
                }
            }
            // Some additional columns can still be used
            if (tryAdditional) {
                while (i < len && masks[columns!![i].columnId] != 0) {
                    i++
                    rowsCost--
                }
            }
            // Increase cost of indexes with additional unused columns
            rowsCost += (len - i).toLong()
        }
        // If the ORDER BY clause matches the ordering of this index,
        // it will be cheaper than another index, so adjust the cost
        // accordingly.
        var sortingCost: Long = 0
        if (sortOrder != null) {
            sortingCost = 100 + rowCount / 10
        }
        if (sortOrder != null && !isScanIndex) {
            var sortOrderMatches = true
            var coveringCount = 0
            val sortTypes = sortOrder.sortTypesWithNullOrdering
            val tableFilter = filters?.get(filter)
            var i = 0
            val len = sortTypes.size
            while (i < len) {
                if (i >= indexColumns!!.size) {
                    // We can still use this index if we are sorting by more
                    // than it's columns, it's just that the coveringCount
                    // is lower than with an index that contains
                    // more of the order by columns.
                    break
                }
                val col = sortOrder.getColumn(i, tableFilter)
                if (col == null) {
                    sortOrderMatches = false
                    break
                }
                val indexCol = indexColumns!![i]
                if (col != indexCol.column) {
                    sortOrderMatches = false
                    break
                }
                val sortType = sortTypes[i]
                if (sortType != indexCol.sortType) {
                    sortOrderMatches = false
                    break
                }
                coveringCount++
                i++
            }
            if (sortOrderMatches) {
                // "coveringCount" makes sure that when we have two
                // or more covering indexes, we choose the one
                // that covers more.
                sortingCost = (100 - coveringCount).toLong()
            }
        }
        // If we have two indexes with the same cost, and one of the indexes can
        // satisfy the query without needing to read from the primary table
        // (scan index), make that one slightly lower cost.
        var needsToReadFromScanIndex: Boolean
        if (!isScanIndex && allColumnsSet != null) {
            needsToReadFromScanIndex = false
            val foundCols = allColumnsSet[table]
            if (foundCols != null) {
                val main = table.getMainIndexColumn()
                loop@ for (c in foundCols) {
                    val id = c.columnId
                    if (id == SearchRow.ROWID_INDEX || id == main) continue
                    for (c2 in columns!!) {
                        if (c === c2) continue@loop
                    }
                    needsToReadFromScanIndex = true
                    break
                }
            }
        } else {
            needsToReadFromScanIndex = true
        }
        val rc: Long = if (isScanIndex) {
            rowsCost + sortingCost + 20
        } else if (needsToReadFromScanIndex) {
            rowsCost + rowsCost + sortingCost + 20
        } else {
            // The (20-x) calculation makes sure that when we pick a covering
            // index, we pick the covering index that has the smallest number of
            // columns (the more columns we have in index - the higher cost).
            // This is faster because a smaller index will fit into fewer data
            // blocks.
            rowsCost + sortingCost + columns!!.size
        }
        return rc
    }

    /**
     * Check if this row may have duplicates with the same indexed values in the
     * current compatibility mode. Duplicates with `NULL` values are
     * allowed in some modes.
     *
     * @param searchRow
     * the row to check
     * @return `true` if specified row may have duplicates,
     * `false otherwise`
     */
    fun mayHaveNullDuplicates(searchRow: SearchRow): Boolean {
        return when (database!!.getMode().uniqueIndexNullsHandling) {
            UniqueIndexNullsHandling.ALLOW_DUPLICATES_WITH_ANY_NULL -> {
                (0..uniqueColumnColumn)
                    .map { i -> columnIds!![i] }
                    .any { index -> searchRow.getValue(index) === ValueNull.INSTANCE }
            }
            UniqueIndexNullsHandling.ALLOW_DUPLICATES_WITH_ALL_NULLS -> {
                (0..uniqueColumnColumn)
                    .map { i -> columnIds!![i] }
                    .any { index -> searchRow.getValue(index) !== ValueNull.INSTANCE }
                    .not()
            }
            else -> false
        }
    }
}