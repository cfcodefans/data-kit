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
import java.util.*

/**
 * This class can read a file that is similar to BNF (Backus-Naur form).
 * It is made especially to support SQL grammar.
 */
open class Bnf(
        /**
         * The rule map. The key is lowercase, and all spaces
         * are replaces with underscore.
         */
        val ruleMap: MutableMap<String, RuleHead> = hashMapOf(),
        var syntax: String? = null,
        var currentToken: String? = null,
        var tokens: Array<String>? = null,
        var firstChar: Char? = null,
        var index: Int = 0,
        var lastRepeat: Rule? = null,
        var statements: ArrayList<RuleHead>? = null,
        var currentTopic: String? = null) {


    companion object {

        /**
         * Create an instance using the grammar specified in the CSV file.
         * @param csv if not specified, the help.csv is used
         * @return a new instance
         */
        @JvmStatic
        @Throws(SQLException::class, IOException::class)
        fun getInstance(csv: Reader?): Bnf {
            return Bnf().parse(csv ?: InputStreamReader(ByteArrayInputStream(Utils.getResource("/org/h2/res/help.csv"))))
        }

        /**
         * Get the tokenizer for the given syntax.
         *
         * @param s the syntax
         * @return the tokenizer
         */
        fun getTokenizer(s: String?): StringTokenizer {
            return StringTokenizer(s, " [](){}|.,\r\n<>:-+*/=\"!'$", true)
        }

        /**
         * Check whether the statement starts with a whitespace.
         *
         * @param s the statement
         * @return if the statement is not empty and starts with a whitespace
         */
        fun startWithSpace(s: String): Boolean {
            return s.isNotEmpty() && Character.isWhitespace(s[0])
        }


        /**
         * Convert convert ruleLink to rule_link.
         *
         * @param token the token
         * @return the rule map key
         */
        fun getRuleMapKey(token: String): String {
            val buff = StringBuilder(token.length)
            for (ch in token.toCharArray()) {
                if (Character.isUpperCase(ch)) {
                    buff.append('_').append(Character.toLowerCase(ch))
                } else {
                    buff.append(ch)
                }
            }
            return buff.toString()
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
        addRule(name, "Fixed", RuleFixed(fixedType))
    }

    @Throws(SQLException::class, IOException::class)
    fun parse(reader: Reader) = apply {
        var functions: Rule? = null

        val csv: Csv = Csv()
        csv.lineComment = '#'
        val rs: ResultSet = csv.read(reader, null)!!

        while (rs.next()) {
            val section: String = rs.getString("SECTION").trim()
            if (section.startsWith("System")) continue

            val topic: String = rs.getString("TOPIC")
            syntax = rs.getString("SYNTAX").trim { it <= ' ' }
            currentTopic = section
            tokens = tokenize(syntax!!)
            index = 0

            var rule = parseRule()!!
            if (section.startsWith("Command")) {
                rule = RuleList(first = rule, next = RuleElement(";\n\n", currentTopic!!), or = false)
            }
            val head = addRule(topic, section, rule)
            if (section.startsWith("Function")) {
                functions = functions?.let { RuleList(first = rule, next = it, or = true) } ?: rule
            } else if (section.startsWith("Commands")) {
                statements!!.add(head!!)
            }
        }

        addRule("@func@", "Function", functions!!)
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

    private fun tokenize(syntax: String): Array<String> {
        var syntax: String? = StringUtils.replaceAll(syntax, "yyyy-MM-dd", "@ymd@")
        syntax = StringUtils.replaceAll(syntax, "hh:mm:ss", "@hms@")
        syntax = StringUtils.replaceAll(syntax, "hh:mm", "@hms@")
        syntax = StringUtils.replaceAll(syntax, "mm:ss", "@hms@")
        syntax = StringUtils.replaceAll(syntax, "nnnnnnnnn", "@nanos@")
        syntax = StringUtils.replaceAll(syntax, "function", "@func@")
        syntax = StringUtils.replaceAll(syntax, "0x", "@hexStart@")
        syntax = StringUtils.replaceAll(syntax, ",...", "@commaDots@")
        syntax = StringUtils.replaceAll(syntax, "...", "@dots@")
        syntax = StringUtils.replaceAll(syntax, "||", "@concat@")
        syntax = StringUtils.replaceAll(syntax, "a-z|_", "@az_@")
        syntax = StringUtils.replaceAll(syntax, "A-Z|_", "@az_@")
        syntax = StringUtils.replaceAll(syntax, "A-F", "@af@")
        syntax = StringUtils.replaceAll(syntax, "0-9", "@digit@")
        syntax = StringUtils.replaceAll(syntax, "'['", "@openBracket@")
        syntax = StringUtils.replaceAll(syntax, "']'", "@closeBracket@")

        val tokenizer: StringTokenizer = getTokenizer(syntax)!!
        return Sequence(tokenizer::asIterator)
                .map { StringUtils.cache(it as String)!! }
                .filterNot { s -> s.length == 1 && " \r\n".indexOf(s[0]) >= 0 }
                .toList()
                .toTypedArray()
    }

    private fun read() {
        if (index < tokens!!.size) {
            currentToken = tokens!![index++]
            firstChar = currentToken!![0]
        } else {
            currentToken = ""
            firstChar = 0.toChar()
        }
    }

    private fun parseRule(): Rule? {
        read()
        return parseOr()
    }

    private fun parseOr(): Rule {
        var r: Rule = parseList()
        if (firstChar == '|') {
            read()
            r = RuleList(first = r, next = parseOr(), or = true)
        }
        lastRepeat = r
        return r
    }

    private fun parseList(): Rule {
        var r: Rule = parseToken()
        if (firstChar != '|' && firstChar != ']' && firstChar != '}' && firstChar!!.code != 0) {
            r = RuleList(first = r, next = parseList(), or = false)
        }
        lastRepeat = r
        return r
    }

    private fun parseToken(): Rule {
        var r: Rule?
        if (firstChar!! >= 'A' && firstChar!! <= 'Z' || firstChar!! >= 'a' && firstChar!! <= 'z') {
            // r = new RuleElement(currentToken+ " syntax:" + syntax);
            r = RuleElement(currentToken!!, currentTopic!!)
        } else if (firstChar == '[') {
            read()
            val r2 = parseOr()
            r = RuleOptional(r2)
            if (firstChar != ']') {
                throw AssertionError("""expected ], got $currentToken syntax:$syntax""")
            }
        } else if (firstChar == '{') {
            read()
            r = parseOr()
            if (firstChar != '}') {
                throw AssertionError("""expected }, got $currentToken syntax:$syntax""")
            }
        } else if ("@commaDots@" == currentToken) {
            r = RuleList(first = RuleElement(",", currentTopic!!), next = lastRepeat!!, or = false)
            r = RuleRepeat(r, true)
        } else if ("@dots@" == currentToken) {
            r = RuleRepeat(lastRepeat!!, false)
        } else {
            r = RuleElement(currentToken!!, currentTopic!!)
        }
        lastRepeat = r
        read()
        return r!!
    }
}