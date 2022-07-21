package org.h2.expression.condition

import org.h2.engine.SessionLocal
import org.h2.expression.Expression
import org.h2.expression.ExpressionVisitor
import org.h2.expression.aggregate.Aggregate
import org.h2.expression.aggregate.AggregateType
import org.h2.message.DbException
import org.h2.util.HasSQL
import org.h2.value.Value
import org.h2.value.ValueBoolean
import org.h2.value.ValueNull

/**
 * Example comparison expressions are ID=1, NAME=NAME, NAME IS NULL.
 *
 */
class Comparison(private var compareType: Int = 0,
                 private val left: Expression? = null,
                 private val right: Expression? = null,
                 private val whenOperand: Boolean = false) : Condition() {
    companion object {
        /**
         * The comparison type meaning = as in ID=1.
         */
        const val EQUAL: Int = 0

        /**
         * The comparison type meaning &lt;&gt; as in ID&lt;&gt;1.
         */
        const val NOT_EQUAL: Int = 1

        /**
         * The comparison type meaning &lt; as in ID&lt;1.
         */
        const val SMALLER: Int = 2

        /**
         * The comparison type meaning &gt; as in ID&gt;1.
         */
        const val BIGGER: Int = 3

        /**
         * The comparison type meaning &lt;= as in ID&lt;=1.
         */
        const val SMALLER_EQUAL: Int = 4

        /**
         * The comparison type meaning &gt;= as in ID&gt;=1.
         */
        const val BIGGER_EQUAL: Int = 5

        /**
         * The comparison type meaning ID IS NOT DISTINCT FROM 1.
         */
        const val EQUAL_NULL_SAFE: Int = 6

        /**
         * The comparison type meaning ID IS DISTINCT FROM 1.
         */
        const val NOT_EQUAL_NULL_SAFE: Int = 7

        /**
         * This is a comparison type that is only used for spatial index
         * conditions (operator "&amp;&amp;").
         */
        const val SPATIAL_INTERSECTS: Int = 8

        val COMPARE_TYPES = arrayOf("=", "<>", "<", ">", "<=", ">=",  //
                                    "IS NOT DISTINCT FROM", "IS DISTINCT FROM",  //
                                    "&&")

        /**
         * This is a pseudo comparison type that is only used for index conditions.
         * It means the comparison will always yield FALSE. Example: 1=0.
         */
        const val FALSE: Int = 9

        /**
         * This is a pseudo comparison type that is only used for index conditions.
         * It means equals any value of a list. Example: IN(1, 2, 3).
         */
        const val IN_LIST: Int = 10

        /**
         * This is a pseudo comparison type that is only used for index conditions.
         * It means equals any value of a list. Example: IN(SELECT ...).
         */
        const val IN_QUERY: Int = 11

        /**
         * Compare two values.
         *
         * @param session the session
         * @param l the first value
         * @param r the second value
         * @param compareType the compare type
         * @return result of comparison, either TRUE, FALSE, or NULL
         */
        fun compare(session: SessionLocal, l: Value, r: Value, compareType: Int): Value {
            return when (compareType) {
                EQUAL -> {
                    when (session.compareWithNull(l, r, true)) {
                        0 -> ValueBoolean.TRUE
                        Int.MIN_VALUE -> ValueNull.INSTANCE
                        else -> ValueBoolean.FALSE
                    }
                }
                EQUAL_NULL_SAFE -> {
                    ValueBoolean[session.areEqual(l, r)]
                }
                NOT_EQUAL -> {
                    when (session.compareWithNull(l, r, true)) {
                        0 -> ValueBoolean.FALSE
                        Int.MIN_VALUE -> ValueNull.INSTANCE
                        else -> ValueBoolean.TRUE
                    }
                }
                NOT_EQUAL_NULL_SAFE -> {
                    ValueBoolean[!session.areEqual(l, r)]
                }
                BIGGER_EQUAL -> {
                    val cmp = session.compareWithNull(l, r, false)
                    when {
                        cmp >= 0 -> ValueBoolean.TRUE
                        cmp == Int.MIN_VALUE -> ValueNull.INSTANCE
                        else -> ValueBoolean.FALSE
                    }
                }
                BIGGER -> {
                    val cmp = session.compareWithNull(l, r, false)
                    when {
                        cmp > 0 -> ValueBoolean.TRUE
                        cmp == Int.MIN_VALUE -> ValueNull.INSTANCE
                        else -> ValueBoolean.FALSE
                    }
                }
                SMALLER_EQUAL -> {
                    val cmp = session.compareWithNull(l, r, false)
                    if (cmp == Int.MIN_VALUE) ValueNull.INSTANCE else ValueBoolean[cmp <= 0]
                }
                SMALLER -> {
                    val cmp = session.compareWithNull(l, r, false)
                    if (cmp == Int.MIN_VALUE) ValueNull.INSTANCE else ValueBoolean[cmp < 0]
                }
                SPATIAL_INTERSECTS -> {
                    if (l === ValueNull.INSTANCE || r === ValueNull.INSTANCE) {
                        ValueNull.INSTANCE
                    } else {
                        ValueBoolean[l.convertToGeometry(null).intersectsBoundingBox(r.convertToGeometry(null))]
                    }
                }
                else -> throw DbException.getInternalError("type=$compareType")
            }
        }

        private fun getConditionIn(session: SessionLocal,
                                   left: Expression,
                                   value1: Expression,
                                   value2: Expression): ConditionIn = ConditionIn(left,
                                                                                  false,
                                                                                  false,
                                                                                  ArrayList<Expression>(2).apply {
                                                                                      add(value1)
                                                                                      add(value2)
                                                                                  })
    }

    override fun needParentheses(): Boolean = true

    override fun getWhenSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder? {
        builder.append(' ').append(COMPARE_TYPES[compareType]).append(' ')
        return right!!.getSQL(builder = builder,
                              sqlFlags = sqlFlags,
                              parentheses = if (right is Aggregate && right.aggregateType == AggregateType.ANY)
                                  WITH_PARENTHESES
                              else AUTO_PARENTHESES)
    }

    override fun getValue(session: SessionLocal?): Value? {
        val l = left!!.getValue(session)
        // Optimization: do not evaluate right if not necessary
        return if (l === ValueNull.INSTANCE && compareType and 1.inv() != EQUAL_NULL_SAFE) {
            ValueNull.INSTANCE
        } else Comparison.compare(session!!, l!!, right!!.getValue(session)!!, compareType)
    }

    override fun getCost(): Int = left!!.getCost() + right!!.getCost() + 1

    override fun isWhenConditionOperand(): Boolean = whenOperand

    private fun getReversedCompareType(type: Int): Int = when (type) {
        EQUAL, EQUAL_NULL_SAFE, NOT_EQUAL, NOT_EQUAL_NULL_SAFE, SPATIAL_INTERSECTS -> type
        BIGGER_EQUAL -> SMALLER_EQUAL
        BIGGER -> SMALLER
        SMALLER_EQUAL -> BIGGER_EQUAL
        SMALLER -> BIGGER
        else -> throw DbException.getInternalError("type=$type")
    }

    override fun getNotIfPossible(session: SessionLocal?): Expression? {
        return if (compareType == SPATIAL_INTERSECTS || whenOperand) null
        else Comparison(getNotCompareType(), left, right, false)
    }

    private fun getNotCompareType(): Int = when (compareType) {
        EQUAL -> NOT_EQUAL
        EQUAL_NULL_SAFE -> NOT_EQUAL_NULL_SAFE
        NOT_EQUAL -> EQUAL
        NOT_EQUAL_NULL_SAFE -> EQUAL_NULL_SAFE
        BIGGER_EQUAL -> SMALLER
        BIGGER -> SMALLER_EQUAL
        SMALLER_EQUAL -> BIGGER
        SMALLER -> BIGGER_EQUAL
        else -> throw DbException.getInternalError("type=$compareType")
    }

    override fun isEverything(visitor: ExpressionVisitor?): Boolean = left!!.isEverything(visitor) && right!!.isEverything(visitor)

    /**
     * Get the other expression if this is an equals comparison and the other
     * expression matches.
     *
     * @param match the expression that should match
     * @return null if no match, the other expression if there is a match
     */
    fun getIfEquals(match: Expression): Expression? {
        if (compareType != EQUAL) return null

        return when (match.getSQL(HasSQL.DEFAULT_SQL_FLAGS)) {
            left!!.getSQL(HasSQL.DEFAULT_SQL_FLAGS) -> right
            right!!.getSQL(HasSQL.DEFAULT_SQL_FLAGS) -> left
            else -> null
        }
    }

    /**
     * Get an additional condition if possible. Example: given two conditions
     * A=B AND B=C, the new condition A=C is returned.
     *
     * @param session the session
     * @param other the second condition
     * @return null or the third condition for indexes
     */
    fun getAdditionalAnd(session: SessionLocal?, other: Comparison): Expression? {
        if (!(compareType == EQUAL && other.compareType == EQUAL) || whenOperand) return null

        val lc = left!!.isConstant()
        val rc = right!!.isConstant()
        val l2c = other.left!!.isConstant()
        val r2c = other.right!!.isConstant()
        val l = left.getSQL(HasSQL.DEFAULT_SQL_FLAGS)
        val l2 = other.left.getSQL(HasSQL.DEFAULT_SQL_FLAGS)
        val r = right.getSQL(HasSQL.DEFAULT_SQL_FLAGS)
        val r2 = other.right.getSQL(HasSQL.DEFAULT_SQL_FLAGS)
        // a=b AND a=c
        // must not compare constants. example: NOT(B=2 AND B=3)
        if (!(rc && r2c) && (l == l2)) return Comparison(EQUAL, right, other.right, false)
        if (!(rc && l2c) && (l == r2)) return Comparison(EQUAL, right, other.left, false)
        if (!(lc && r2c) && (r == l2)) return Comparison(EQUAL, left, other.right, false)
        if (!(lc && l2c) && (r == r2)) return Comparison(EQUAL, left, other.left, false)
        return null
    }

    /**
     * Replace the OR condition with IN condition if possible. Example: given
     * the two conditions A=1 OR A=2, the new condition A IN(1, 2) is returned.
     *
     * @param session the session
     * @param other the second condition
     * @return null or the joined IN condition
     */
    fun optimizeOr(session: SessionLocal?, other: Comparison): Expression? {
        if (compareType != EQUAL || other.compareType != EQUAL) return null

        val left2 = other.left
        val right2 = other.right
        val l2 = left2!!.getSQL(HasSQL.DEFAULT_SQL_FLAGS)
        val r2 = right2!!.getSQL(HasSQL.DEFAULT_SQL_FLAGS)

        if (left!!.isEverything(ExpressionVisitor.DETERMINISTIC_VISITOR)) {
            val l = left.getSQL(HasSQL.DEFAULT_SQL_FLAGS)
            if (l == l2) return getConditionIn(session!!, left, right!!, right2)
            if (l == r2) return getConditionIn(session!!, left, right!!, left2)
        }
        if (right!!.isEverything(ExpressionVisitor.DETERMINISTIC_VISITOR)) {
            val r = right.getSQL(HasSQL.DEFAULT_SQL_FLAGS)
            if (r == l2) return getConditionIn(session!!, right, left, right2)
            if (r == r2) return getConditionIn(session!!, right, left, left2)
        }
        return null
    }

    override fun getSubexpressionCount(): Int = 2

    override fun getSubexpression(index: Int): Expression? = when (index) {
        0 -> left
        1 -> right
        else -> throw IndexOutOfBoundsException()
    }
}