package org.h2.table

import org.h2.api.ErrorCode
import org.h2.command.query.AllColumnsForPlan
import org.h2.command.query.Select
import org.h2.engine.Database
import org.h2.engine.Right
import org.h2.engine.SessionLocal
import org.h2.expression.Expression
import org.h2.expression.condition.ConditionAndOr
import org.h2.index.Index
import org.h2.index.IndexCondition
import org.h2.index.IndexCursor
import org.h2.message.DbException
import org.h2.result.Row
import org.h2.result.SearchRow
import org.h2.result.SortOrder
import org.h2.util.HasSQL
import org.h2.util.ParserUtil
import org.h2.util.StringUtils
import org.h2.util.Utils
import org.h2.value.TypeInfo
import org.h2.value.Value
import org.h2.value.ValueBigint
import org.h2.value.ValueInteger
import org.h2.value.ValueNull
import org.h2.value.ValueSmallint
import org.h2.value.ValueTinyint

/**
 * A visitor for table filters.
 * This method is called for each nested or joined table filter.
 * @param f the filter
 */
typealias TableFilterVisitor = (TableFilter) -> Unit

/**
 * A table filter represents a table that is used in a query. There is one such
 * object whenever a table (or view) is used in a query. For example the
 * following query has 2 table filters: SELECT * FROM TEST T1, TEST T2.
 *
 *     * Create a new table filter object.
 *
 * @param session the session
 * @param table the table from where to read data
 * @param alias the alias name
 * @param rightsChecked true if rights are already checked
 * @param select the select statement
 * @param orderInFrom original order number (index) of this table filter in
 * @param indexHints the index hints to be used by the query planner
 */
open class TableFilter(private var session: SessionLocal,
                       private val table: Table,
                       private var alias: String?,
                       rightsChecked: Boolean,
                       override val select: Select?,
                       private val orderInFrom: Int = 0,
                       val indexHints: IndexHints?) : ColumnResolver {

    init {
        if (rightsChecked.not()) session.user.checkTableRight(table, Right.SELECT)
    }

    /**
     * Whether this is a direct or indirect (nested) outer join
     */
    protected var joinOuterIndirect = false

    private var index: Index? = null

    var masks: IntArray? = null
    private var scanCount: Int = 0
    var evaluatable: Boolean = false

    /**
     * Indicates that this filter is used in the plan.
     */
    var used: Boolean = false

    /**
     * Additional conditions that can't be used for index lookup, but for row
     * filter for this table (ID=ID, NAME LIKE '%X%')
     */
    private var filterCondition: Expression? = null

    /**
     * The complete join condition.
     */
    private var joinCondition: Expression? = null

    private var currentSearchRow: SearchRow? = null
    private var current: Row? = null
    private var state = 0

    private var foundOne: Boolean = false
    private var fullCondition: Expression? = null
    private val hashCode: Int = session.nextObjectId()

    /**
     * The joined table (if there is one).
     */
    private var join: TableFilter? = null

    /**
     * Whether this is an outer join.
     */
    private var joinOuter = false

    /**
     * The nested joined table (if there is one).
     */
    private var nestedJoin: TableFilter? = null

    /**
     * Map of common join columns, used for NATURAL joins and USING clause of
     * other joins. This map preserves original order of the columns.
     */
    private var commonJoinColumns: java.util.LinkedHashMap<Column, Column>? = null
    private var commonJoinColumnsFilter: TableFilter? = null
    private val commonJoinColumnsToExclude: java.util.ArrayList<Column>? = null

    /**
     * Visit this and all joined or nested table filters.
     *
     * @param visitor the visitor
     */
    open fun visit(visitor: TableFilterVisitor) {
        var f: TableFilter? = this
        do {
            visitor(f!!)
            f.nestedJoin?.visit(visitor)
            f = f.join
        } while (f != null)
    }

    /**
     * The filter used to walk through the index.
     */
    var cursor: IndexCursor? = IndexCursor()
        private set

    /**
     * Map of derived column names. This map preserves original order of the
     * columns.
     */
    private var derivedColumnMap: LinkedHashMap<Column, String>? = null

    companion object {
        private const val BEFORE_FIRST = 0
        private const val FOUND = 1
        private const val AFTER_LAST = 2
        private const val NULL_ROW = 3

        /**
         * A visitor that sets joinOuterIndirect to true.
         */
        private val JOI_VISITOR: TableFilterVisitor = { f: TableFilter -> f.joinOuterIndirect = true }

        private class MapColumnsVisitor(private val on: Expression) : TableFilterVisitor {
            override fun invoke(f: TableFilter) = on.mapColumns(f, 0, Expression.MAP_INITIAL)
        }
    }

    /**
     * Lock the table. This will also lock joined tables.
     *
     * @param s the session
     */
    open fun lock(s: SessionLocal?) {
        table.lock(s, false, false)
        join?.lock(s)
    }


    /**
     * The index conditions used for direct index lookup (start or end).
     */
    private val indexConditions: ArrayList<IndexCondition> = Utils.newSmallArrayList<IndexCondition>()

    /**
     * Get the best plan item (index, cost) to use for the current join order.
     *
     * @TODO hard to understand
     *
     * @param s the session
     * @param filters all joined table filters
     * @param filter the current table filter index
     * @param allColumnsSet the set of all columns
     * @return the best plan item
     */
    open fun getBestPlanItem(s: SessionLocal?,
                             filters: Array<TableFilter>,
                             filter: Int,
                             allColumnsSet: AllColumnsForPlan?): PlanItem? {
        var filter = filter
        var sortOrder: SortOrder? = select?.sortOrder

        var item1: PlanItem? = null

        if (indexConditions.isEmpty()) {
            item1 = PlanItem(index = table.getScanIndex(s, null, filters, filter, sortOrder, allColumnsSet))
                .apply { cost = index!!.getCost(s, null, filters, filter, sortOrder, allColumnsSet) }
        }

        var masks: IntArray? = IntArray(table.getColumns().size)

        for (condition in indexConditions) {
            if (!condition.isEvaluatable) continue
            if (condition.isAlwaysFalse) {
                masks = null
                break
            }
            val id = condition.column.columnId
            if (id >= 0) {
                masks!![id] = masks[id] or condition.getMask(indexConditions)
            }
        }

        var item = table.getBestPlanItem(s, masks, filters, filter, sortOrder, allColumnsSet)
        item.masks = masks
        // The more index conditions, the earlier the table.
        // This is to ensure joins without indexes run quickly:
        // x (x.a=10); y (x.b=y.b) - see issue 113
        item.cost -= item.cost * indexConditions.size / 100 / (filter + 1)
        if (item1 != null && item1.cost < item.cost) {
            item = item1
        }

        if (nestedJoin != null) {
            evaluatable = true
            item.nestedJoinPlan = nestedJoin!!.getBestPlanItem(s, filters, filter, allColumnsSet)
            // TODO optimizer: calculate cost of a join: should use separate
            // expected row number and lookup cost
            item.cost += item.cost * item.nestedJoinPlan.cost
        }
        if (join != null) {
            evaluatable = true
            do {
                filter++
            } while (filters[filter] !== join)
            item.joinPlan = join!!.getBestPlanItem(s, filters, filter, allColumnsSet)
            // TODO optimizer: calculate cost of a join: should use separate
            // expected row number and lookup cost
            item.cost += item.cost * item.joinPlan.cost
        }
        return item
    }

    /**
     * Set what plan item (index, cost, masks) to use.
     *
     * @param item the plan item
     */
    open fun setPlanItem(item: PlanItem?) {
        // invalid plan, most likely because a column wasn't found
        // this will result in an exception later on
        if (item == null) return

        index = item.index
        masks = item.masks
        if (nestedJoin != null) {
            if (item.nestedJoinPlan != null) {
                nestedJoin!!.setPlanItem(item.nestedJoinPlan)
            } else {
                nestedJoin!!.setScanIndexes()
            }
        }
        if (join != null) {
            if (item.joinPlan != null) {
                join!!.setPlanItem(item.joinPlan)
            } else {
                join!!.setScanIndexes()
            }
        }
    }

    open fun setIndex(index: Index?) {
        this.index = index
        cursor!!.setIndex(index)
    }

    /**
     * Set all missing indexes to scan indexes recursively.
     */
    private fun setScanIndexes() {
        if (index == null) {
            setIndex(table.getScanIndex(session))
        }
        join?.setScanIndexes()
        nestedJoin?.setScanIndexes()
    }

    /**
     * Prepare reading rows. This method will remove all index conditions that
     * can not be used, and optimize the conditions.
     */
    open fun prepare() {
        // forget all unused index conditions
        // the indexConditions list may be modified here
        var i = 0; while (i < indexConditions.size) {
            val condition = indexConditions[i]
            if (!condition.isAlwaysFalse) {
                val col = condition.column
                if (col.columnId >= 0) {
                    if (index!!.getColumnIndex(col) < 0) {
                        indexConditions.removeAt(i)
                        i--
                    }
                }
            }
            i++; }

        if (nestedJoin != null) {
            if (nestedJoin === this) {
                throw DbException.getInternalError("self join")
            }
            nestedJoin!!.prepare()
        }
        if (join != null) {
            if (join === this) {
                throw DbException.getInternalError("self join")
            }
            join!!.prepare()
        }
        if (filterCondition != null) {
            filterCondition = filterCondition!!.optimizeCondition(session)
        }
        if (joinCondition != null) {
            joinCondition = joinCondition!!.optimizeCondition(session)
        }
    }

    /**
     * Start the query. This will reset the scan counts.
     *
     * @param s the session
     */
    open fun startQuery(s: SessionLocal?) {
        session = s!!
        scanCount = 0
        nestedJoin?.startQuery(s)
        join?.startQuery(s)
    }

    /**
     * Reset to the current position.
     */
    open fun reset() {
        nestedJoin?.reset()
        join?.reset()
        state = BEFORE_FIRST
        foundOne = false
    }

    /**
     * Check if there are more rows to read.
     *
     * @return true if there are
     */
    open fun next(): Boolean {
        if (state == AFTER_LAST) return false
        if (state == BEFORE_FIRST) {
            cursor!!.find(session, indexConditions)
            if (!cursor!!.isAlwaysFalse) {
                nestedJoin?.reset()
                join?.reset()
            }
        } else if (join?.next() == true) return true

        while (true) {
            // go to the next row
            if (state == NULL_ROW) break

            if (cursor!!.isAlwaysFalse) {
                state = AFTER_LAST
            } else if (nestedJoin != null) {
                if (state == BEFORE_FIRST) state = FOUND
            } else {
                if (++scanCount and 4095 == 0) {
                    checkTimeout()
                }
                if (cursor!!.next()) {
                    currentSearchRow = cursor!!.searchRow
                    current = null
                    state = FOUND
                } else {
                    state = AFTER_LAST
                }
            }

            if (nestedJoin != null && state == FOUND) {
                if (!nestedJoin!!.next()) {
                    state = AFTER_LAST
                    // possibly null row
                    if (joinOuter && !foundOne) {
                    } else continue
                }
            }

            // if no more rows found, try the null row (for outer joins only)
            if (state == AFTER_LAST) {
                if (joinOuter && !foundOne) setNullRow() else break
            }

            if (!isOk(filterCondition)) continue

            val joinConditionOk = isOk(joinCondition)
            if (state == FOUND) {
                foundOne = (if (joinConditionOk) true else continue)
            }

            if (join != null) {
                join!!.reset()
                if (!join!!.next()) continue
            }

            // check if it's ok
            if (state == NULL_ROW || joinConditionOk) return true
        }

        state = AFTER_LAST
        return false
    }

    open fun isNullRow(): Boolean = state == NULL_ROW

    /**
     * Set the state of this and all nested tables to the NULL row.
     */
    protected open fun setNullRow() {
        state = NULL_ROW
        current = table.nullRow
        currentSearchRow = current
        nestedJoin?.visit() { obj: TableFilter -> obj.setNullRow() }
    }

    private fun checkTimeout() = session.checkCanceled()

    /**
     * Whether the current value of the condition is true, or there is no
     * condition.
     *
     * @param condition the condition (null for no condition)
     * @return true if yes
     */
    open fun isOk(condition: Expression?): Boolean = condition == null || condition.getBooleanValue(session)

    /**
     * Get the current row.
     *
     * @return the current row, or null
     */
    open fun get(): Row? {
        if (current == null && currentSearchRow != null) {
            current = cursor!!.get()
        }
        return current
    }

    /**
     * Set the current row.
     *
     * @param current the current row
     */
    open fun set(current: Row?) {
        this.current = current
        currentSearchRow = current
    }

    /**
     * Get the table alias name. If no alias is specified, the table name is
     * returned.
     *
     * @return the alias name
     */
    override fun getTableAlias(): String = alias ?: table.name

    /**
     * Add an index condition.
     *
     * @param condition the index condition
     */
    open fun addIndexCondition(condition: IndexCondition?) {
        indexConditions.add(condition!!)
    }

    /**
     * Add a filter condition.
     *
     * @param condition the condition
     * @param isJoin if this is in fact a join condition
     */
    open fun addFilterCondition(condition: Expression, isJoin: Boolean) {
        if (isJoin) {
            joinCondition = joinCondition?.let { ConditionAndOr(ConditionAndOr.AND, it, condition) } ?: condition
        } else {
            filterCondition = filterCondition?.let { ConditionAndOr(ConditionAndOr.AND, it, condition) } ?: condition
        }
    }

    /**
     * Create the index conditions for this filter if needed.
     */
    open fun createIndexConditions() {
        if (joinCondition != null) {
            joinCondition = joinCondition!!.optimizeCondition(session)
            if (joinCondition != null) {
                joinCondition!!.createIndexConditions(session, this)
                if (nestedJoin != null) {
                    joinCondition!!.createIndexConditions(session, nestedJoin)
                }
            }
        }
        join?.createIndexConditions()
        nestedJoin?.createIndexConditions()
    }

    /**
     * Add a joined table.
     *
     * @param filter the joined table filter
     * @param outer if this is an outer join
     * @param on the join condition
     */
    open fun addJoin(filter: TableFilter, outer: Boolean, on: Expression?) {
        if (on != null) {
            on.mapColumns(this, 0, Expression.MAP_INITIAL)
            val visitor: TableFilterVisitor = MapColumnsVisitor(on)
            visit(visitor)
            filter.visit(visitor)
        }
        if (join == null) {
            join = filter
            filter.joinOuter = outer

            if (outer) filter.visit(JOI_VISITOR)
            if (on != null) filter.mapAndAddFilter(on)
        } else {
            join!!.addJoin(filter, outer, on)
        }
    }

    /**
     * Map the columns and add the join condition.
     *
     * @param on the condition
     */
    open fun mapAndAddFilter(on: Expression): TableFilter = apply {
        on.mapColumns(this, 0, Expression.MAP_INITIAL)
        addFilterCondition(on, true)
        nestedJoin?.let { on.mapColumns(it, 0, Expression.MAP_INITIAL) }
        join?.mapAndAddFilter(on)
    }

    override fun getColumns(): Array<Column>? = table.getColumns()

    override fun findColumn(name: String?): Column? {
        if (derivedColumnMap == null) return table.findColumn(name)

        val db: Database = session.database

        return derivedColumnMap!!.entries.find { en ->
            val (key, value) = en
            db.equalsIdentifiers(value, name)
        }?.key
    }

    override fun getValue(column: Column?): Value? {
        if (currentSearchRow == null) return null

        val columnId = column!!.columnId
        if (columnId == -1) return ValueBigint[currentSearchRow!!.key]

        if (current != null) return current!!.getValue(columnId)

        val v: Value? = currentSearchRow!!.getValue(columnId)
        if (v != null) return v

        if (columnId == column.table.mainIndexColumn) {
            return getDelegatedValue(column)
        }

        return cursor!!.get()?.let { it.getValue(columnId) } ?: ValueNull.INSTANCE
    }

    private fun getDelegatedValue(column: Column): Value? {
        val key: Long = currentSearchRow!!.key
        return when (column.type.valueType) {
            Value.TINYINT -> ValueTinyint[key.toByte()]
            Value.SMALLINT -> ValueSmallint.get(key.toShort())
            Value.INTEGER -> ValueInteger.get(key.toInt())
            Value.BIGINT -> ValueBigint[key]
            else -> throw DbException.getInternalError()
        }
    }

    /**
     * Get the query execution plan text to use for this table filter and append
     * it to the specified builder.
     *
     * @param builder string builder to append to
     * @param isJoin if this is a joined table
     * @param sqlFlags formatting flags
     * @return the specified builder
     */
    open fun getPlanSQL(builder: StringBuilder, isJoin: Boolean, sqlFlags: Int): StringBuilder {

        if (isJoin) builder.append(if (joinOuter) "LEFT OUTER JOIN " else "INNER JOIN ")

        if (nestedJoin != null) {
            val buffNested: StringBuilder = StringBuilder()
            var n: TableFilter? = nestedJoin!!; do {
                n!!.getPlanSQL(buffNested, n !== nestedJoin, sqlFlags).append('\n')
                n = n.join
            } while (n != null)

            val nested = buffNested.toString()
            val enclose = !nested.startsWith("(")
            if (enclose) builder.append("(\n")
            StringUtils.indent(builder, nested, 4, false)
            if (enclose) builder.append(')')

            if (isJoin) {
                builder.append(" ON ")
                if (joinCondition == null) {
                    // need to have a ON expression,
                    // otherwise the nesting is unclear
                    builder.append("1=1")
                } else {
                    joinCondition!!.getUnenclosedSQL(builder, sqlFlags)
                }
            }
            return builder
        }

        if (table is TableView && table.isRecursive) {
            table.getSchema().getSQL(builder, sqlFlags).append('.')
            ParserUtil.quoteIdentifier(builder, table.getName(), sqlFlags)
        } else {
            table.getSQL(builder, sqlFlags)
        }

        if (table is TableView && table.isInvalid) {
            throw DbException.get(ErrorCode.VIEW_IS_INVALID_2, table.getName(), "not compiled")
        }

        if (alias != null) {
            builder.append(' ')
            ParserUtil.quoteIdentifier(builder, alias, sqlFlags)
            if (derivedColumnMap != null) {
                builder.append('(')
                var f = false
                for (name in derivedColumnMap!!.values) {
                    if (f) builder.append(", ")
                    f = true
                    ParserUtil.quoteIdentifier(builder, name, sqlFlags)
                }
                builder.append(')')
            }
        }

        if (indexHints != null) {
            builder.append(" USE INDEX (")
            var first = true
            for (index in indexHints.allowedIndexes) {
                if (!first) builder.append(", ") else first = false
                ParserUtil.quoteIdentifier(builder, index, sqlFlags)
            }
            builder.append(")")
        }

        if (index != null && sqlFlags and HasSQL.ADD_PLAN_INFORMATION != 0) {
            builder.append('\n')
            val planBuilder = StringBuilder().append("/* ").append(index!!.planSQL)

            if (indexConditions.isNotEmpty()) {
                planBuilder.append(": ")
                for (i in 0 until indexConditions.size) {
                    if (i > 0) planBuilder.append("\n    AND ")
                    planBuilder.append(indexConditions[i].getSQL(
                        HasSQL.TRACE_SQL_FLAGS or HasSQL.ADD_PLAN_INFORMATION))
                }
            }
            if (planBuilder.indexOf("\n", 3) >= 0) {
                planBuilder.append('\n')
            }
            StringUtils.indent(builder, planBuilder.append(" */").toString(), 4, false)
        }

        if (isJoin) {
            builder.append("\n    ON ")
            if (joinCondition == null) {
                // need to have a ON expression, otherwise the nesting is unclear
                builder.append("1=1")
            } else {
                joinCondition!!.getUnenclosedSQL(builder, sqlFlags)
            }
        }

        if (sqlFlags and HasSQL.ADD_PLAN_INFORMATION != 0) {
            if (filterCondition != null) {
                builder.append('\n')
                var condition = filterCondition!!.getSQL(HasSQL.TRACE_SQL_FLAGS or HasSQL.ADD_PLAN_INFORMATION,
                    Expression.WITHOUT_PARENTHESES)
                condition = "/* WHERE $condition\n*/"
                StringUtils.indent(builder, condition, 4, false)
            }
            if (scanCount > 0) builder.append("\n    /* scanCount: ").append(scanCount).append(" */")
        }
        return builder
    }

    /**
     * Remove all index conditions that are not used by the current index.
     */
    open fun removeUnusableIndexConditions() {
        // the indexConditions list may be modified here
        val iter = indexConditions.listIterator(); while (iter.hasNext()) {
            val cond = iter.next()
            if (cond.getMask(indexConditions) == 0 || !cond.isEvaluatable)
                iter.remove()
        }
    }

    open fun setFullCondition(condition: Expression) {
        this.fullCondition = condition
        join?.setFullCondition(condition)
    }

    /**
     * Optimize the full condition. This will add the full condition to the
     * filter condition.
     */
    open fun optimizeFullCondition() {
        if (joinOuter || fullCondition == null) return

        fullCondition!!.addFilterConditions(this)
        nestedJoin?.optimizeFullCondition()
        join?.optimizeFullCondition()
    }

    /**
     * Update the filter and join conditions of this and all joined tables with
     * the information that the given table filter and all nested filter can now
     * return rows or not.
     *
     * @param filter the table filter
     * @param b the new flag
     */
    open fun setEvaluatable(filter: TableFilter, b: Boolean) {
        filter.evaluatable = b
        filterCondition?.setEvaluatable(filter, b)
        joinCondition?.setEvaluatable(filter, b)

        if (nestedJoin != null) {
            // don't enable / disable the nested join filters
            // if enabling a filter in a joined filter
            if (this === filter) {
                nestedJoin!!.setEvaluatable(nestedJoin!!, b)
            }
        }
        join?.setEvaluatable(filter, b)
    }

    override fun getSchemaName(): String? = if (alias == null && table !is VirtualTable)
        table.schema.name
    else null

    override fun getColumnName(column: Column): String? = derivedColumnMap?.get(column) ?: column.name

    override fun hasDerivedColumnList(): Boolean = derivedColumnMap != null

    /**
     * Get the column with the given name.
     *
     * @param columnName the column name
     * @param ifExists if (@code true) return `null` if column does not exist
     * @return the column
     * @throws DbException
     * if the column was not found and `ifExists` is `false`
     */
    open fun getColumn(columnName: String?, ifExists: Boolean): Column? {
        return if (derivedColumnMap == null) table.getColumn(columnName, ifExists)
        else
            derivedColumnMap!!.let { map ->
                val database = session.database
                map.entries
                    .firstOrNull { en -> database.equalsIdentifiers(columnName, en.value) }
                    ?.key
                    ?: if (ifExists) null else throw DbException.get(ErrorCode.COLUMN_NOT_FOUND_1, columnName!!)
            }
    }

    /**
     * Get the system columns that this table understands. This is used for
     * compatibility with other databases. The columns are only returned if the
     * current mode supports system columns.
     *
     * @return the system columns
     */
    override fun getSystemColumns(): Array<Column>? = if (!session.database.mode.systemColumns)
        null
    else arrayOf(Column("oid", TypeInfo.TYPE_INTEGER, table, 0),  //
        Column("ctid", TypeInfo.TYPE_VARCHAR, table, 0))

    /**
     * Set derived column list.
     *
     * @param derivedColumnNames names of derived columns
     */
    open fun setDerivedColumns(derivedColumnNames: java.util.ArrayList<String>) {
        val columns = getColumns()
        val count = columns!!.size
        if (count != derivedColumnNames.size) throw DbException.get(ErrorCode.COLUMN_COUNT_DOES_NOT_MATCH)

        val map = java.util.LinkedHashMap<Column, String>()
        for ((i, alias) in derivedColumnNames.withIndex()) {
            map.putIfAbsent(columns[i], alias)?.let { throw DbException.get(ErrorCode.DUPLICATE_COLUMN_NAME_1, alias) }
        }
        derivedColumnMap = map
    }

    override fun toString(): String = alias ?: table.toString()

    /**
     * Add a column to the common join column list for a left table filter.
     *
     * @param leftColumn the column on the left side
     * @param replacementColumn the column to use instead, may be the same as column on the
     * left side
     * @param replacementFilter the table filter for replacement columns
     */
    open fun addCommonJoinColumns(leftColumn: Column?, replacementColumn: Column?, replacementFilter: TableFilter) {
        if (commonJoinColumns == null) {
            commonJoinColumns = java.util.LinkedHashMap<Column, Column>()
            commonJoinColumnsFilter = replacementFilter
        } else {
            assert(commonJoinColumnsFilter === replacementFilter)
        }
        commonJoinColumns.put(leftColumn, replacementColumn)
    }
}