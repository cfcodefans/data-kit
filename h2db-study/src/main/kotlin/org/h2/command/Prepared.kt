package org.h2.command

import org.h2.api.DatabaseEventListener
import org.h2.api.ErrorCode
import org.h2.engine.DbObject
import org.h2.engine.SessionLocal
import org.h2.expression.Expression
import org.h2.expression.Parameter
import org.h2.message.DbException
import org.h2.message.Trace
import org.h2.result.ResultInterface
import org.h2.table.TableView
import org.h2.util.HasSQL

/**
 * A prepared statement.
 */
abstract class Prepared(open var session: SessionLocal) {

    /**
     * The SQL string.
     */
    open var sqlStatement: String? = null

    /**
     * Whether to create a new object (for indexes).
     */
    protected var create = true

    /**
     * The list of parameters.
     */
    var parameters: ArrayList<Parameter>? = null

    /**
     * If the query should be prepared before each execution. This is set for
     * queries with LIKE ?, because the query plan depends on the parameter
     * value.
     */
    protected var prepareAlways = false

    private val command: Command? = null

    /**
     * Used to preserve object identities on database startup. `0` if
     * object is not stored, `-1` if object is stored and its ID is
     * already read, `>0` if object is stored and its id is not yet read.
     */
    private var persistedObjectId = 0
    private var currentRowNumber: Long = 0
    private var rowScanCount = 0

    /**
     * Common table expressions (CTE) in queries require us to create temporary views,
     * which need to be cleaned up once a command is done executing.
     */
    open var cteCleanups: List<TableView>? = null

    var modificationMetaId: Long = session.database.modificationMetaId

    /**
     * Check if this command is transactional.
     * If it is not, then it forces the current transaction to commit.
     *
     * @return true if it is
     */
    abstract fun isTransactional(): Boolean

    /**
     * Get an empty result set containing the meta data.
     *
     * @return the result set
     */
    abstract fun queryMeta(): ResultInterface?

    /**
     * Get the command type as defined in CommandInterface
     *
     * @return the statement type
     */
    abstract fun getType(): Int

    /**
     * Check if this command is read only.
     *
     * @return true if it is
     */
    open fun isReadOnly(): Boolean = false

    /**
     * Check if the statement needs to be re-compiled.
     *
     * @return true if it must
     */
    open fun needRecompile(): Boolean {
        val db = session.database ?: throw DbException.get(ErrorCode.CONNECTION_BROKEN_1, "database closed")
        // parser: currently, compiling every create/drop/... twice
        // because needRecompile return true even for the first execution
        return prepareAlways
                || modificationMetaId < db.modificationMetaId
                || db.settings.recompileAlways
    }

    /**
     * Check if all parameters have been set.
     *
     * @throws DbException if any parameter has not been set
     */
    protected open fun checkParameters() {
        if (persistedObjectId < 0) {
            // restore original persistedObjectId on Command re-run
            // i.e. due to concurrent update
            persistedObjectId = persistedObjectId.inv()
        }
        parameters?.forEach { it.checkSet() }
    }

    /**
     * Check if this object is a query.
     *
     * @return true if it is
     */
    open fun isQuery(): Boolean = false

    /**
     * Prepare this statement.
     */
    open fun prepare() {
        // nothing to do
    }

    /**
     * Execute the statement.
     *
     * @return the update count
     * @throws DbException if it is a query
     */
    open fun update(): Long = throw DbException.get(ErrorCode.METHOD_NOT_ALLOWED_FOR_QUERY)

    /**
     * Execute the query.
     *
     * @param maxrows the maximum number of rows to return
     * @return the result set
     * @throws DbException if it is not a query
     */
    open fun query(maxrows: Long): ResultInterface? = throw DbException.get(ErrorCode.METHOD_ONLY_ALLOWED_FOR_QUERY)

    /**
     * Get the object id to use for the database object that is created in this
     * statement. This id is only set when the object is already persisted.
     * If not set, this method returns 0.
     *
     * @return the object id or 0 if not set
     */
    open fun getPersistedObjectId(): Int = if (persistedObjectId >= 0) persistedObjectId else 0

    /**
     * Get the current object id, or get a new id from the database. The object
     * id is used when creating new database object (CREATE statement). This
     * method may be called only once.
     *
     * @return the object id
     */
    protected open fun getObjectId(): Int {
        val id = when {
            persistedObjectId == 0 -> session.database.allocateObjectId()
            persistedObjectId < 0 -> throw DbException.getInternalError("Prepared.getObjectId() was called before")
            else -> persistedObjectId
        }
        persistedObjectId = persistedObjectId.inv() // while negative, it can be restored later
        return id
    }

    /**
     * Get the SQL statement with the execution plan.
     *
     * @param sqlFlags formatting flags
     * @return the execution plan
     */
    open fun getPlanSQL(sqlFlags: Int): String? = null

    /**
     * Check if this statement was canceled.
     *
     * @throws DbException if it was canceled
     */
    open fun checkCanceled() {
        session.checkCanceled()
        (command ?: session.currentCommand)?.checkCanceled()
    }

    /**
     * Set the persisted object id for this statement.
     *
     * @param i the object id
     */
    open fun setPersistedObjectId(i: Int) = apply {
        persistedObjectId = i
        create = false
    }

    /**
     * Print information about the statement executed if info trace level is
     * enabled.
     *
     * @param startTimeNanos when the statement was started
     * @param rowCount the query or update row count
     */
    open fun trace(startTimeNanos: Long, rowCount: Long) {
        if (session.trace.isInfoEnabled && startTimeNanos > 0) {
            val deltaTimeNanos = System.nanoTime() - startTimeNanos
            val params = Trace.formatParams(parameters!!)
            session.trace.infoSQL(sqlStatement, params, rowCount, deltaTimeNanos / 1000000L)
        }
        // startTime_nanos can be zero for the command that actually turns on
        // statistics
        if (session.database.queryStatistics && startTimeNanos != 0L) {
            val deltaTimeNanos = System.nanoTime() - startTimeNanos
            session.database.queryStatisticsData.update(toString(), deltaTimeNanos, rowCount)
        }
    }

    /**
     * Set the current row number.
     *
     * @param rowNumber the row number
     */
    open fun setCurrentRowNumber(rowNumber: Long) {
        if (++rowScanCount and 127 == 0) {
            checkCanceled()
        }
        currentRowNumber = rowNumber
        setProgress()
    }

    /**
     * Notifies query progress via the DatabaseEventListener
     */
    private fun setProgress() {
        if (currentRowNumber and 127L == 0L) {
            session.database.setProgress(DatabaseEventListener.STATE_STATEMENT_PROGRESS, sqlStatement,
                                         currentRowNumber, 0L)
        }
    }

    /**
     * Convert the statement to a String.
     *
     * @return the SQL statement
     */
    override fun toString(): String = sqlStatement ?: ""

    /**
     * Get the SQL snippet of the expression list.
     *
     * @param list the expression list
     * @return the SQL snippet
     */
    open fun getSimpleSQL(list: Array<Expression?>?): String? {
        return Expression.writeExpressions(StringBuilder(), list!!, HasSQL.TRACE_SQL_FLAGS).toString()
    }

    /**
     * Set the SQL statement of the exception to the given row.
     *
     * @param e the exception
     * @param rowId the row number
     * @param values the values of the row
     * @return the exception
     */
    protected open fun setRow(e: DbException, rowId: Long, values: String?): DbException? {
        val buff = StringBuilder()
        if (sqlStatement != null) buff.append(sqlStatement)
        buff.append(" -- ")
        if (rowId > 0) {
            buff.append("row #").append(rowId + 1).append(' ')
        }
        buff.append('(').append(values).append(')')
        return e.addSQL(buff.toString())
    }

    open fun isCacheable(): Boolean = false

    /**
     * Find and collect all DbObjects, this Prepared depends on.
     * @param dependencies collection of dependencies to populate
     */
    open fun collectDependencies(dependencies: HashSet<DbObject?>?) {}
}