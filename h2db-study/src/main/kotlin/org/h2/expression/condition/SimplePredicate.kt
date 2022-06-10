package org.h2.expression.condition

import org.h2.engine.SessionLocal
import org.h2.expression.Expression
import org.h2.expression.ExpressionVisitor
import org.h2.expression.ValueExpression
import org.h2.table.ColumnResolver
import org.h2.table.TableFilter

/**
 * Base class for simple predicates.
 */
abstract class SimplePredicate(
    /**
     * The left hand side of the expression.
     */
    var left: Expression? = null,

    /**
     * Whether it is a "not" condition (e.g. "is not null").
     */
    val not: Boolean = false,

    /**
     * Where this is the when operand of the simple case.
     */
    val whenOperand: Boolean = false) : Condition() {

    override fun optimize(session: SessionLocal?): Expression? {
        left = left!!.optimize(session)
        return if (!whenOperand && left!!.isConstant()) {
            ValueExpression.getBoolean(getValue(session))
        } else this
    }

    override fun setEvaluatable(tableFilter: TableFilter?, b: Boolean) {
        left!!.setEvaluatable(tableFilter, b)
    }

    override fun needParentheses(): Boolean = true

    override fun updateAggregate(session: SessionLocal?, stage: Int) = left!!.updateAggregate(session, stage)

    override fun mapColumns(resolver: ColumnResolver?, level: Int, state: Int) = left!!.mapColumns(resolver, level, state)

    override fun isEverything(visitor: ExpressionVisitor?): Boolean = left!!.isEverything(visitor)

    override fun getCost(): Int = left!!.getCost() + 1

    override fun getSubexpressionCount(): Int = 1

    override fun getSubexpression(index: Int): Expression? {
        if (index == 0) return left
        throw IndexOutOfBoundsException()
    }

    override fun isWhenConditionOperand(): Boolean = whenOperand
}