package org.h2.command

import org.h2.api.ErrorCode
import org.h2.engine.Constants
import org.h2.engine.DbObject
import org.h2.engine.Mode.CharPadding
import org.h2.engine.SessionLocal
import org.h2.expression.ParameterInterface
import org.h2.message.DbException
import org.h2.message.Trace
import org.h2.result.ResultInterface
import org.h2.result.ResultWithGeneratedKeys
import org.h2.result.ResultWithPaddedStrings
import org.h2.util.Utils

/**
 * Represents a SQL statement. This object is only used on the server side.
 */
abstract class Command(
        /**
         * The Session
         */
        protected val session: SessionLocal,
        private val sql: String? = null) : CommandInterface {

    /**
     * The last start time
     */
    protected var startTimeNanos: Long = 0

    /**
     * The trace module
     */
    private val trace: Trace = session.database.getTrace(Trace.COMMAND)

    /**
     * If this query was canceled.
     */
    @Volatile
    private var cancel = false

    private var canReuse = false

    /**
     * Check if this command is transactional.
     * If it is not, then it forces the current transaction to commit.
     *
     * @return true if it is
     */
    abstract fun isTransactional(): Boolean

    /**
     * Check if this command is a query.
     *
     * @return true if it is
     */
    abstract override fun isQuery(): Boolean

    /**
     * Get the list of parameters.
     *
     * @return the list of parameters
     */
    abstract override fun getParameters(): ArrayList<out ParameterInterface?>

    /**
     * Check if this command is read only.
     *
     * @return true if it is
     */
    abstract fun isReadOnly(): Boolean

    /**
     * Get an empty result set containing the meta data.
     *
     * @return an empty result set
     */
    abstract fun queryMeta(): ResultInterface?

    /**
     * Execute an updating statement (for example insert, delete, or update), if
     * this is possible.
     *
     * @param generatedKeysRequest
     * `false` if generated keys are not needed, `true` if
     * generated keys should be configured automatically, `int[]`
     * to specify column indices to return generated keys from, or
     * `String[]` to specify column names to return generated keys
     * from
     * @return the update count and generated keys, if any
     * @throws DbException if the command is not an updating statement
     */
    abstract fun update(generatedKeysRequest: Any?): ResultWithGeneratedKeys?

    /**
     * Execute a query statement, if this is possible.
     *
     * @param maxrows the maximum number of rows returned
     * @return the local result set
     * @throws DbException if the command is not a query
     */
    abstract fun query(maxrows: Long): ResultInterface?

    override fun getMetaData(): ResultInterface? = queryMeta()


    /**
     * Start the stopwatch.
     */
    open fun start() {
        if (trace.isInfoEnabled() || session.database.queryStatistics) {
            startTimeNanos = Utils.currentNanoTime()
        }
    }

    open fun setProgress(state: Int) {
        session.database.setProgress(state, sql, 0, 0)
    }

    /**
     * Check if this command has been canceled, and throw an exception if yes.
     *
     * @throws DbException if the statement has been canceled
     */
    open fun checkCanceled() {
        if (cancel) {
            cancel = false
            throw DbException.get(ErrorCode.STATEMENT_WAS_CANCELED)
        }
    }

    override fun stop() {
        commitIfNonTransactional()
        if (isTransactional() && session.autoCommit) session.commit(false)

        if (trace.isInfoEnabled() && startTimeNanos != 0L) {
            val timeMillis = (System.nanoTime() - startTimeNanos) / 1000000L
            if (timeMillis > Constants.SLOW_QUERY_LIMIT_MS) {
                trace.info("slow query: {0} ms", timeMillis)
            }
        }
    }

    private fun commitIfNonTransactional() {
        if (isTransactional()) return

        val autoCommit = session.autoCommit
        session.commit(true)
        if (!autoCommit && session.autoCommit) {
            session.begin()
        }
    }

    private fun filterConcurrentUpdate(e: DbException, start: Long): Long {
        val errorCode = e.getErrorCode()
        if (errorCode != ErrorCode.CONCURRENT_UPDATE_1
                && errorCode != ErrorCode.ROW_NOT_FOUND_IN_PRIMARY_INDEX
                && errorCode != ErrorCode.ROW_NOT_FOUND_WHEN_DELETING_1) {
            throw e
        }
        val now = Utils.currentNanoTime()
        if (start != 0L && now - start > session.lockTimeout * 1000000L) {
            throw DbException.get(ErrorCode.LOCK_TIMEOUT_1, e)
        }
        return if (start == 0L) now else start
    }

    override fun close() {
        canReuse = true
    }

    override fun cancel() {
        cancel = true
    }

    override fun toString(): String = sql + Trace.formatParams(getParameters())

    open fun isCacheable(): Boolean = false

    /**
     * Whether the command is already closed (in which case it can be re-used).
     *
     * @return true if it can be re-used
     */
    open fun canReuse(): Boolean = canReuse

    /**
     * The command is now re-used, therefore reset the canReuse flag, and the
     * parameter values.
     */
    open fun reuse() {
        canReuse = false
        getParameters().forEach { it!!.setValue(null, true) }
    }

    open fun setCanReuse(canReuse: Boolean) {
        this.canReuse = canReuse
    }

    abstract fun getDependencies(): Set<DbObject?>?

    /**
     * Is the command we just tried to execute a DefineCommand (i.e. DDL).
     *
     * @return true if yes
     */
    protected abstract fun isCurrentCommandADefineCommand(): Boolean

    /**
     * Execute a query and return the result.
     * This method prepares everything and calls [.query] finally.
     *
     * @param maxrows the maximum number of rows to return
     * @param scrollable if the result set must be scrollable (ignored)
     * @return the result set
     */
    override fun executeQuery(maxrows: Long, scrollable: Boolean): ResultInterface? {
        startTimeNanos = 0L
        var start = 0L
        val database = session.database
        session.waitIfExclusiveModeEnabled()
        var callStop = true
        synchronized(session) {
            session.startStatementWithinTransaction(this)
            try {
                while (true) {
                    database.checkPowerOff()
                    try {
                        val result = query(maxrows)
                        callStop = !result!!.isLazy()
                        return if (database.mode.charPadding == CharPadding.IN_RESULT_SETS)
                            ResultWithPaddedStrings.get(result)
                        else result
                    } catch (e: DbException) {
                        // cannot retry DDL
                        if (isCurrentCommandADefineCommand()) throw e
                        start = filterConcurrentUpdate(e, start)
                    } catch (e: OutOfMemoryError) {
                        callStop = false
                        // there is a serious problem:
                        // the transaction may be applied partially
                        // in this case we need to panic:
                        // close the database
                        database.shutdownImmediately()
                        throw DbException.convert(e)
                    } catch (e: Throwable) {
                        throw DbException.convert(e)
                    }
                }
            } catch (e: DbException) {
                val e = e.addSQL(sql)
                val s = e.getSQLException()
                database.exceptionThrown(s, sql)
                if (s!!.errorCode == ErrorCode.OUT_OF_MEMORY) {
                    callStop = false
                    database.shutdownImmediately()
                    throw e
                }
                database.checkPowerOff()
                throw e
            } finally {
                session.endStatement()
                if (callStop) stop()
            }
        }
    }

    override fun executeUpdate(generatedKeysRequest: Any?): ResultWithGeneratedKeys? {
        var start: Long = 0
        val database = session.database
        session.waitIfExclusiveModeEnabled()

        var callStop = true
        synchronized(session) {
            commitIfNonTransactional()
            val rollback = session.setSavepoint()
            session.startStatementWithinTransaction(this)
            var ex: DbException? = null
            try {
                while (true) {
                    database.checkPowerOff()
                    try {
                        return update(generatedKeysRequest)
                    } catch (e: DbException) {
                        // cannot retry DDL
                        if (isCurrentCommandADefineCommand()) throw e
                        start = filterConcurrentUpdate(e, start)
                    } catch (e: OutOfMemoryError) {
                        callStop = false
                        database.shutdownImmediately()
                        throw DbException.convert(e)
                    } catch (e: Throwable) {
                        throw DbException.convert(e)
                    }
                }
            } catch (e: DbException) {
                val e = e.addSQL(sql)
                val s = e.getSQLException()
                database.exceptionThrown(s, sql)
                if (s!!.errorCode == ErrorCode.OUT_OF_MEMORY) {
                    callStop = false
                    database.shutdownImmediately()
                    throw e
                }
                try {
                    database.checkPowerOff()
                    if (s.errorCode == ErrorCode.DEADLOCK_1) session.rollback() else session.rollbackTo(rollback)
                } catch (nested: Throwable) {
                    e.addSuppressed(nested)
                }
                ex = e
                throw e
            } finally {
                try {
                    session.endStatement()
                    if (callStop) stop()
                } catch (nested: Throwable) {
                    if (ex == null) throw nested else ex.addSuppressed(nested)
                }
            }
        }
    }


}