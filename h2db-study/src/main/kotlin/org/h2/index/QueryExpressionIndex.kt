package org.h2.index

import org.h2.command.query.Query
import org.h2.engine.SessionLocal
import org.h2.expression.Parameter
import org.h2.expression.condition.Comparison
import org.h2.result.SortOrder
import org.h2.table.Column
import org.h2.table.IndexColumn
import org.h2.table.QueryExpressionTable
import org.h2.table.TableFilter
import org.h2.util.HasSQL

/**
 * This object represents a virtual index for a query expression.
 */
open class QueryExpressionIndex(
        override val table: QueryExpressionTable,
        private val querySQL: String? = null,
        private val originalParameters: ArrayList<Parameter>? = null,
        private var recursive: Boolean = false) : Index(table = table,
        id = 0,
        name = null,
        indexColumns = null,
        uniqueColumnColumn = 0,
        indexType = IndexType.createNonUnique(false)), SpatialIndex {

    private var createSession: SessionLocal? = null
    private var indexMasks: IntArray? = null

    private var query: Query? = null

    /**
     * The time in nanoseconds when this index (and its cost) was calculated.
     */
    private var evaluatedAt: Long = Long.MIN_VALUE

    init {
        columns = emptyArray()
    }

    /**
     * Constructor for plan item generation. Over this index the query will be
     * executed.
     *
     * @param table the query expression table
     * @param index the main index
     * @param session the session
     * @param masks the masks
     * @param filters table filters
     * @param filter current filter
     * @param sortOrder sort order
     */
    constructor(table: QueryExpressionTable,
                index: QueryExpressionIndex,
                session: SessionLocal,
                masks: IntArray?,
                filters: Array<TableFilter?>?,
                filter: Int,
                sortOrder: SortOrder?) : this(table = table,
            querySQL = index.querySQL,
            originalParameters = index.originalParameters,
            recursive = index.recursive) {

        indexMasks = masks
        createSession = session
        if (!recursive) {
            query = getQuery(session, masks)
        }
        evaluatedAt = if (recursive || table.topQuery != null) {
            Long.MAX_VALUE
        } else {
            var time = System.nanoTime()
            if (time == Long.MAX_VALUE) {
                time++
            }
            time
        }
    }

    private fun getQuery(session: SessionLocal, masks: IntArray?): Query {
        var q = session.prepareQueryExpression(querySQL)
        if (masks == null || !q.allowGlobalConditions()) {
            q.preparePlan()
            return q
        }
        val firstIndexParam = table.getParameterOffset(originalParameters)
        // the column index of each parameter
        // (for example: paramColumnIndex {0, 0} mean
        // param[0] is column 0, and param[1] is also column 0)
        val paramColumnIndex = org.h2.util.IntArray()
        var indexColumnCount = 0
        for (i in masks.indices) {
            val mask = masks[i]
            if (mask == 0) {
                continue
            }
            indexColumnCount++
            // the number of parameters depends on the mask;
            // for range queries it is 2: >= x AND <= y
            // but bitMask could also be 7 (=, and <=, and >=)
            val bitCount = Integer.bitCount(mask)
            for (j in 0 until bitCount) {
                paramColumnIndex.add(i)
            }
        }
        val len = paramColumnIndex.size()
        val columnList = java.util.ArrayList<Column>(len)
        var i = 0
        while (i < len) {
            val idx = paramColumnIndex[i]
            columnList.add(table.getColumn(idx))
            val mask = masks[idx]
            if (mask and IndexCondition.EQUALITY != 0) {
                val param = Parameter(firstIndexParam + i)
                q.addGlobalCondition(param, idx, Comparison.EQUAL_NULL_SAFE)
                i++
            }
            if (mask and IndexCondition.START != 0) {
                val param = Parameter(firstIndexParam + i)
                q.addGlobalCondition(param, idx, Comparison.BIGGER_EQUAL)
                i++
            }
            if (mask and IndexCondition.END != 0) {
                val param = Parameter(firstIndexParam + i)
                q.addGlobalCondition(param, idx, Comparison.SMALLER_EQUAL)
                i++
            }
            if (mask and IndexCondition.SPATIAL_INTERSECTS != 0) {
                val param = Parameter(firstIndexParam + i)
                q.addGlobalCondition(param, idx, Comparison.SPATIAL_INTERSECTS)
                i++
            }
        }
        columns = columnList.toTypedArray<Column>()

        // reconstruct the index columns from the masks
        indexColumns = arrayOfNulls<>(indexColumnCount)
        columnIds = IntArray(indexColumnCount)
        var type = 0
        var indexColumnId: Int = 0
        while (type < 2) {
            for (i in masks.indices) {
                val mask = masks[i]
                if (mask == 0) {
                    continue
                }
                if (type == 0) {
                    if (mask and IndexCondition.EQUALITY == 0) {
                        // the first columns need to be equality conditions
                        continue
                    }
                } else {
                    if (mask and IndexCondition.EQUALITY != 0) {
                        // after that only range conditions
                        continue
                    }
                }
                val column = table.getColumn(i)
                indexColumns!![indexColumnId] = IndexColumn(column)
                columnIds!![indexColumnId] = column.columnId
                indexColumnId++
            }
            type++
        }
        val sql = q.getPlanSQL(HasSQL.DEFAULT_SQL_FLAGS)
        if (sql != querySQL) {
            q = session.prepareQueryExpression(sql)
        }
        q.preparePlan()
        return q
    }
}