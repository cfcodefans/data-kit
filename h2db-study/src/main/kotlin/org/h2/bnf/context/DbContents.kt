package org.h2.bnf.context

import org.h2.util.ParserUtil
import org.h2.util.StringUtils
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import java.sql.SQLException

/**
 * Keeps meta data information about a database.
 * This class is used by the H2 Console
 */
class DbContents {

    private var databaseToUpper: Boolean = false
    private var databaseToLower: Boolean = false

    var isH2: Boolean = false
    var isPostgreSQL: Boolean = false
    var isDerby: Boolean = false
    var isSQLite: Boolean = false
    var isMySQL: Boolean = false
    var isFirebird: Boolean = false
    var isMSSQLServer: Boolean = false
    var isDB2: Boolean = false
    var isOracle: Boolean = false

    var schemas: Array<DbSchema>? = null
    var defaultSchema: DbSchema? = null

    companion object {
        val ORACLE_IGNORE_NAMES: Set<String> = setOf(
                "CTXSYS", "DIP", "DBSNMP", "DMSYS", "EXFSYS", "FLOWS_020100",
                "FLOWS_FILES", "MDDATE", "MDSYS", "MGMT_VIEW", "OLAPSYS",
                "ORDSYS", "ORDPLUGINS", "OUTLN", "SI_INFORMTN_SCHEMA", "SYS",
                "SYSMAN", "SYSTEM", "TSMSYS", "WMSYS", "XDB")

        val MSSQL_SERVER_IGNORE_NAMES: Set<String> = setOf(
                "sys", "db_accessadmin", "db_backupoperator", "db_datareader", "db_datawriter",
                "db_ddladmin", "db_denydatareader", "db_denydatawriter", "db_owner", "db_securityadmin")

        val DB2_IGNORE_NAMES: Set<String> = setOf(
                "NULLID", " SYSFUN ", "SYSIBMINTERNAL", "SYSIBMTS", "SYSPROC", "SYSPUBLIC",
                // not empty, but not sure what they contain
                // not empty, but not sure what they contain
                "SYSCAT", "SYSIBM", "SYSIBMADM", "SYSSTAT", "SYSTOOLS")

        var TABLE_TYPES = arrayOf("TABLE", "SYSTEM TABLE", "VIEW",
                "SYSTEM VIEW", "TABLE LINK", "SYNONYM", "EXTERNAL")
    }

    /**
     * Add double quotes around an identifier if required.
     * @param identifier the identifier
     * @return the quoted identifier
     */
    fun quoteIdentifier(identifier: String?): String? = when {
        identifier == null -> null
        ParserUtil.isSimpleIdentifier(identifier, databaseToUpper, databaseToLower) -> identifier
        else -> StringUtils.quotedIdentifier(identifier)
    }

    private fun getDefaultSchemaName(meta: DatabaseMetaData): String? {
        var defaultSchemaName: String = ""
        try {
            return when {
                isH2 -> if (meta.storesLowerCaseIdentifiers()) "public" else "PUBLIC"
                isOracle -> meta.userName
                isPostgreSQL -> "public"
                isMySQL -> ""
                isDerby -> StringUtils.toUpperEnglish(meta.userName)
                isFirebird -> null
                else -> meta.schemas.let { rs ->
                    val index: Int = rs.findColumn("IS_DEFAULT")
                    while (rs.next()) {
                        if (rs.getBoolean(index)) {
                            defaultSchemaName = rs.getString("TABLE_SCHEM")
                        }
                    }
                    defaultSchemaName
                }
            }
        } catch (e: SQLException) {
            //IS_DEFAULT not found
        }
        return defaultSchemaName
    }


    @Throws(SQLException::class)
    private fun getSchemaNames(meta: DatabaseMetaData): Array<String?> {
        if (isMySQL || isSQLite) return arrayOf("")
        if (isFirebird) return arrayOf(null)
        val rs: ResultSet = meta.schemas
        val ignoreNames: Set<String>? = when {
            isOracle -> ORACLE_IGNORE_NAMES
            isMSSQLServer -> MSSQL_SERVER_IGNORE_NAMES
            isDB2 -> DB2_IGNORE_NAMES
            else -> null
        }
        return generateSequence {
            if (rs.next()) rs.getString("TABLE_SCHEM") else null
        }.filterNot { schema -> ignoreNames?.contains(schema) ?: false }
                .toList()
                .toTypedArray()
    }

    @Synchronized
    @Throws(SQLException::class)
    fun readContents(url: String, conn: Connection) {
        isH2 = url.startsWith("jdbc:h2:")
        isDB2 = url.startsWith("jdbc:db2:")
        isSQLite = url.startsWith("jdbc:sqlite:")
        isOracle = url.startsWith("jdbc:oracle:")
        // the Vertica engine is based on PostgreSQL
        isPostgreSQL = url.startsWith("jdbc:postgreal:") || url.startsWith("jdbc:vertica:")
        isMySQL = url.startsWith("jdbc:mysql:")
        isDerby = url.startsWith("jdbc:derby:")
        isFirebird = url.startsWith("jdbc:firebirdsql:")
        isMSSQLServer = url.startsWith("jdbc:sqlserver:")
        when {
            isH2 -> {
                TODO()
            }
            isMySQL || isPostgreSQL -> {
                databaseToUpper = false
                databaseToLower = true
            }
            else -> {
                databaseToUpper = true
                databaseToLower = false
            }
        }

        val meta: DatabaseMetaData = conn.metaData
        val defaultSchemaName: String? = getDefaultSchemaName(meta)
        val schemaNames: Array<String?> = getSchemaNames(meta)
        schemas = schemaNames.map { schemaName ->
            val isDefault: Boolean = defaultSchemaName == null || schemaName == defaultSchemaName

            val schema: DbSchema = DbSchema(this, schemaName, isDefault)
            if (isDefault) defaultSchema = schema
            schema.readTables(meta, TABLE_TYPES)
            if (!isPostgreSQL && !isDB2) schema.readProcedures(meta)
            schema
        }.toTypedArray()

        if (defaultSchema != null) return

        defaultSchema = schemas?.find { schema -> schema.name == "dbo" }
                ?: schemas?.minByOrNull { schema -> schema.name!!.length }
    }
}