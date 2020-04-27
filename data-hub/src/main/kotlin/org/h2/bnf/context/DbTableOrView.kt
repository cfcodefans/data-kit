package org.h2.bnf.context

import java.sql.DatabaseMetaData
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

/**
 * Contains meta data information about a table or a view.
 * This class is used by the H2 Console.
 */
class DbTableOrView(val schema: DbSchema, rs: ResultSet) {
    val name: String = rs.getString("TABLE_NAME")
    val isView: Boolean = "VIEW" == rs.getString("TABLE_TYPE")
    val quotedName: String? = schema.contents.quoteIdentifier(this.name)

    /**
     * The column list.
     */
    lateinit var columns: Array<DbColumn>

    /**
     * Read the column for this table from the database meta data.
     * @param meta the database meta data
     * @param ps prepared statement with custom query for H2 database, null for others
     */
    @Throws(SQLException::class)
    fun readColumns(meta: DatabaseMetaData, ps: PreparedStatement) {
        val rs: ResultSet = if (schema.contents.isH2) {
            ps.setString(1, schema.name)
            ps.setString(2, name)
            ps.executeQuery()
        } else {
            meta.getColumns(null, schema.name, name, null)
        }

        rs.use { _ ->
            columns = generateSequence {
                if (rs.next()) DbColumn.getColumn(schema.contents, rs) else null
            }.toList().toTypedArray()
        }
    }
}