package org.h2.command.query

import org.h2.api.ErrorCode
import org.h2.command.Prepared
import org.h2.engine.SessionLocal
import org.h2.expression.*
import org.h2.message.DbException
import org.h2.result.LocalResult
import org.h2.result.ResultInterface
import org.h2.result.ResultTarget
import org.h2.result.SortOrder
import org.h2.table.ColumnResolver
import org.h2.table.Table
import org.h2.table.TableFilter
import org.h2.util.HasSQL
import org.h2.value.ExtTypeInfoRow
import org.h2.value.TypeInfo
import org.h2.value.Value
import org.h2.value.ValueInteger
import org.jetbrains.kotlin.utils.addToStdlib.cast

/**
 * Represents a SELECT statement (simple, or union).
 */
abstract class Query(session: SessionLocal) : Prepared(session) {

    companion object {
        /**
         * Evaluated values of OFFSET and FETCH clauses.
         */
        internal class OffsetFetch(
            /**  OFFSET value. */
            val offset: Long,
            /**  FETCH value. */
            val fetch: Long,
            /** Whether FETCH value is a PERCENT value. */
            val fetchPercent: Boolean)
    }

    /**
     * The column list, including invisible expressions such as order by expressions.
     */
    open var expressions: ArrayList<Expression>? = null

    /**
     * Array of expressions.
     *
     * @see .expressions
     */
    open var expressionArray: Array<Expression>? = null

    /**
     * Describes elements of the ORDER BY clause of a query.
     */
    var orderList: java.util.ArrayList<QueryOrderBy>? = null

    /**
     * A sort order represents an ORDER BY clause in a query.
     */
    var sort: SortOrder? = null

    /**
     * The fetch expression as specified in the FETCH, LIMIT, or TOP clause.
     */
    var fetchExpr: Expression? = null

    /**
     * Whether limit expression specifies percentage of rows.
     */
    var fetchPercent = false

    /**
     * Whether tied rows should be included in result too.
     */
    var withTies = false

    /**
     * The offset expression as specified in the OFFSET clause.
     */
    var offsetExpr: Expression? = null

    /**
     * Whether the result must only contain distinct rows.
     */
    var distinct = false

    /**
     * Whether the result needs to support random access.
     */
    var randomAccessResult = false

    /**
     * The visible columns (the ones required in the result).
     */
    var visibleColumnCount: Int = 0

    /**
     * Number of columns including visible columns and additional virtual
     * columns for ORDER BY and DISTINCT ON clauses. This number does not
     * include virtual columns for HAVING and QUALIFY.
     */
    var resultColumnCount: Int = 0

    var noCache: Boolean = false
    var lastLimit: Long = 0
    var lastEvaluated: Long = 0
    var lastResult: ResultInterface? = null
    var lastParameters: Array<Value>? = null
    var cacheableChecked: Boolean = false
    var neverLazy: Boolean = false
    var checkInit: Boolean = false
    var isPrepared: Boolean = false

    /**
     * Check if this is a UNION query.
     *
     * @return `true` if this is a UNION query
     */
    abstract fun isUnion(): Boolean

    override fun queryMeta(): ResultInterface = LocalResult(session, expressionArray, visibleColumnCount, resultColumnCount).apply { done() }

    /**
     * Execute the query without checking the cache. If a target is specified,
     * the results are written to it, and the method returns null. If no target
     * is specified, a new LocalResult is created and returned.
     *
     * @param limit the limit as specified in the JDBC method call
     * @param target the target to write results to
     * @return the result
     */
    protected abstract fun queryWithoutCache(limit: Long, target: ResultTarget?): ResultInterface?

    private fun queryWithoutCacheLazyCheck(limit: Long, target: ResultTarget): ResultInterface? {
        val disableLazy = neverLazy && session.isLazyQueryExecution
        if (disableLazy) session.isLazyQueryExecution = false

        return try {
            queryWithoutCache(limit, target)
        } finally {
            if (disableLazy) session.isLazyQueryExecution = true
        }
    }

    /**
     * Initialize the query.
     */
    abstract fun init()

    /**
     * Calculate the cost to execute this query.
     *
     * @return the cost
     */
    abstract fun getCost(): Double

    /**
     * Calculate the cost when used as a subquery.
     * This method returns a value between 10 and 1000000,
     * to ensure adding other values can't result in an integer overflow.
     *
     * @return the estimated cost as an integer
     */
    open fun getCostAsExpression(): Int {
        // ensure the cost is not larger than 1 million,
        // so that adding other values can't overflow
        return 1000000.0.coerceAtMost(10.0 + 10.0 * getCost()).toInt()
    }

    /**
     * Get all tables that are involved in this query.
     *
     * @return the set of tables
     */
    abstract fun getTables(): HashSet<Table>?

    /**
     * Whether the query has an order.
     *
     * @return true if it has
     */
    open fun hasOrder(): Boolean = orderList != null || sort != null

    /**
     * Set the 'for update' flag.
     * @param forUpdate the new setting
     */
    abstract fun setForUpdate(forUpdate: Boolean)

    /**
     * Returns data type of rows.
     * @return data type of rows
     */
    open fun getRowDataType(): TypeInfo? = if (visibleColumnCount == 1) {
        expressionArray!![0].type
    } else TypeInfo.getTypeInfo(type = Value.ROW,
                                precision = -1L,
                                scale = -1,
                                ExtTypeInfoRow(expressionArray!!.cast(), visibleColumnCount))

    /**
     * Map the columns to the given column resolver.
     *
     * @param resolver the resolver
     * @param level  the subquery level (0 is the top level query, 1 is the first
     * subquery level)
     */
    abstract fun mapColumns(resolver: ColumnResolver?, level: Int)

    /**
     * Change the evaluatable flag. This is used when building the execution plan.
     *
     * @param tableFilter the table filter
     * @param b the new value
     */
    abstract fun setEvaluatable(tableFilter: TableFilter?, b: Boolean)

    /**
     * Add a condition to the query. This is used for views.
     *
     * @param param the parameter
     * @param columnId the column index (0 meaning the first column)
     * @param comparisonType the comparison type
     */
    abstract fun addGlobalCondition(param: Parameter?,
                                    columnId: Int,
                                    comparisonType: Int)

    /**
     * Check whether adding condition to the query is allowed. This is not
     * allowed for views that have an order by and a limit, as it would affect
     * the returned results.
     *
     * @return true if adding global conditions is allowed
     */
    abstract fun allowGlobalConditions(): Boolean

    /**
     * Check if this expression and all sub-expressions can fulfill a criteria.
     * If any part returns false, the result is false.
     *
     * @param visitor the visitor
     * @return if the criteria can be fulfilled
     */
    abstract fun isEverything(visitor: ExpressionVisitor?): Boolean

    override fun isReadOnly(): Boolean = isEverything(ExpressionVisitor.READONLY_VISITOR)

    /**
     * Update all aggregate function values.
     *
     * @param s the session
     * @param stage select stage
     */
    abstract fun updateAggregate(s: SessionLocal?, stage: Int)

    /**
     * Call the before triggers on all tables.
     */
    abstract fun fireBeforeSelectTriggers()

    /**
     * Set the distinct flag only if it is possible, may be used as a possible
     * optimization only.
     */
    open fun setDistinctIfPossible() {
        if (!isAnyDistinct() && (offsetExpr == null && fetchExpr == null)) {
            distinct = true
        }
    }

    /**
     * @return whether this query is a `DISTINCT` or
     * `DISTINCT ON (...)` query
     */
    open fun isAnyDistinct(): Boolean = distinct

    override fun isQuery(): Boolean = true

    override fun isTransactional(): Boolean = true

    /**
     * Disable caching of result sets.
     */
    open fun disableCache() {
        noCache = true
    }

    private fun sameResultAsLast(params: Array<Value>, lastParams: Array<Value>, lastEval: Long): Boolean {
        if (!cacheableChecked) {
            val max: Long = getMaxDataModificationId()
            noCache = max == Long.MAX_VALUE
            if (!isEverything(ExpressionVisitor.DETERMINISTIC_VISITOR) ||
                !isEverything(ExpressionVisitor.INDEPENDENT_VISITOR)) {
                noCache = true
            }
            cacheableChecked = true
        }
        if (noCache) return false
        
        for (i in params.indices) {
            val a = lastParams[i]
            val b = params[i]
            if (a.getValueType() != b.getValueType() || !session.areEqual(a, b)) {
                return false
            }
        }
        return getMaxDataModificationId() <= lastEval
    }

    fun getMaxDataModificationId(): Long {
        val visitor = ExpressionVisitor.getMaxModificationIdVisitor()
        isEverything(visitor)
        return Math.max(visitor.maxDataModificationId, session.snapshotDataModificationId)
    }

    private fun getParameterValues(): Array<Value> = parameters?.map { it.paramValue }?.toTypedArray() ?: Value.EMPTY_VALUES

    override fun query(maxrows: Long): ResultInterface? = query(maxrows, null)

    /**
     * Execute the query, writing the result to the target result.
     *
     * @param limit the maximum number of rows to return
     * @param target the target result (null will return the result)
     * @return the result set (if the target is not set).
     */
    fun query(limit: Long, target: ResultTarget?): ResultInterface? {
        if (isUnion()) {
            // union doesn't always know the parameter list of the left and
            // right queries
            return queryWithoutCacheLazyCheck(limit, target!!)
        }
        fireBeforeSelectTriggers()
        if (noCache
            || !session.database.optimizeReuseResults
            || session.isLazyQueryExecution && !neverLazy) {
            return queryWithoutCacheLazyCheck(limit, target!!)
        }
        val params = getParameterValues()
        val now = session.database.modificationDataId
        if (isEverything(ExpressionVisitor.DETERMINISTIC_VISITOR)) {
            if (lastResult != null && !lastResult!!.isClosed() && limit == lastLimit) {
                if (sameResultAsLast(params, lastParameters!!, lastEvaluated)) {
                    lastResult = lastResult!!.createShallowCopy(session)
                    if (lastResult != null) {
                        lastResult!!.reset()
                        return lastResult
                    }
                }
            }
        }
        lastParameters = params
        closeLastResult()
        val r = queryWithoutCacheLazyCheck(limit, target!!)
        lastResult = r
        lastEvaluated = now
        lastLimit = limit
        return r
    }

    private fun closeLastResult() = lastResult?.close()

    /**
     * Initialize the order by list. This call may extend the expressions list.
     *
     * @param expressionSQL the select list SQL snippets
     * @param mustBeInResult all order by expressions must be in the select list
     * @param filters the table filters
     * @return `true` if ORDER BY clause is preserved, `false`
     * otherwise
     */
    open fun initOrder(expressionSQL: ArrayList<String>?,
                       mustBeInResult: Boolean,
                       filters: ArrayList<TableFilter>?): Boolean {
        val i = orderList!!.iterator()
        while (i.hasNext()) {
            val o = i.next()
            val e = o.expression ?: continue
            if (e.isConstant) {
                i.remove()
                continue
            }
            val idx: Int = initExpression(expressionSQL, e, mustBeInResult, filters)
            o.columnIndexExpr = ValueExpression.get(ValueInteger.get(idx + 1))
            o.expression = expressions!![idx].getNonAliasExpression()
        }
        if (orderList!!.isEmpty()) {
            orderList = null
            return false
        }
        return true
    }

    /**
     * Initialize the 'ORDER BY' or 'DISTINCT' expressions.
     *
     * @param expressionSQL the select list SQL snippets
     * @param e the expression.
     * @param mustBeInResult all order by expressions must be in the select list
     * @param filters the table filters.
     * @return index on the expression in the [.expressions] list.
     */
    open fun initExpression(expressionSQL: ArrayList<String>?,
                            e: Expression,
                            mustBeInResult: Boolean,
                            filters: ArrayList<TableFilter>?): Int {
        val db = session.database
        // special case: SELECT 1 AS A FROM DUAL ORDER BY A
        // (oracle supports it, but only in order by, not in group by and
        // not in having):
        // SELECT 1 AS A FROM DUAL ORDER BY -A
        if (e is ExpressionColumn) {
            // order by expression
            val exprCol = e
            val tableAlias = exprCol.originalTableAliasName
            val col = exprCol.originalColumnName
            var j = 0
            val visible: Int = visibleColumnCount
            while (j < visible) {
                val ec = expressions!![j]
                if (ec is ExpressionColumn) {
                    // select expression
                    val c = ec
                    if (!db.equalsIdentifiers(col, c.getColumnName(session, j))) {
                        j++
                        continue
                    }
                    if (tableAlias == null) {
                        return j
                    }
                    val ca = c.originalTableAliasName
                    if (ca != null) {
                        if (db.equalsIdentifiers(ca, tableAlias)) {
                            return j
                        }
                    } else if (filters != null) {
                        // select id from test order by test.id
                        for (f in filters) {
                            if (db.equalsIdentifiers(f.tableAlias, tableAlias)) {
                                return j
                            }
                        }
                    }
                } else if (ec is Alias) {
                    if (tableAlias == null && db.equalsIdentifiers(col, ec.getAlias(session, j))) {
                        return j
                    }
                    val ec2 = ec.getNonAliasExpression()
                    if (ec2 is ExpressionColumn) {
                        val c2 = ec2
                        val ta = exprCol.getSQL(HasSQL.DEFAULT_SQL_FLAGS, Expression.WITHOUT_PARENTHESES)
                        val tb = c2.getSQL(HasSQL.DEFAULT_SQL_FLAGS, Expression.WITHOUT_PARENTHESES)
                        val s2 = c2.getColumnName(session, j)
                        if (db.equalsIdentifiers(col, s2) && db.equalsIdentifiers(ta, tb)) {
                            return j
                        }
                    }
                }
                j++
            }
        } else if (expressionSQL != null) {
            val s = e.getSQL(HasSQL.DEFAULT_SQL_FLAGS, Expression.WITHOUT_PARENTHESES)
            var j = 0
            val size = expressionSQL.size
            while (j < size) {
                if (db.equalsIdentifiers(expressionSQL[j], s)) {
                    return j
                }
                j++
            }
        }
        if (expressionSQL == null
            || (mustBeInResult && !db.mode.allowUnrelatedOrderByExpressionsInDistinctQueries
                    && !Query.checkOrderOther(session, e, expressionSQL))) {
            throw DbException.get(ErrorCode.ORDER_BY_NOT_IN_RESULT, e.getTraceSQL())
        }
        val idx = expressions!!.size
        expressions!!.add(e)
        expressionSQL.add(e.getSQL(HasSQL.DEFAULT_SQL_FLAGS, Expression.WITHOUT_PARENTHESES))
        return idx
    }
}