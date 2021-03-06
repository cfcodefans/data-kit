package org.h2.bnf

import org.h2.tools.Csv
import org.h2.util.StringUtils
import org.h2.util.Utils
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.sql.ResultSet
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

        /**
         * Create an instance using the grammar specified in the CSV file.
         * @param csv if not specified, the help.csv is used
         * @return a new instance
         */
        @JvmStatic
        @Throws(SQLException::class, IOException::class)
        fun getInstance(csv: Reader?): Bnf {
            val bnf = Bnf()
            bnf.parse(csv ?: InputStreamReader(ByteArrayInputStream(Utils.getResource("/org/h2/res/help.csv"))))
            return bnf
        }
    }

    private fun addRule(topic: String, section: String, rule: Rule): RuleHead? {
        val head = RuleHead(section, topic, rule)
        val key = StringUtils.toLowerEnglish(topic.trim { it <= ' ' }.replace(' ', '_'))
        if (ruleMap[key] != null) {
            throw AssertionError("already exists: $topic")
        }
        ruleMap[key] = head
        return head
    }

    private fun addFixedRule(name: String, fixedType: Int) {
        val rule: Rule = RuleFixed(fixedType)
        addRule(name, "Fixed", rule)
    }

    @Throws(SQLException::class, IOException::class)
    fun parse(reader: Reader) {
        val csv: Csv = Csv()
        csv.lineComment = '#'
        val rs: ResultSet = csv.read(reader, null)!!
        while (rs.next()) {

        }

        addRule("@func@", "Function", functions)
        addFixedRule("@ymd@", RuleFixed.YMD)
        addFixedRule("@hms@", RuleFixed.HMS)
        addFixedRule("@nanos@", RuleFixed.NANOS)
        addFixedRule("anything_except_single_quote", RuleFixed.ANY_EXCEPT_SINGLE_QUOTE)
        addFixedRule("anything_except_double_quote", RuleFixed.ANY_EXCEPT_DOUBLE_QUOTE)
        addFixedRule("anything_until_end_of_line", RuleFixed.ANY_UNTIL_EOL)
        addFixedRule("anything_until_end_comment", RuleFixed.ANY_UNTIL_END)
        addFixedRule("anything_except_two_dollar_signs", RuleFixed.ANY_EXCEPT_2_DOLLAR)
        addFixedRule("anything", RuleFixed.ANY_WORD)
        addFixedRule("@hex_start@", RuleFixed.HEX_START)
        addFixedRule("@concat@", RuleFixed.CONCAT)
        addFixedRule("@az_@", RuleFixed.AZ_UNDERSCORE)
        addFixedRule("@af@", RuleFixed.AF)
        addFixedRule("@digit@", RuleFixed.DIGIT)
        addFixedRule("@open_bracket@", RuleFixed.OPEN_BRACKET)
        addFixedRule("@close_bracket@", RuleFixed.CLOSE_BRACKET)
        addFixedRule("json_text", RuleFixed.JSON_TEXT)
    }


}