package org.h2.expression

import org.h2.api.ErrorCode
import org.h2.engine.Mode.ExpressionNames
import org.h2.engine.Mode.ViewExpressionNames
import org.h2.engine.SessionLocal
import org.h2.expression.function.NamedExpression
import org.h2.message.DbException
import org.h2.table.Column
import org.h2.table.ColumnResolver
import org.h2.table.TableFilter
import org.h2.util.HasSQL
import org.h2.util.StringUtils
import org.h2.util.Typed
import org.h2.value.TypeInfo
import org.h2.value.Value

/**
 * An expression is a operation, a value, or a function in a query.
 */
abstract class Expression : HasSQL, Typed {

    companion object {
        /**
         * Initial state for [.mapColumns].
         */
        const val MAP_INITIAL = 0

        /**
         * State for expressions inside a window function for
         * [.mapColumns].
         */
        const val MAP_IN_WINDOW = 1

        /**
         * State for expressions inside an aggregate for
         * [.mapColumns].
         */
        const val MAP_IN_AGGREGATE = 2

        /**
         * Wrap expression in parentheses only if it can't be safely included into
         * other expressions without them.
         */
        const val AUTO_PARENTHESES = 0

        /**
         * Wrap expression in parentheses unconditionally.
         */
        const val WITH_PARENTHESES = 1

        /**
         * Do not wrap expression in parentheses.
         */
        const val WITHOUT_PARENTHESES = 2

        /**
         * Get the SQL snippet for a list of expressions.
         *
         * @param builder the builder to append the SQL to
         * @param expressions the list of expressions
         * @param sqlFlags formatting flags
         * @return the specified string builder
         */
        fun writeExpressions(builder: StringBuilder, expressions: List<Expression>, sqlFlags: Int): StringBuilder? {
            for ((i, exp) in expressions.withIndex()) {
                if (i > 0) builder.append(", ")
                exp.getUnenclosedSQL(builder, sqlFlags)
            }
            return builder
        }

        /**
         * Get the SQL snippet for an array of expressions.
         *
         * @param builder the builder to append the SQL to
         * @param expressions the list of expressions
         * @param sqlFlags formatting flags
         * @return the specified string builder
         */
        fun writeExpressions(builder: StringBuilder, expressions: Array<Expression?>, sqlFlags: Int): StringBuilder {
            for ((i, e) in expressions.withIndex()) {
                if (i > 0) builder.append(", ")

                if (e == null) builder.append("DEFAULT")
                else e.getUnenclosedSQL(builder, sqlFlags)
            }
            return builder
        }
    }

    private var addedToFilter: Boolean = false

    /**
     * Return the resulting value for the current row.
     *
     * @param session the session
     * @return the result
     */
    abstract fun getValue(session: SessionLocal?): Value?

    /**
     * Map the columns of the resolver to expression columns.
     *
     * @param resolver the column resolver
     * @param level the subquery nesting level
     * @param state current state for nesting checks, initial value is
     * [.MAP_INITIAL]
     */
    abstract fun mapColumns(resolver: ColumnResolver?, level: Int, state: Int)

    /**
     * Try to optimize the expression.
     *
     * @param session the session
     * @return the optimized expression
     */
    abstract fun optimize(session: SessionLocal?): Expression?

    /**
     * Try to optimize or remove the condition.
     *
     * @param session the session
     * @return the optimized condition, or `null`
     */
    fun optimizeCondition(session: SessionLocal): Expression? {
        val e = optimize(session)!!
        return if (e.isConstant()) {
            if (e.getBooleanValue(session)) null else ValueExpression.FALSE
        } else e
    }

    /**
     * Tell the expression columns whether the table filter can return values
     * now. This is used when optimizing the query.
     *
     * @param tableFilter the table filter
     * @param value true if the table filter can return value
     */
    abstract fun setEvaluatable(tableFilter: TableFilter?, value: Boolean)

    override fun getSQL(sqlFlags: Int): String = getSQL(StringBuilder(), sqlFlags, AUTO_PARENTHESES).toString()

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder = getSQL(builder, sqlFlags, AUTO_PARENTHESES)

    /**
     * Get the SQL statement of this expression. This may not always be the
     * original SQL statement, especially after optimization.
     *
     * @param sqlFlags formatting flags
     * @param parentheses parentheses mode
     * @return the SQL statement
     */
    fun getSQL(sqlFlags: Int, parentheses: Int): String = getSQL(StringBuilder(), sqlFlags, parentheses).toString()

    /**
     * Get the SQL statement of this expression. This may not always be the
     * original SQL statement, especially after optimization.
     *
     * @param builder string builder
     * @param sqlFlags formatting flags
     * @param parentheses parentheses mode
     * @return the specified string builder
     */
    fun getSQL(builder: StringBuilder, sqlFlags: Int, parentheses: Int): StringBuilder {
        return if (parentheses == WITH_PARENTHESES || parentheses != WITHOUT_PARENTHESES && needParentheses()) getUnenclosedSQL(builder.append('('), sqlFlags)!!.append(')') else getUnenclosedSQL(builder, sqlFlags)!!
    }

    /**
     * Returns whether this expressions needs to be wrapped in parentheses when
     * it is used as an argument of other expressions.
     *
     * @return `true` if it is
     */
    open fun needParentheses(): Boolean = false

    /**
     * Get the SQL statement of this expression. This may not always be the
     * original SQL statement, especially after optimization. Enclosing '(' and
     * ')' are always appended.
     *
     * @param builder string builder
     * @param sqlFlags formatting flags
     * @return the specified string builder
     */
    fun getEnclosedSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder = getUnenclosedSQL(builder.append('('), sqlFlags)!!.append(')')

    /**
     * Get the value in form of a boolean expression.
     * Returns true or false.
     * In this database, everything can be a condition.
     *
     * @param session the session
     * @return the result
     */
    open fun getBooleanValue(session: SessionLocal?): Boolean = getValue(session)!!.isTrue()

    /**
     * Get the SQL statement of this expression. This may not always be the
     * original SQL statement, especially after optimization. Enclosing '(' and
     * ')' are never appended.
     *
     * @param builder string builder
     * @param sqlFlags formatting flags
     * @return the specified string builder
     */
    abstract fun getUnenclosedSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder?

    /**
     * Update an aggregate value. This method is called at statement execution
     * time. It is usually called once for each row, but if the expression is
     * used multiple times (for example in the column list, and as part of the
     * HAVING expression) it is called multiple times - the row counter needs to
     * be used to make sure the internal state is only updated once.
     *
     * @param session the session
     * @param stage select stage
     */
    abstract fun updateAggregate(session: SessionLocal?, stage: Int)

    /**
     * Check if this expression and all sub-expressions can fulfill a criteria.
     * If any part returns false, the result is false.
     *
     * @param visitor the visitor
     * @return if the criteria can be fulfilled
     */
    abstract fun isEverything(visitor: ExpressionVisitor?): Boolean

    /**
     * Estimate the cost to process the expression.
     * Used when optimizing the query, to calculate the query plan
     * with the lowest estimated cost.
     *
     * @return the estimated cost
     */
    abstract fun getCost(): Int

    /**
     * If it is possible, return the negated expression. This is used
     * to optimize NOT expressions: NOT ID&gt;10 can be converted to
     * ID&lt;=10. Returns null if negating is not possible.
     *
     * @param session the session
     * @return the negated expression, or null
     */
    open fun getNotIfPossible(session: SessionLocal?): Expression? {
        // by default it is not possible
        return null
    }

    /**
     * Check if this expression will always return the same value.
     *
     * @return if the expression is constant
     */
    open fun isConstant(): Boolean = false

    /**
     * Check if this expression will always return the NULL value.
     *
     * @return if the expression is constant NULL value
     */
    open fun isNullConstant(): Boolean = false

    /**
     * Is the value of a parameter set.
     *
     * @return true if set
     */
    open fun isValueSet(): Boolean = false

    /**
     * Check if this is an identity column.
     *
     * @return true if it is an identity column
     */
    open fun isIdentity(): Boolean = false

    /**
     * Create index conditions if possible and attach them to the table filter.
     *
     * @param session the session
     * @param filter the table filter
     */
    open fun createIndexConditions(session: SessionLocal?, filter: TableFilter?) {
        // default is do nothing
    }

    /**
     * Get the column name or alias name of this expression.
     *
     * @param session the session
     * @param columnIndex 0-based column index
     * @return the column name
     */
    open fun getColumnName(session: SessionLocal, columnIndex: Int): String? = getAlias(session, columnIndex)

    /**
     * Get the schema name, or null
     *
     * @return the schema name
     */
    open fun getSchemaName(): String? = null

    /**
     * Get the table name, or null
     *
     * @return the table name
     */
    open fun getTableName(): String? = null

    /**
     * Check whether this expression is a column and can store NULL.
     *
     * @return whether NULL is allowed
     */
    open fun getNullable(): Int = Column.NULLABLE_UNKNOWN

    /**
     * Get the table alias name or null
     * if this expression does not represent a column.
     *
     * @return the table alias name
     */
    open fun getTableAlias(): String? = null

    /**
     * Get the alias name of a column or SQL expression
     * if it is not an aliased expression.
     *
     * @param session the session
     * @param columnIndex 0-based column index
     * @return the alias name
     */
    open fun getAlias(session: SessionLocal, columnIndex: Int): String? = when (session.mode.expressionNames) {
        ExpressionNames.EMPTY -> ""
        ExpressionNames.NUMBER -> (columnIndex + 1).toString()
        ExpressionNames.C_NUMBER -> "C" + (columnIndex + 1)
        ExpressionNames.POSTGRESQL_STYLE -> {
            if (this is NamedExpression) {
                StringUtils.toLowerEnglish((this as NamedExpression).name)
            } else "?column?"
        }
        else -> getSQL(HasSQL.QUOTE_ONLY_WHEN_REQUIRED or HasSQL.NO_CASTS, WITHOUT_PARENTHESES)
    }

    /**
     * Get the column name of this expression for a view.
     *
     * @param session the session
     * @param columnIndex 0-based column index
     * @return the column name for a view
     */
    open fun getColumnNameForView(session: SessionLocal, columnIndex: Int): String? = when (session.mode.viewExpressionNames) {
        ViewExpressionNames.AS_IS -> getAlias(session, columnIndex)
        ViewExpressionNames.EXCEPTION -> throw DbException.get(ErrorCode.COLUMN_ALIAS_IS_NOT_SPECIFIED_1, getTraceSQL()!!)
        ViewExpressionNames.MYSQL_STYLE -> {
            var name = getSQL(HasSQL.QUOTE_ONLY_WHEN_REQUIRED or HasSQL.NO_CASTS, WITHOUT_PARENTHESES)
            if (name.length > 64) "Name_exp_" + (columnIndex + 1) else name
        }
        else -> getAlias(session, columnIndex)
    }

    /**
     * Returns the main expression, skipping aliases.
     * @return the expression
     */
    open fun getNonAliasExpression(): Expression? = this

    /**
     * Add conditions to a table filter if they can be evaluated.
     * @param filter the table filter
     */
    open fun addFilterConditions(filter: TableFilter) {
        if (!addedToFilter && isEverything(ExpressionVisitor.EVALUATABLE_VISITOR)) {
            filter.addFilterCondition(this, false)
            addedToFilter = true
        }
    }

    /**
     * Convert this expression to a String.
     * @return the string representation
     */
    override fun toString(): String = getTraceSQL()!!

    /**
     * Returns count of subexpressions.
     * @return count of subexpressions
     */
    open fun getSubexpressionCount(): Int = 0

    /**
     * Returns subexpression with specified index.
     * @param index 0-based index
     * @return subexpression with specified index, may be null
     * @throws IndexOutOfBoundsException if specified index is not valid
     */
    open fun getSubexpression(index: Int): Expression? = throw IndexOutOfBoundsException()

    /**
     * Return the resulting value of when operand for the current row.
     * @param session  the session
     * @param left value on the left side
     * @return the result
     */
    open fun getWhenValue(session: SessionLocal, left: Value?): Boolean {
        return session.compareWithNull(left, getValue(session), true) == 0
    }

    /**
     * Appends the SQL statement of this when operand to the specified builder.
     * @param builder string builder
     * @param sqlFlags formatting flags
     * @return the specified string builder
     */
    open fun getWhenSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder? = getUnenclosedSQL(builder.append(' '), sqlFlags)

    /**
     * Returns whether this expression is a right side of condition in a when
     * operand.
     * @return `true` if it is, `false` otherwise
     */
    open fun isWhenConditionOperand(): Boolean = false
}
