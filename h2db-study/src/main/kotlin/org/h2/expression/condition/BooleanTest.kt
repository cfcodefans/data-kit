package org.h2.expression.condition

import org.h2.engine.SessionLocal
import org.h2.expression.Expression
import org.h2.expression.ExpressionColumn
import org.h2.expression.TypedValueExpression
import org.h2.expression.ValueExpression
import org.h2.index.IndexCondition
import org.h2.table.TableFilter
import org.h2.value.Value
import org.h2.value.ValueBoolean
import org.h2.value.ValueNull

/**
 * Boolean test (IS [NOT] { TRUE | FALSE | UNKNOWN }).
 */
open class BooleanTest(val right: Boolean?,
                       left: Expression?, not: Boolean, whenOperand: Boolean)
    : SimplePredicate(left, not, whenOperand) {

    override fun getUnenclosedSQL(builder: StringBuilder?, sqlFlags: Int): StringBuilder? {
        return getWhenSQL(left!!.getSQL(builder!!, sqlFlags, AUTO_PARENTHESES), sqlFlags)
    }

    override fun getWhenSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder? {
        return builder.append(if (not) " IS NOT " else " IS ").append(if (right == null) "UNKNOWN" else if (right) "TRUE" else "FALSE")
    }

    override fun getValue(session: SessionLocal?): Value? = ValueBoolean[getValue(left!!.getValue(session)!!)]

    override fun getWhenValue(session: SessionLocal, left: Value?): Boolean = if (!whenOperand) {
        super.getWhenValue(session, left)
    } else getValue(left!!)

    private fun getValue(left: Value): Boolean = (if (left === ValueNull.INSTANCE)
        right == null
    else
        right != null && right == left.getBoolean()) xor not

    override fun getNotIfPossible(session: SessionLocal?): Expression? = if (whenOperand) null else BooleanTest(left = left, not = !not, whenOperand = false, right = right)

    override fun createIndexConditions(session: SessionLocal?, filter: TableFilter?) {
        if (whenOperand || !filter!!.table.isQueryComparable) return

        if (left !is ExpressionColumn) return

        val c = left as ExpressionColumn
        if (c.type.valueType == Value.BOOLEAN && filter === c.tableFilter) {
            if (not) {
                if (right == null && c.column.isNullable) {
                    val list = ArrayList<Expression>(2)
                    list.add(ValueExpression.FALSE)
                    list.add(ValueExpression.TRUE)
                    filter.addIndexCondition(IndexCondition.getInList(c, list))
                }
            } else {
                filter.addIndexCondition(IndexCondition.get(Comparison.EQUAL_NULL_SAFE, c,
                                                            if (right == null) TypedValueExpression.UNKNOWN else ValueExpression.getBoolean(right)))
            }
        }
    }
}