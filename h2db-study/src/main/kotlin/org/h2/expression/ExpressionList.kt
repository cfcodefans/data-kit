package org.h2.expression

import org.h2.engine.SessionLocal
import org.h2.table.ColumnResolver
import org.h2.table.TableFilter
import org.h2.value.ExtTypeInfoRow
import org.h2.value.TypeInfo
import org.h2.value.Typed
import org.h2.value.Value
import org.h2.value.ValueArray
import org.h2.value.ValueRow
import org.jetbrains.kotlin.utils.addToStdlib.cast

/**
 * A list of expressions, as in (ID, NAME).
 * The result of this expression is a row or an array.
 */
open class ExpressionList(open val list: Array<Expression>,
                          open val isArray: Boolean = false) : Expression() {

    override var type: TypeInfo? = null

    fun initializeType() {
        type = if (isArray)
            TypeInfo.getTypeInfo(Value.ARRAY, list!!.size.toLong(), 0, TypeInfo.getHigherType(list.cast()))
        else
            TypeInfo.getTypeInfo(Value.ROW, 0, 0, ExtTypeInfoRow(list as Array<Typed>))
    }

    override fun getValue(session: SessionLocal?): Value? {
        val v = arrayOfNulls<Value>(list.size)
        for (i in list.indices) {
            v[i] = list[i].getValue(session)
        }
        return if (isArray) ValueArray[type?.extTypeInfo as TypeInfo?, v, session] else ValueRow[type, v]
    }

    override fun mapColumns(resolver: ColumnResolver?, level: Int, state: Int) {
        for (e in list) {
            e.mapColumns(resolver, level, state)
        }
    }

    override fun optimize(session: SessionLocal?): Expression? {
        var allConst = true
        val count = list.size
        for (i in 0 until count) {
            val e = list[i].optimize(session)
            if (!e!!.isConstant()) {
                allConst = false
            }
            list[i] = e
        }
        initializeType()
        return if (allConst) ValueExpression.get(getValue(session)) else this
    }

    override fun setEvaluatable(tableFilter: TableFilter?, b: Boolean) {
        for (e in list) {
            e.setEvaluatable(tableFilter, b)
        }
    }

    override fun getUnenclosedSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder? {
        return if (isArray) writeExpressions(builder.append("ARRAY ["), list as Array<Expression?>, sqlFlags).append(']')
        else writeExpressions(builder.append("ROW ("), list as Array<Expression?>, sqlFlags).append(')')
    }

    override fun updateAggregate(session: SessionLocal?, stage: Int) {
        for (e in list) {
            e.updateAggregate(session, stage)
        }
    }

    override fun isEverything(visitor: ExpressionVisitor?): Boolean = list.any { !it.isEverything(visitor) }.not()

    override fun getCost(): Int = list.fold(1) { r, t -> r + t.getCost() }

    override fun isConstant(): Boolean = list.any { !it.isConstant() }.not()

    override fun getSubexpression(index: Int): Expression? = list[index]

    override fun getSubexpressionCount(): Int = list.size
}