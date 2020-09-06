package org.h2.bnf

import org.h2.bnf.context.DbSchema
import org.h2.bnf.context.DbTableOrView
import org.h2.util.StringUtils
import java.util.concurrent.TimeUnit

/**
 * A query context object. It contains the list of table and alias objects.
 * Used for autocomplete.
 */
class Sentence {
    companion object {
        /**
         * This token type means the possible choices of the item depend on the
         * context. For example the item represents a table name of the current
         * database.
         */
        const val CONTEXT: Int = 0

        /**
         * The token type for a keyword
         */
        const val KEYWORD: Int = 1

        /**
         * The token type for a keyword
         */
        const val FUNCTION: Int = 2

        const val MAX_PROCESSING_TIME: Long = 100L

        /**
         * The map of next tokens in the form type#tokenName token
         */
        val next: HashMap<String, String> = HashMap()
    }

    /**
     * The complete query string.
     */
    var query: String = ""
        set(value: String) {
            if (value != field) {
                field = value
                this.queryUpper = StringUtils.toUpperEnglish(field)
            }
        }

    /**
     * The uppercase version of the query string.
     */
    lateinit var queryUpper: String
        private set

    var stopAtNs: Long = -1L

    var lastMatchedSchema: DbSchema? = null
    lateinit var lastMatchedTable: DbTableOrView
    var lastTable: DbTableOrView? = null
    val tables: HashSet<DbTableOrView> by lazy { HashSet() }
    val aliases: HashMap<String, DbTableOrView?> by lazy { HashMap() }

    /**
     * Start the timer to make sure processing doesn't take too long.
     */
    fun start() {
        stopAtNs = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(MAX_PROCESSING_TIME)
    }

    /**
     * Check if it's time to stop processing.
     * Processing auto-complete shouldn't take more than a few milliseconds.
     * If processing is stopped, this method throws an IllegalStateException
     */
    fun stopIfRequired() {
        if (System.nanoTime() > stopAtNs) throw  IllegalStateException()
    }

    /**
     * Add a word to the set of next tokens.
     * @param n the token name
     * @param string an example text
     * @param type the token type
     */
    fun add(n: String, string: String, type: Int) = next.put("$type#$n", string)

    /**
     * Add an alias name and object
     * @param alias the alias name
     * @param table the alias table
     */
    fun addAlias(alias: String, table: DbTableOrView?) = aliases.put(alias, table)

    /**
     * Add a table.
     * @param table the table
     */
    fun addTable(table: DbTableOrView) {
        lastTable = table
        tables.add(table)
    }
}