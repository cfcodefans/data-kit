package org.h2.table

import org.h2.api.ErrorCode
import org.h2.command.query.AllColumnsForPlan
import org.h2.constraint.Constraint
import org.h2.engine.CastDataProvider
import org.h2.engine.SessionLocal
import org.h2.index.Index
import org.h2.index.IndexType
import org.h2.message.DbException
import org.h2.message.Trace
import org.h2.result.Row
import org.h2.result.SortOrder
import org.h2.schema.Schema
import org.h2.schema.SchemaObject
import org.h2.schema.Sequence
import org.h2.schema.TriggerObject
import org.h2.value.CompareMode
import org.h2.value.Value

/**
 * This is the base class for most tables.
 * A table contains a list of columns and a list of rows.
 */
abstract class Table(
    schema: Schema,
    id: Int,
    name: String,
    var persistIndexes: Boolean?,
    var persistData: Boolean?) : SchemaObject(schema = schema,
    id = id,
    name = name,
    traceModuleId = Trace.TABLE) {

    val columnMap: HashMap<String, Column> = schema.database.newStringMap()

    /**
     * The compare mode used for this table.
     */
    open var comparedMode: CompareMode = schema.database.compareMode

    var constraints: ArrayList<Constraint>? = null

    /** Is foreign key constraint checking enabled for this table.  */
    private var checkForeignKeyConstraints: Boolean = true

    var onCommitDrop: Boolean = false
    var onCommitTruncate: Boolean = false

    override fun rename(newName: String) {
        super.rename(newName)
        constraints?.forEach { constraint ->
            constraint.rebuild()
        }
    }

    open fun isView(): Boolean = false

    /**
     * The columns of this table.
     */
    protected abstract var columns: Array<Column>
    private var triggers: ArrayList<TriggerObject>? = null
    private var sequences: ArrayList<Sequence>? = null

    /**
     * Lock the table for the given session.
     * This method waits until the lock is granted.
     * @param session the session
     * @param lockType the type of lock
     * @return true if the table was already exclusively locked by this session.
     * @throws DbException if a lock timeout occurred
     */
    open fun lock(session: SessionLocal?, lockType: Int): Boolean = false

    /**
     * Close the table object and flush changes.
     * @param session the session
     */
    abstract fun close(session: SessionLocal?)

    /**
     * Release the lock for this session.
     * @param s the session
     */
    open fun unlock(s: SessionLocal?) {}

    /**
     * Create an index for this table
     *
     * @param session the session
     * @param indexName the name of the index
     * @param indexId the id
     * @param cols the index columns
     * @param uniqueColumnCount the count of unique columns
     * @param indexType the index type
     * @param create whether this is a new index
     * @param indexComment the comment
     * @return the index
     */
    abstract fun addIndex(session: SessionLocal?,
                          indexName: String?,
                          indexId: Int,
                          cols: Array<IndexColumn?>?,
                          uniqueColumnCount: Int,
                          indexType: IndexType?,
                          create: Boolean,
                          indexComment: String?): Index?

    /**
     * Get the given row.
     *
     * @param session the session
     * @param key the primary key
     * @return the row
     */
    open fun getRow(session: SessionLocal?, key: Long): Row? = null

    /**
     * Returns whether this table is insertable.
     * @return whether this table is insertable
     */
    open fun isInsertable(): Boolean = true

    /**
     * Remove a row from the table and all indexes.
     * @param session the session
     * @param row the row
     */
    abstract fun removeRow(session: SessionLocal?, row: Row?)

    /**
     * Locks row, preventing any updated to it, except from the session specified.
     *
     * @param session the session
     * @param row to lock
     * @return locked row, or null if row does not exist anymore
     */
    open fun lockRow(session: SessionLocal?, row: Row?): Row? {
        throw DbException.getUnsupportedException("lockRow()")
    }

    /**
     * Remove all rows from the table and indexes.
     *
     * @param session the session
     * @return number of removed rows, possibly including uncommitted rows
     */
    abstract fun truncate(session: SessionLocal?): Long

    /**
     * Add a row to the table and all indexes.
     *
     * @param session the session
     * @param row the row
     * @throws DbException if a constraint was violated
     */
    abstract fun addRow(session: SessionLocal?, row: Row?)

    /**
     * Update a row to the table and all indexes.
     *
     * @param session the session
     * @param oldRow the row to update
     * @param newRow the row with updated values (_rowid_ suppose to be the same)
     * @throws DbException if a constraint was violated
     */
    open fun updateRow(session: SessionLocal?, oldRow: Row, newRow: Row) {
        newRow.key = oldRow.key
        removeRow(session, oldRow)
        addRow(session, newRow)
    }

    /**
     * Check if this table supports ALTER TABLE.
     * @throws DbException if it is not supported
     */
    abstract fun checkSupportAlter()

    /**
     * Get the table type name
     * @return the table type name
     */
    abstract fun getTableType(): TableType?

    /**
     * Return SQL table type for INFORMATION_SCHEMA.
     *
     * @return SQL table type for INFORMATION_SCHEMA
     */
    open fun getSQLTableType(): String? {
        if (isView()) return "VIEW"
        return if (super.temporary) {
            if (isGlobalTemporary()) "GLOBAL TEMPORARY" else "LOCAL TEMPORARY"
        } else
            "BASE TABLE"
    }

    open fun isGlobalTemporary(): Boolean = false

    /**
     * Check if this table can be truncated.
     * @return true if it can
     */
    open fun canTruncate(): Boolean = false

    /**
     * Enable or disable foreign key constraint checking for this table.
     *
     * @param session the session
     * @param enabled true if checking should be enabled
     * @param checkExisting true if existing rows must be checked during this
     * call
     */
    open fun setCheckForeignKeyConstraints(session: SessionLocal?, enabled: Boolean, checkExisting: Boolean) {
        if (enabled && checkExisting) {
            constraints?.forEach { c ->
                if (c.constraintType == Constraint.Type.REFERENTIAL) {
                    c.checkExistingData(session)
                }
            }
        }
        checkForeignKeyConstraints = enabled
    }

    /**
     * Get the index that has the given column as the first element.
     * This method returns null if no matching index is found.
     *
     * @param column the column
     * @param needGetFirstOrLast if the returned index must be able
     * to do [Index.canGetFirstOrLast]
     * @param needFindNext if the returned index must be able to do
     * [Index.findNext]
     * @return the index or null
     */
    open fun getIndexForColumn(column: Column?,
                               needGetFirstOrLast: Boolean, needFindNext: Boolean): Index? {
        val indexes: ArrayList<Index> = getIndexes() ?: return null

        return indexes.filterNot { index: Index -> needGetFirstOrLast && !index.canGetFirstOrLast() }
            .filterNot { index: Index -> needFindNext && !index.canFindNext() }
            .filter { index: Index -> index.isFirstColumn(column) }
            // choose the minimal covering index with the needed first
            // column to work consistently with execution plan from
            // Optimizer
            .minBy { index: Index -> index.columns.size }
    }

    /**
     * If the index is still required by a constraint, transfer the ownership to
     * it. Otherwise, the index is removed.
     *
     * @param session the session
     * @param index the index that is no longer required
     */
    open fun removeIndexOrTransferOwnership(session: SessionLocal?, index: Index?) {
        var stillNeeded = false

        constraints?.filter { cons -> cons.usesIndex(index) }
            ?.forEach { cons ->
                cons.setIndexOwner(index)
                database!!.updateMeta(session, cons)
                stillNeeded = true
            }

        if (!stillNeeded) {
            database!!.removeSchemaObject(session, index)
        }
    }

    /**
     * Removes dependencies of column expressions, used for tables with circular
     * dependencies.
     *
     * @param session the session
     */
    open fun removeColumnExpressionsDependencies(session: SessionLocal?) {
        for (column in columns) {
            column.setDefaultExpression(session, null)
            column.setOnUpdateExpression(session, null)
        }
    }


    /**
     * Check if a deadlock occurred. This method is called recursively. There is
     * a circle if the session to be tested has already being visited. If this
     * session is part of the circle (if it is the clash session), the method
     * must return an empty object array. Once a deadlock has been detected, the
     * methods must add the session to the list. If this session is not part of
     * the circle, or if no deadlock is detected, this method returns null.
     *
     * @param session the session to be tested for
     * @param clash set with sessions already visited, and null when starting
     * verification
     * @param visited set with sessions already visited, and null when starting
     * verification
     * @return an object array with the sessions involved in the deadlock, or
     * null
     */
    open fun checkDeadlock(session: SessionLocal?,
                           clash: SessionLocal?,
                           visited: Set<SessionLocal>?): ArrayList<SessionLocal>? {
        return null
    }

    /**
     * Compare two values with the current comparison mode. The values may be of
     * different type.
     *
     * @param provider the cast information provider
     * @param a the first value
     * @param b the second value
     * @return 0 if both values are equal, -1 if the first value is smaller, and
     * 1 otherwise
     */
    open fun compareValues(provider: CastDataProvider?,
                           a: Value,
                           b: Value?): Int = a.compareTo(b!!, provider, this.comparedMode)

    /**
     * Tests if the table can be written. Usually, this depends on the
     * database.checkWritingAllowed method, but some tables (eg. TableLink)
     * overwrite this default behaviour.
     */
    open fun checkWritingAllowed() = database!!.checkWritingAllowed()

    /**
     * Get all indexes for this table.
     *
     * @return the list of indexes
     */
    abstract fun getIndexes(): ArrayList<Index>?

    override fun getType(): Int = TABLE_OR_VIEW

    /**
     * Get the column at the given index.
     *
     * @param index the column index (0, 1,...)
     * @return the column
     */
    open fun getColumn(index: Int): Column? = columns[index]

    /**
     * Get the column with the given name.
     *
     * @param columnName the column name
     * @return the column
     * @throws DbException if the column was not found
     */
    open fun getColumn(columnName: String?): Column? = columnMap[columnName] ?: throw DbException.get(ErrorCode.COLUMN_NOT_FOUND_1, columnName!!)

    /**
     * Get the column with the given name.
     *
     * @param columnName the column name
     * @param ifExists if `true` return `null` if column does not exist
     * @return the column
     * @throws DbException if the column was not found
     */
    open fun getColumn(columnName: String?, ifExists: Boolean): Column? {
        val column = columnMap[columnName]
        if (column == null && !ifExists) {
            throw DbException.get(ErrorCode.COLUMN_NOT_FOUND_1, columnName!!)
        }
        return column
    }

    /**
     * Get the column with the given name if it exists.
     *
     * @param columnName the column name, or `null`
     * @return the column
     */
    open fun findColumn(columnName: String?): Column? {
        return columnMap[columnName]
    }

    /**
     * Does the column with the given name exist?
     *
     * @param columnName the column name
     * @return true if the column exists
     */
    open fun doesColumnExist(columnName: String?): Boolean {
        return columnMap.containsKey(columnName)
    }

    /**
     * Returns first identity column, or `null`.
     *
     * @return first identity column, or `null`
     */
    open fun getIdentityColumn(): Column? = columns.firstOrNull { it.isIdentity() }

    /**
     * Get the best plan for the given search mask.
     *
     * @param session the session
     * @param masks per-column comparison bit masks, null means 'always false',
     * see constants in IndexCondition
     * @param filters all joined table filters
     * @param filter the current table filter index
     * @param sortOrder the sort order
     * @param allColumnsSet the set of all columns
     * @return the plan item
     */
    open fun getBestPlanItem(session: SessionLocal, masks: IntArray?,
                             filters: Array<TableFilter?>?, filter: Int, sortOrder: SortOrder?,
                             allColumnsSet: AllColumnsForPlan?): PlanItem? {
        val item = PlanItem()
        item.setIndex(getScanIndex(session))
        item.cost = item.getIndex().getCost(session, null, filters, filter, null, allColumnsSet)
        val t = session.trace
        if (t.isDebugEnabled) {
            t.debug("Table      :     potential plan item cost {0} index {1}",
                item.cost, item.getIndex().getPlanSQL())
        }
        val indexes = getIndexes()
        val indexHints: IndexHints = Table.getIndexHints(filters, filter)
        if (indexes != null && masks != null) {
            var i = 1
            val size = indexes.size
            while (i < size) {
                val index = indexes[i]
                if (Table.isIndexExcludedByHints(indexHints, index)) {
                    i++
                    continue
                }
                val cost = index.getCost(session, masks, filters, filter,
                    sortOrder, allColumnsSet)
                if (t.isDebugEnabled) {
                    t.debug("Table      :     potential plan item cost {0} index {1}",
                        cost, index.planSQL)
                }
                if (cost < item.cost) {
                    item.cost = cost
                    item.setIndex(index)
                }
                i++
            }
        }
        return item
    }
}