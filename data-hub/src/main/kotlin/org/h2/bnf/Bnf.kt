package org.h2.bnf

import java.io.IOException
import java.io.Reader
import java.sql.SQLException

/**
 * This class can read a file that is similar to BNF (Backus-Naur form).
 * It is made especially to support SQL grammar.
 */
class Bnf {
    val ruleMap: MutableMap<String, RuleHead> = hashMapOf()
    var syntax: String? = null
    var currentToken: String? = null
    var tokens: Array<String>? = null
    var firstChar: Char? = null
    var index: Int = 0
    var lastRepeat: Rule? = null
    var statements: ArrayList<RuleHead>? = null
    var currentTopic: String? = null

    companion object {

        @JvmStatic
        @Throws(SQLException::class)
        @Throws(IOException::class)
        fun getInstance(csv: Reader?): Bnf {
            val bnf = Bnf()

        }
    }

    @Throws(SQLException::class)
    @Throws(IOException::class)
    fun parse(reader: Reader) {

    }
}