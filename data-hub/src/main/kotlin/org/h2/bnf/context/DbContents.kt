package org.h2.bnf.context

import org.h2.util.ParserUtil
import org.h2.util.StringUtils

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
}