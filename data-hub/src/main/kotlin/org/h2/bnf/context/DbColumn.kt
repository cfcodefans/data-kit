package org.h2.bnf.context

import java.sql.DatabaseMetaData
import java.sql.ResultSet
import java.sql.SQLException

/**
 * Keeps the meta data information of a column.
 * This class is used by the H2 Console
 */
class DbColumn {

    companion object {
        /**
         * Create a column from a DatabaseMetaData.getProcedureColumns row.
         * @param contents the database contents
         * @param rs the result set
         * @return the column
         */
        @JvmStatic
        @Throws(SQLException::class)
        fun getProcedureColumn(contents: DbContents, rs: ResultSet): DbColumn = DbColumn(contents, rs, true)

        /**
         *  Create a column from a DatabaseMetaData.getColumns row.
         *  @param contents the database contents
         * @param rs the result set
         * @return the column
         */
        @JvmStatic
        @Throws(SQLException::class)
        fun getColumn(contents: DbContents, rs: ResultSet): DbColumn = DbColumn(contents, rs, false)
    }

    lateinit var name: String
    var quotedName: String?
    var dataType: String = ""
        private set
    var position: Int = -1

    @Throws(SQLException::class)
    constructor(contents: DbContents, rs: ResultSet, procedureColumn: Boolean) {
        name = rs.getString("COLUMN_NAME")
        quotedName = contents.quoteIdentifier(name)
        position = rs.getInt("ORDINAL_POSITION")
        if (contents.isH2 && !procedureColumn) {
            dataType = rs.getString("COLUMN_TYPE")
            return
        }
        // a procedures column size is identified by PRECISION, for table this
        // is COLUMN_SIZE
        val (precisionColumnName, scaleColumnName) = if (procedureColumn) {
            "PRECISION" to "SCALE"
        } else {
            "COLUMN_SIZE" to "DECIMAL_DIGITS"
        }

        val precision: Int = rs.getInt(precisionColumnName)
        if (precision > 0 && !contents.isSQLite) {
            val scale: Int = rs.getInt(scaleColumnName)
            dataType += "${rs.getString("TYPE_NAME")} ($precision ${if (scale > 0) ",$scale" else ""})"
        }
        if (rs.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls) {
            dataType += " NOT NULL"
        }
    }
}