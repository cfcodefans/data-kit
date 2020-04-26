package org.h2.bnf.context

/**
 * Keeps meta data information about a database.
 * This class is used by the H2 Console
 */
class DbContents {

    private var databaseToUpper: Boolean = false
    private var databaseToLower: Boolean = false

    /**
     * Add double quotes around an identifier if required.
     * @param identifier the identifier
     * @return the quoted identifier
     */
    fun quoteIdentifier(identifier: String?): String? {
        if (identifier == null) return null
        if (ParserUtil)
    }
}