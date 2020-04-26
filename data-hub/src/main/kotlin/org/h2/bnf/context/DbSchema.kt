package org.h2.bnf.context

/**
 * Contains meta data information about a database schema.
 * This class is used by the H2 Console.
 */
class DbSchema(val contents: DbContents,
               val name: String,
               val isDefault: Boolean) {
    //    val name: String? // The schema name
    //    val isDefault: Boolean //True if this is the default schema for this database.
    val isSystem: Boolean // True if this is a system schema (for example the INFORMATION_SCHEMA)
    val quotedName: String = contents.quoteIdentifier(name)// The quoted schema name.

    //    val contents: DbContents // The database content container.
    private lateinit var tables: Array<DbTableOrView> // The table list.
    private lateinit var procedures: Array<DbProcedure> // The procedures list


}