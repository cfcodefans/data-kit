package org.h2.bnf.context

import java.sql.ResultSet

/**
 * Contains meta data information about a procedure.
 * This class is used by the H2 Console.
 */
class DbProcedure(val schema: DbSchema, rs: ResultSet) {
    val name: String = rs.getString("PROCEDURE_NAME")
    val quotedName: String? = schema.contents.quoteIdentifier(this.name)

}