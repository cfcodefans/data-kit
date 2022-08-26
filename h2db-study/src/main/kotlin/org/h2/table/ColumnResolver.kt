package org.h2.table

import org.h2.command.query.Select
import org.h2.expression.Expression
import org.h2.expression.ExpressionColumn
import org.h2.value.Value

/**
 * A column resolver is list of column (for example, a table) that can map a
 * column name to an actual column.
 */
interface ColumnResolver {

    /**
     * Get the table alias.
     * @return the table alias
     */
    fun getTableAlias(): String? = null

    /**
     * Get the column list.
     *
     * @return the column list
     */
    fun getColumns(): Array<Column>?

    /**
     * Get the column with the specified name.
     *
     * @param name
     * the column name, must be a derived name if this column
     * resolver has a derived column list
     * @return the column with the specified name, or `null`
     */
    fun findColumn(name: String?): Column?

    /**
     * Get the name of the specified column.
     *
     * @param column column
     * @return column name
     */
    fun getColumnName(column: Column): String? = column.name

    /**
     * Returns whether this column resolver has a derived column list.
     *
     * @return `true` if this column resolver has a derived column list,
     * `false` otherwise
     */
    fun hasDerivedColumnList(): Boolean = false

    /**
     * Get the list of system columns, if any.
     * @return the system columns or null
     */
    fun getSystemColumns(): Array<Column?>? = null

    /**
     * Get the row id pseudo column, if there is one.
     * @return the row id column or null
     */
    fun getRowIdColumn(): Column? = null

    /**
     * Get the schema name or null.
     * @return the schema name or null
     */
    fun getSchemaName(): String? = null

    /**
     * Get the value for the given column.
     * @param column the column
     * @return the value
     */
    fun getValue(column: Column?): Value?

    /**
     * Get the table filter.
     *
     * @return the table filter
     */
    fun getTableFilter(): TableFilter? = null

    /**
     * Get the select statement.
     * @return the select statement
     */
    open val select: Select?

    /**
     * Get the expression that represents this column.
     *
     * @param expressionColumn the expression column
     * @param column the column
     * @return the optimized expression
     */
    fun optimize(expressionColumn: ExpressionColumn?, column: Column?): Expression? = expressionColumn
}