package org.h2.expression.condition

import org.h2.engine.SessionLocal
import org.h2.expression.Expression
import org.h2.expression.ExpressionList
import org.h2.expression.ValueExpression

/**
 * Null predicate (IS [NOT] NULL).
 */
class NullPredicate(left: Expression,
                    not: Boolean,
                    whenOperand: Boolean) : SimplePredicate(left, not, whenOperand) {
    private var optimized = false

    override fun getUnenclosedSQL(builder: StringBuilder?, sqlFlags: Int): StringBuilder? {
        return getWhenSQL(left!!.getSQL(builder!!, sqlFlags, AUTO_PARENTHESES), sqlFlags)
    }

    override fun getWhenSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder? {
        return builder.append(if (not) " IS NOT NULL" else " IS NULL")
    }

    override fun optimize(session: SessionLocal?): Expression? {
        if (optimized) return this

        val o = super.optimize(session)
        if (o !== this) return o

        optimized = true
        if (whenOperand || left !is ExpressionList) return this

        val list = left as ExpressionList
        if (list.isArray) return this

        var i = 0
        val count = list.subexpressionCount
        while (i < count) {
            if (list.getSubexpression(i).isNullConstant) {
                if (not) return ValueExpression.FALSE

                val newList = ArrayList<Expression>(count - 1)
                for (j in 0 until i) {
                    newList.add(list.getSubexpression(j))
                }
                for (j in i + 1 until count) {
                    val e = list.getSubexpression(j)
                    if (!e.isNullConstant) {
                        newList.add(e)
                    }
                }
                left = if (newList.size == 1) newList[0] //
                else ExpressionList(newList.toTypedArray(), false)
                break
            }
            i++
        }
        return this
    }
}