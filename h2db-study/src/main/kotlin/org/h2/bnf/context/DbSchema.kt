package org.h2.bnf.context

import org.h2.engine.SysProperties
import org.h2.util.StringUtils
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import java.sql.SQLException

/**
 * Contains meta data information about a database schema.
 * This class is used by the H2 Console.
 */
class DbSchema(val contents: DbContents,
               val name: String?,
               val isDefault: Boolean) {
    //    val name: String? // The schema name
    //    val isDefault: Boolean //True if this is the default schema for this database.

    // True if this is a system schema (for example the INFORMATION_SCHEMA)
    val isSystem: Boolean = name.isNullOrBlank()
            || "INFORMATION_SCHEMA" == name
            || (!contents.isH2 && StringUtils.toUpperEnglish(name).startsWith("INFO"))
            || (contents.isPostgreSQL && StringUtils.toUpperEnglish(name).startsWith("PG_"))
            || (contents.isDerby && StringUtils.toUpperEnglish(name).startsWith("SYS"))
    val quotedName: String? = contents.quoteIdentifier(name)// The quoted schema name.

    //    val contents: DbContents // The database content container.
    lateinit var tables: Array<DbTableOrView> // The table list.
    lateinit var procedures: Array<DbProcedure> // The procedures list

    /**
     * Read all tables for this schema from the database meta data.
     * @param meta the database meta data
     * @param tableTypes the table types to read
     */
    @Throws(SQLException::class)
    fun readTables(meta: DatabaseMetaData, tableTypes: Array<String>) {
        val rs: ResultSet = meta.getTables(null, name, null, tableTypes)
        rs.use { it ->
            tables = generateSequence {
                if (it.next()) DbTableOrView(this, it) else null
            }.toList().toTypedArray()
        }

        if (tables.size >= SysProperties.CONSOLE_MAX_TABLES_LIST_COLUMNS) return
        if (contents.isH2) {
            meta.connection.prepareStatement("""
                SELECT COLUMN_NAME, ORDINAL_POSITION, COLUMN_TYPE
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
            """.trimIndent()).use { ps ->
                for (tbl in tables) {
                    tbl.readColumns(meta, ps)
                }
            }
        }
    }

    /**
     * Read all procedures in the database.
     * @param meta the database meta data
     * @throws SQLException Error while fetching procedures
     */
    @Throws(SQLException::class)
    fun readProcedures(meta: DatabaseMetaData) {
        val rs: ResultSet = meta.getProcedures(null, name, null)
        rs.use { it ->
            procedures = generateSequence {
                if (it.next()) DbProcedure(this, it) else null
            }.toList().toTypedArray()
        }
        if (procedures.size < SysProperties.CONSOLE_MAX_PROCEDURES_LIST_COLUMNS) {
            for (procedure in procedures) {
                procedure.readParameters(meta)
            }
        }
    }

}