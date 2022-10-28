package org.h2.table

import org.h2.command.ddl.CreateTableData
import org.h2.engine.Database
import org.h2.index.IndexType
import org.h2.result.SearchRow
import org.h2.result.SortOrder
import org.h2.util.HasSQL
import org.h2.util.StringUtils
import org.h2.value.Value

/**
 * The base class of a regular table, or a user defined table.
 */
abstract class TableBase(data: CreateTableData) : Table(schema = data.schema,
    id = data.id,
    name = data.tableName,
    persistIndexes = data.persistIndexes,
    persistData = data.persistData) {

    companion object {
        /**
         * Returns main index column if index is an primary key index and has only
         * one column with _ROWID_ compatible data type.
         *
         * @param indexType type of an index
         * @param cols columns of the index
         * @return main index column or [SearchRow.ROWID_INDEX]
         */
        fun getMainIndexColumn(indexType: IndexType, cols: Array<IndexColumn>): Int {
            if (!indexType.isPrimaryKey || cols.size != 1) return SearchRow.ROWID_INDEX

            val first = cols[0]
            return if (first.sortType and SortOrder.DESCENDING != 0) {
                SearchRow.ROWID_INDEX
            } else when (first.column.type.valueType) {
                Value.TINYINT,
                Value.SMALLINT,
                Value.INTEGER,
                Value.BIGINT -> first.column.columnId

                else -> SearchRow.ROWID_INDEX
            }
        }
    }

    /**
     * The table engine used (null for regular tables).
     */
    private val tableEngine: String? = data.tableEngine

    /** Provided table parameters  */
    private val tableEngineParams: List<String> = data.tableEngineParams ?: emptyList()

    private val globalTemporary = data.globalTemporary

    init {
        super.temporary = data.temporary
        super.columns = data.columns.toTypedArray()
    }

    override fun getDropSQL(): String? {
        val builder = StringBuilder("DROP TABLE IF EXISTS ")
        getSQL(builder, HasSQL.DEFAULT_SQL_FLAGS).append(" CASCADE")
        return builder.toString()
    }

    private fun getCreateSQL(forMeta: Boolean): String? {
        val db: Database = database
            ?: // closed
            return null
        val buff = StringBuilder("CREATE ")
        if (temporary) {
            buff.append(if (isGlobalTemporary()) "GLOBAL " else "LOCAL ")
                .append("TEMPORARY ")
        } else buff.append(if (persistIndexes) "CACHED " else "MEMORY ")

        buff.append("TABLE ")
        if (isHidden()) {
            buff.append("IF NOT EXISTS ")
        }
        getSQL(buff, HasSQL.DEFAULT_SQL_FLAGS)
        if (comment != null) {
            buff.append(" COMMENT ")
            StringUtils.quoteStringSQL(buff, comment)
        }
        buff.append("(\n    ")

        columns.joinTo(buffer = buff, separator = ",\n    ") { col -> col.getCreateSQL(forMeta) }
        buff.append("\n)")

        if (tableEngine != null) {
            val d = db.settings.defaultTableEngine
            if (d == null || !tableEngine.endsWith(d)) {
                buff.append("\nENGINE ")
                StringUtils.quoteIdentifier(buff, tableEngine)
            }
        }

        if (tableEngineParams.isNotEmpty()) {
            buff.append("\nWITH ")
            var i = 0;
            val l = tableEngineParams.size; while (i < l) {
                if (i > 0) buff.append(", ")

                StringUtils.quoteIdentifier(buff, tableEngineParams[i])
                i++; }
        }
        if (!persistIndexes && !persistData) buff.append("\nNOT PERSISTENT")
        if (isHidden()) buff.append("\nHIDDEN")
        return buff.toString()
    }
}