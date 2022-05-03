package org.h2.command

import org.h2.api.DatabaseEventListener
import org.h2.api.ErrorCode
import org.h2.command.ddl.DefineCommand
import org.h2.command.dml.DataChangeStatement
import org.h2.engine.DbObject
import org.h2.engine.SessionLocal
import org.h2.expression.Expression
import org.h2.expression.ExpressionColumn
import org.h2.expression.ParameterInterface
import org.h2.message.DbException
import org.h2.result.LocalResult
import org.h2.result.ResultInterface
import org.h2.result.ResultTarget
import org.h2.result.ResultWithGeneratedKeys
import org.h2.result.ResultWithGeneratedKeys.WithKeys
import org.h2.table.Column
import org.h2.table.DataChangeDeltaTable.ResultOption
import org.h2.table.TableView
import org.h2.util.StringUtils
import org.h2.util.Utils
import org.h2.value.Value

/**
 * Represents a single SQL statements.
 * It wraps a prepared statement.
 */
open class CommandContainer(session: SessionLocal, sql: String, private var prepared: Prepared)
    : Command(session = session, sql = sql) {

    companion object {
        /**
         * Clears CTE views.
         *
         * @param session the session
         * @param views list of view
         */
        fun clearCTE(session: SessionLocal, views: List<TableView>) {
            for (view in views) {
                // check if view was previously deleted as their name is set to null
                if (view.name != null) session.removeLocalTempTable(view)
            }
        }

        /**
         * Clears CTE views for a specified statement.
         *
         * @param session the session
         * @param prepared prepared statement
         */
        fun clearCTE(session: SessionLocal, prepared: Prepared) {
            prepared.cteCleanups?.let { clearCTE(session, it) }
        }

        private class GeneratedKeysCollector internal constructor(private val indexes: IntArray, private val result: LocalResult) : ResultTarget {
            override fun limitsWereApplied() {
                // Nothing to do
            }

            override fun getRowCount(): Long {
                // Not required
                return 0L
            }

            override fun addRow(vararg values: Value?) {
                val length = indexes.size
                val row = arrayOfNulls<Value>(length)
                for (i in 0 until length) {
                    row[i] = values[indexes[i]]
                }
                result.addRow(*row)
            }
        }
    }

    init {
        prepared.setCommand(this)
    }

    private var readOnlyKnown = false
    private var readOnly = false

    override fun getParameters(): ArrayList<out ParameterInterface?> = prepared.getParameters()

    override fun isTransactional(): Boolean = prepared.isTransactional

    override fun isQuery(): Boolean = prepared.isQuery

    private fun recompileIfRequired() {
        if (!prepared.needRecompile()) return

        // TODO test with 'always recompile'
        prepared.modificationMetaId = 0
        val sql = prepared.sql
        val oldParams = prepared.getParameters()

        prepared = Parser(session).parse(sql)

        val mod = prepared.modificationMetaId
        prepared.modificationMetaId = 0

        prepared.getParameters()
                .zip(oldParams)
                .filter { it.second.isValueSet }
                .forEach {
                    val (newParam, oldParam) = it
                    newParam.setValue(oldParam.getValue(session))
                }

//        val newParams = prepared.getParameters()
//        var i = 0
//        val size = newParams.size
//        while (i < size) {
//            val old = oldParams[i]
//            if (old.isValueSet) {
//                val v = old.getValue(session)
//                val p = newParams[i]
//                p.setValue(v)
//            }
//            i++
//        }

        prepared.prepare()
        prepared.modificationMetaId = mod
    }


    override fun update(generatedKeysRequest: Any?): ResultWithGeneratedKeys {
        recompileIfRequired()
        setProgress(DatabaseEventListener.STATE_STATEMENT_START)
        start()
        prepared.checkParameters()
        val result: ResultWithGeneratedKeys
        if (generatedKeysRequest != null && generatedKeysRequest != false) {
            if (prepared is DataChangeStatement && prepared.type != CommandInterface.DELETE) {
                result = executeUpdateWithGeneratedKeys(prepared as DataChangeStatement, generatedKeysRequest)
            } else {
                result = WithKeys(prepared.update(), LocalResult())
            }
        } else {
            result = ResultWithGeneratedKeys.of(prepared.update())
        }
        prepared.trace(startTimeNanos, result.updateCount)
        setProgress(DatabaseEventListener.STATE_STATEMENT_END)
        return result
    }

    /**
     * TODO to read
     */
    private fun executeUpdateWithGeneratedKeys(statement: DataChangeStatement,
                                               generatedKeysRequest: Any): ResultWithGeneratedKeys {
        val db = session.database
        val table = statement.table

        val expressionColumns: java.util.ArrayList<ExpressionColumn>

        if (true == generatedKeysRequest) {
            expressionColumns = Utils.newSmallArrayList()
            val columns = table.columns
            val primaryKey = table.findPrimaryKey()
            for (column in columns) {
                var e: Expression
                if (column.isIdentity
                        || column.effectiveDefaultExpression.also { e = it } != null && !e.isConstant
                        || primaryKey != null && primaryKey.getColumnIndex(column) >= 0) {
                    expressionColumns.add(ExpressionColumn(db, column))
                }
            }
        } else if (generatedKeysRequest is IntArray) {
            val indexes = generatedKeysRequest
            val columns = table.columns
            val cnt = columns.size
            expressionColumns = java.util.ArrayList(indexes.size)
            for (idx in indexes) {
                if (idx < 1 || idx > cnt) {
                    throw DbException.get(ErrorCode.COLUMN_NOT_FOUND_1, "Index: $idx")
                }
                expressionColumns.add(ExpressionColumn(db, columns[idx - 1]))
            }
        } else if (generatedKeysRequest is Array<*> && generatedKeysRequest.isArrayOf<String>()) {
            val names = generatedKeysRequest
            expressionColumns = ArrayList(names.size)

            for (name in names as Array<String>) {
                var column: Column = table.findColumn(name)
                        ?: db.settings.let {
                            if (it.databaseToUpper) table.findColumn(StringUtils.toUpperEnglish(name))
                            else if (it.databaseToLower) table.findColumn(StringUtils.toLowerEnglish(name))
                            else null
                        } ?: table.columns.firstOrNull { c -> c.name.equals(name, ignoreCase = true) }
                        ?: throw DbException.get(ErrorCode.COLUMN_NOT_FOUND_1, name)

                expressionColumns.add(ExpressionColumn(db, column))
            }
        } else {
            throw DbException.getInternalError()
        }

        val columnCount = expressionColumns.size
        if (columnCount == 0) {
            return WithKeys(statement.update(), LocalResult())
        }

        val indexes = IntArray(columnCount)
        val expressions = expressionColumns.toTypedArray()
        for (i in 0 until columnCount) {
            indexes[i] = expressions[i].column.columnId
        }
        val result = LocalResult(session, expressions, columnCount, columnCount)
        return WithKeys(statement.update(GeneratedKeysCollector(indexes, result), ResultOption.FINAL), result)
    }

    override fun query(maxrows: Long): ResultInterface? {
        recompileIfRequired()
        setProgress(DatabaseEventListener.STATE_STATEMENT_START)
        start()
        prepared.checkParameters()
        val result = prepared.query(maxrows)
        prepared.trace(startTimeNanos, if (result.isLazy) 0 else result.rowCount)
        setProgress(DatabaseEventListener.STATE_STATEMENT_END)
        return result
    }

    override fun stop() {
        super.stop()
        // Clean up after the command was run in the session.
        // Must restart query (and dependency construction) to reuse.
        clearCTE(session, prepared)
    }

    override fun canReuse(): Boolean = super.canReuse() && prepared.cteCleanups == null

    override fun isReadOnly(): Boolean {
        if (!readOnlyKnown) {
            readOnly = prepared.isReadOnly
            readOnlyKnown = true
        }
        return readOnly
    }

    override fun queryMeta(): ResultInterface? = prepared.queryMeta()

    override fun isCacheable(): Boolean = prepared.isCacheable

    override fun getCommandType(): Int = prepared.type

    /**
     * Clean up any associated CTE.
     */
    open fun clearCTE() {
        clearCTE(session, prepared)
    }

    override fun getDependencies(): Set<DbObject>? {
        return HashSet<DbObject>().also { prepared.collectDependencies(it) }
    }

    override fun isCurrentCommandADefineCommand(): Boolean = prepared is DefineCommand
}