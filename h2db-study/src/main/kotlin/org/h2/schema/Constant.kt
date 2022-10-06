package org.h2.schema

import org.h2.engine.DbObject
import org.h2.engine.SessionLocal
import org.h2.expression.ValueExpression
import org.h2.message.DbException
import org.h2.message.Trace
import org.h2.table.Table
import org.h2.util.HasSQL
import org.h2.value.Value

/**
 * A user-defined constant as created by the SQL statement
 * CREATE CONSTANT
 */
class Constant(schema: Schema, id: Int, name: String) : SchemaObject(schema, id, name, Trace.SCHEMA) {

    private var value: Value? = null
    private var expression: ValueExpression? = null

    override fun getCreateSQLForCopy(table: Table?, quotedName: String?): String? = throw DbException.getInternalError(toString())

    override fun getCreateSQL(): String = StringBuilder("CREATE CONSTANT ")
        .also {
            getSQL(it, HasSQL.DEFAULT_SQL_FLAGS).append(" VALUE ")
            value!!.getSQL(it, HasSQL.DEFAULT_SQL_FLAGS)
        }.toString()

    override fun getType(): Int = DbObject.CONSTANT

    override fun removeChildrenAndResources(session: SessionLocal?) {
        database!!.removeMeta(session, id)
        invalidate()
    }

    fun setValue(value: Value?) {
        this.value = value
        expression = ValueExpression.get(value)
    }

    fun getValue(): ValueExpression? = expression
}