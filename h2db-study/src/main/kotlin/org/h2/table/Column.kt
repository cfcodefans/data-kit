package org.h2.table

import org.h2.api.ErrorCode
import org.h2.command.Parser
import org.h2.command.ddl.SequenceOptions
import org.h2.engine.CastDataProvider
import org.h2.engine.SessionLocal
import org.h2.expression.Expression
import org.h2.expression.ValueExpression
import org.h2.message.DbException
import org.h2.result.Row
import org.h2.schema.Domain
import org.h2.schema.Sequence
import org.h2.util.HasSQL
import org.h2.util.ParserUtil
import org.h2.util.StringUtils
import org.h2.util.StringUtils.appends
import org.h2.util.Typed
import org.h2.value.TypeInfo
import org.h2.value.Value
import org.h2.value.ValueNull
import java.sql.ResultSetMetaData


/**
 * This class represents a column in a table.
 */
open class Column(override var type: TypeInfo? = null,
                  var table: Table? = null,
                  var name: String? = null,
                  var columnId: Int = 0) : HasSQL, Typed, ColumnTemplate {
    companion object {
        /**
         * The name of the rowid pseudo column.
         */
        const val ROWID: String = "_ROWID_"

        /**
         * This column is not nullable.
         */
        const val NOT_NULLABLE = ResultSetMetaData.columnNoNulls

        /**
         * This column is nullable.
         */
        const val NULLABLE = ResultSetMetaData.columnNullable

        /**
         * It is not know whether this column is nullable.
         */
        const val NULLABLE_UNKNOWN = ResultSetMetaData.columnNullableUnknown

        /**
         * Appends the specified columns to the specified builder.
         * @param builder         * string builder
         * @param columns         * columns
         * @param sqlFlags        * formatting flags
         * @return the specified string builder
         */
        fun writeColumns(builder: StringBuilder, columns: Array<Column>, sqlFlags: Int): StringBuilder {
            return builder.appends(array = columns) { sb, column -> column.getSQL(sb, sqlFlags) }
        }

        /**
         * Appends the specified columns to the specified builder.
         *
         * @param builder string builder
         * @param columns columns
         * @param separator separator
         * @param suffix additional SQL to append after each column
         * @param sqlFlags formatting flags
         * @return the specified string builder
         */
        fun writeColumns(builder: StringBuilder,
                         columns: Array<Column>,
                         separator: String,
                         suffix: String,
                         sqlFlags: Int): StringBuilder {
            return builder.appends(array = columns, separator = separator, postfix = suffix) { sb, column -> column.getSQL(sb, sqlFlags) }
        }
    }

    override fun equals(other: Any?): Boolean {
        return (other == this)
                || (other is Column
                && (table != null && name != null
                && name == other.name && table === other.table))
    }

    override fun hashCode(): Int = if (table == null || name == null) 0 else table!!.id xor name.hashCode()

    open fun getClone(): Column? {
        return Column(name = name, type = type).copy(this)
    }

    var nullable = true

    private var defaultExpression: Expression? = null
    override fun getDefaultExpression(): Expression? = defaultExpression

    override fun getEffectiveDefaultExpression(): Expression? {
        /*
         * Identity columns may not have a default expression and may not use an
         * expression from domain.
         *
         * Generated columns always have an own expression.
         */

        /*
         * Identity columns may not have a default expression and may not use an
         * expression from domain.
         *
         * Generated columns always have an own expression.
         */
        if (sequence != null) {
            return null
        }
        return if (defaultExpression != null) defaultExpression else if (domain != null) domain!!.effectiveDefaultExpression else null
    }

    private var onUpdateExpression: Expression? = null
    override fun getOnUpdateExpression(): Expression? = onUpdateExpression
    override fun getEffectiveOnUpdateExpression(): Expression? {
        TODO("Not yet implemented")
    }

    var identityOptions: SequenceOptions? = null
    var defaultOnNull = false
    var sequence: Sequence? = null
    var isGeneratedAlways = false

    private var generatedTableFilter: GeneratedColumnResolver? = null

    var selectivity = 0
    var comment: String? = null
    var primaryKey = false
    var visible = true
    var rowId = false
    override var domain: Domain? = null


    /**
     * Copy the data of the source column into the current column.
     *
     * @param source the source column
     */
    open fun copy(source: Column) = apply {
        name = source.name
        type = source.type
        domain = source.domain
        // table is not set
        // columnId is not set
        nullable = source.nullable
        defaultExpression = source.defaultExpression
        onUpdateExpression = source.onUpdateExpression
        // identityOptions field is not set
        defaultOnNull = source.defaultOnNull
        sequence = source.sequence
        comment = source.comment
        generatedTableFilter = source.generatedTableFilter
        isGeneratedAlways = source.isGeneratedAlways
        selectivity = source.selectivity
        primaryKey = source.primaryKey
        visible = source.visible
    }

    /**
     * Convert a value to this column's type without precision and scale checks.
     *
     * @param provider the cast information provider
     * @param v the value
     * @return the value
     */
    open fun convert(provider: CastDataProvider?, v: Value): Value? {
        return try {
            v.convertTo(type, provider, this)
        } catch (e: DbException) {
            throw if (e.getErrorCode() == ErrorCode.DATA_CONVERSION_ERROR_1) getDataConversionError(v, e) else e
        }
    }

    /**
     * Returns whether this column is an identity column.
     * @return whether this column is an identity column
     */
    open fun isIdentity(): Boolean = sequence != null || identityOptions != null

    /**
     * Returns whether this column is a generated column.
     * @return whether this column is a generated column
     */
    open fun isGenerated(): Boolean = isGeneratedAlways && defaultExpression != null

    /**
     * Set the default value in the form of a generated expression of other
     * columns.
     *
     * @param expression the computed expression
     */
    open fun setGeneratedExpression(expression: Expression?) = apply {
        isGeneratedAlways = true
        defaultExpression = expression
    }

    /**
     * Set the table and column id.
     *
     * @param table the table
     * @param columnId the column index
     */
    open fun setTable(table: Table?, columnId: Int) = apply {
        this.table = table
        this.columnId = columnId
    }

    private fun getDataConversionError(value: Value, cause: DbException): DbException {
        val builder = StringBuilder().append(value.getTraceSQL()).append(" (")
        if (table != null) builder.append(table!!.name).append(": ")
        builder.append(getCreateSQL()).append(')')
        return DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, cause, builder.toString())
    }

    override fun setDefaultExpression(session: SessionLocal?, defaultExpression: Expression?) {
        // also to test that no column names are used
        this.defaultExpression = defaultExpression
            ?.let { it.optimize(session) }
            ?.let { if (it.isConstant()) ValueExpression.get(it.getValue(session)) else it }
        isGeneratedAlways = false
    }

    override fun setOnUpdateExpression(session: SessionLocal?, onUpdateExpression: Expression?) {
        // also to test that no column names are used
        this.onUpdateExpression = onUpdateExpression
            ?.let { it.optimize(session) }
            ?.let { if (it.isConstant()) ValueExpression.get(it.getValue(session)) else it }
    }

    override fun getSQL(sqlFlags: Int): String {
        return if (rowId) name!! else Parser.quoteIdentifier(name, sqlFlags)!!
    }

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder {
        return if (rowId) builder.append(name) else ParserUtil.quoteIdentifier(builder, name, sqlFlags)
    }

    /**
     * Appends the table name and column name to the specified builder.
     *
     * @param builder the string builder
     * @param sqlFlags formatting flags
     * @return the specified string builder
     */
    open fun getSQLWithTable(builder: StringBuilder, sqlFlags: Int): StringBuilder {
        return getSQL(table!!.getSQL(builder, sqlFlags).append('.'), sqlFlags)
    }

    open fun getCreateSQL(): String? = getCreateSQL(false)

    /**
     * Get this columns part of CREATE TABLE SQL statement.
     *
     * @param forMeta whether this is for the metadata table
     * @return the SQL statement
     */
    open fun getCreateSQL(forMeta: Boolean): String? {
        val builder = StringBuilder()
        if (name != null) ParserUtil.quoteIdentifier(builder, name, HasSQL.DEFAULT_SQL_FLAGS).append(' ')
        return getCreateSQL(builder, forMeta)
    }

    private fun getCreateSQL(builder: StringBuilder, forMeta: Boolean): String? {
        if (domain != null) domain!!.getSQL(builder, HasSQL.DEFAULT_SQL_FLAGS)
        else type!!.getSQL(builder, HasSQL.DEFAULT_SQL_FLAGS)

        if (!visible) builder.append(" INVISIBLE ")

        if (sequence != null) {
            builder.append(" GENERATED ").append(if (isGeneratedAlways) "ALWAYS" else "BY DEFAULT").append(" AS IDENTITY")
            if (!forMeta) sequence!!.getSequenceOptionsSQL(builder.append('(')).append(')')
        } else if (defaultExpression != null) {
            if (isGeneratedAlways) defaultExpression!!.getEnclosedSQL(builder.append(" GENERATED ALWAYS AS "), HasSQL.DEFAULT_SQL_FLAGS)
            else defaultExpression!!.getUnenclosedSQL(builder.append(" DEFAULT "), HasSQL.DEFAULT_SQL_FLAGS)
        }

        if (onUpdateExpression != null) onUpdateExpression!!.getUnenclosedSQL(builder.append(" ON UPDATE "), HasSQL.DEFAULT_SQL_FLAGS)
        if (defaultOnNull) builder.append(" DEFAULT ON NULL")
        if (sequence != null) sequence!!.getSQL(builder.append(" SEQUENCE "), HasSQL.DEFAULT_SQL_FLAGS)
        if (selectivity != 0) builder.append(" SELECTIVITY ").append(selectivity)
        if (comment != null) StringUtils.quoteStringSQL(builder.append(" COMMENT "), comment)
        if (!nullable) builder.append(" NOT NULL")

        return builder.toString()
    }

    /**
     * Validate the value, convert it if required, and update the sequence value
     * if required. If the value is null, the default value (NULL if no default
     * is set) is returned. Domain constraints are validated as well.
     *
     * @param session the session
     * @param value the value or null
     * @param row the row
     * @return the new or converted value
     */
    open fun validateConvertUpdateSequence(session: SessionLocal, value: Value?, row: Row?): Value? {
        var value: Value? = run {
            if (value == null) {
                if (sequence != null) session.getNextValueFor(sequence, null) else null
            } else {
                getDefaultOrGenerated(session, row!!)
            }
        }
        if (value === ValueNull.INSTANCE && !nullable) {
            throw DbException.get(ErrorCode.NULL_NOT_ALLOWED, name!!)
        }
        try {
            value = value?.convertForAssignTo(type, session, name)
        } catch (e: DbException) {
            throw if (e.getErrorCode() == ErrorCode.DATA_CONVERSION_ERROR_1) getDataConversionError(value!!, e) else e
        }
        if (domain != null) {
            domain!!.checkConstraints(session, value)
        }
        if (sequence != null && session.mode.updateSequenceOnManualIdentityInsertion) {
            updateSequenceIfRequired(session, value!!.getLong())
        }
        return value
    }

    private fun getDefaultOrGenerated(session: SessionLocal, row: Row): Value {
        return getEffectiveDefaultExpression()?.let { localDefaultExpression ->
            if (isGeneratedAlways) {
                synchronized(this) {
                    generatedTableFilter!!.set(row)
                    try {
                        localDefaultExpression.getValue(session)
                    } finally {
                        generatedTableFilter!!.set(null)
                    }
                }
            } else localDefaultExpression.getValue(session)
        } ?: ValueNull.INSTANCE
    }

    private fun updateSequenceIfRequired(session: SessionLocal, value: Long) {
        if (sequence!!.cycle == Sequence.Cycle.EXHAUSTED) return
        val current = sequence!!.currentValue
        val inc = sequence!!.increment
        if (inc > 0) {
            if (value < current) return
        } else if (value > current) return

        try {
            sequence!!.modify(value + inc, null, null, null, null, null, null)
        } catch (ex: DbException) {
            if (ex.getErrorCode() == ErrorCode.SEQUENCE_ATTRIBUTES_INVALID_7) return
            throw ex
        }
        sequence!!.flush(session)
    }

    override fun getDefaultSQL(): String? = defaultExpression?.getUnenclosedSQL(StringBuilder(), HasSQL.DEFAULT_SQL_FLAGS)?.toString()

    override fun getOnUpdateSQL(): String? = onUpdateExpression?.getUnenclosedSQL(StringBuilder(), HasSQL.DEFAULT_SQL_FLAGS)?.toString()
    override fun prepareExpressions(session: SessionLocal?) {
        TODO("Not yet implemented")
    }

    /**
     * Check whether the new column is of the same type and not more restricted
     * than this column.
     *
     * @param newColumn the new (target) column
     * @return true if the new column is compatible
     */
    open fun isWideningConversion(newColumn: Column): Boolean {
        val newType = newColumn.type
        val valueType: Int = type!!.valueType
        if (valueType != newType?.valueType) return false

        val precision: Long = type!!.precision
        val newPrecision: Long = newType.precision
        if (precision > newPrecision
            || precision < newPrecision
            && (valueType == Value.CHAR || valueType == Value.BINARY)) {
            return false
        }

        if (type!!.scale != newType.scale) return false
        if (type!!.extTypeInfo != newType.extTypeInfo) return false
        if (nullable && !newColumn.nullable) return false
        if (primaryKey != newColumn.primaryKey) return false
        if (identityOptions != null || newColumn.identityOptions != null) return false
        if (domain != newColumn.domain) return false
        if (defaultExpression != null || newColumn.defaultExpression != null) return false
        if (isGeneratedAlways || newColumn.isGeneratedAlways) return false
        return !(onUpdateExpression != null || newColumn.onUpdateExpression != null)
    }


}

