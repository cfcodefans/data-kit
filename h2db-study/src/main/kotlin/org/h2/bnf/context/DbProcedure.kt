package org.h2.bnf.context

import java.sql.DatabaseMetaData
import java.sql.ResultSet
import java.sql.SQLException

/**
 * Contains meta data information about a procedure.
 * This class is used by the H2 Console.
 */
class DbProcedure(val schema: DbSchema, rs: ResultSet) {
    val name: String = rs.getString("PROCEDURE_NAME")
    val quotedName: String? = schema.contents.quoteIdentifier(this.name)
    val returnResult: Boolean = rs.getInt("PROCEDURE_TYPE") == DatabaseMetaData.procedureReturnsResult
    lateinit var parameters: Array<DbColumn>
        private set

    /**
     * Read the column for this table from the database meta data.
     * @param meta the database meta data
     */
    @Throws(SQLException::class)
    fun readParameters(meta: DatabaseMetaData) {
        val rs: ResultSet = meta.getProcedureColumns(null, schema.name, name, null)
        rs.use { _ ->
            parameters = generateSequence {
                if (rs.next()) DbColumn.getProcedureColumn(schema.contents, rs) else null
            }.filter { col -> col.position > 0 }
                    .toList()
                    .sortedBy { col -> col.position }
                    .toTypedArray()
        }
        //Store the parameter in the good position [1-n]
    }
}