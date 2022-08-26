package org.h2.command

import org.h2.util.ParserUtil

/**
 * The parser is used to convert a SQL statement string to an command object.
 *
 * @author Thomas Mueller
 * @author Noel Grandin
 * @author Nicolas Fortin, Atelier SIG, IRSTV FR CNRS 24888
 */
open class Parser {
    companion object {
        private const val WITH_STATEMENT_SUPPORTS_LIMITED_SUB_STATEMENTS = "WITH statement supports only SELECT, TABLE, VALUES, CREATE TABLE, INSERT, UPDATE, MERGE or DELETE statements"

        // used during the tokenizer phase
        private const val CHAR_END = 1  // used during the tokenizer phase
        private const val CHAR_VALUE = 2  // used during the tokenizer phase
        private const val CHAR_QUOTED = 3
        private const val CHAR_NAME = 4
        private const val CHAR_SPECIAL_1 = 5
        private const val CHAR_SPECIAL_2 = 6
        private const val CHAR_STRING = 7
        private const val CHAR_DOT = 8
        private const val CHAR_DOLLAR_QUOTED_STRING = 9

        // these are token types, see also types in ParserUtil

        // these are token types, see also types in ParserUtil
        /**
         * Token with parameter.
         */
        private val PARAMETER: Int = ParserUtil.LAST_KEYWORD + 1

        /**
         * End of input.
         */
        private val END_OF_INPUT = PARAMETER + 1

        /**
         * Token with literal.
         */
        private val LITERAL = END_OF_INPUT + 1

        /**
         * The token "=".
         */
        private val EQUAL = LITERAL + 1

        /**
         * The token ">=".
         */
        private val BIGGER_EQUAL = EQUAL + 1

        /**
         * The token ">".
         */
        private val BIGGER = BIGGER_EQUAL + 1

        /**
         * The token "<".
         */
        private val SMALLER = BIGGER + 1

        /**
         * The token "<=".
         */
        private val SMALLER_EQUAL = SMALLER + 1

        /**
         * The token "<>" or "!=".
         */
        private val NOT_EQUAL = SMALLER_EQUAL + 1

        /**
         * The token "@".
         */
        private val AT = NOT_EQUAL + 1

        /**
         * The token "-".
         */
        private val MINUS_SIGN = AT + 1

        /**
         * The token "+".
         */
        private val PLUS_SIGN = MINUS_SIGN + 1

        /**
         * The token "||".
         */
        private val CONCATENATION = PLUS_SIGN + 1

        /**
         * The token "(".
         */
        private val OPEN_PAREN = CONCATENATION + 1

        /**
         * The token ")".
         */
        private val CLOSE_PAREN = OPEN_PAREN + 1

        /**
         * The token &amp;.
         */
        private val AMPERSAND = CLOSE_PAREN + 1

        /**
         * The token "&amp;&amp;".
         */
        private val SPATIAL_INTERSECTS = AMPERSAND + 1

        /**
         * The token "*".
         */
        private val ASTERISK = SPATIAL_INTERSECTS + 1

        /**
         * The token ",".
         */
        private val COMMA = ASTERISK + 1

        /**
         * The token ".".
         */
        private val DOT = COMMA + 1

        /**
         * The token "{".
         */
        private val OPEN_BRACE = DOT + 1

        /**
         * The token "}".
         */
        private val CLOSE_BRACE = OPEN_BRACE + 1

        /**
         * The token "/".
         */
        private val SLASH = CLOSE_BRACE + 1

        /**
         * The token "%".
         */
        private val PERCENT = SLASH + 1

        /**
         * The token ";".
         */
        private val SEMICOLON = PERCENT + 1

        /**
         * The token ":".
         */
        private val COLON = SEMICOLON + 1

        /**
         * The token "[".
         */
        private val OPEN_BRACKET = COLON + 1

        /**
         * The token "]".
         */
        private val CLOSE_BRACKET = OPEN_BRACKET + 1

        /**
         * The token "~".
         */
        private val TILDE = CLOSE_BRACKET + 1

        /**
         * The token "::".
         */
        private val COLON_COLON = TILDE + 1

        /**
         * The token ":=".
         */
        private val COLON_EQ = COLON_COLON + 1

        /**
         * The token "!~".
         */
        private val NOT_TILDE = COLON_EQ + 1

        private val TOKENS = arrayOf( // Unused
            null,  // KEYWORD
            null,  // IDENTIFIER
            null,  // ALL
            "ALL",  // AND
            "AND",  // ANY
            "ANY",  // ARRAY
            "ARRAY",  // AS
            "AS",  // ASYMMETRIC
            "ASYMMETRIC",  // AUTHORIZATION
            "AUTHORIZATION",  // BETWEEN
            "BETWEEN",  // CASE
            "CASE",  // CAST
            "CAST",  // CHECK
            "CHECK",  // CONSTRAINT
            "CONSTRAINT",  // CROSS
            "CROSS",  // CURRENT_CATALOG
            "CURRENT_CATALOG",  // CURRENT_DATE
            "CURRENT_DATE",  // CURRENT_PATH
            "CURRENT_PATH",  // CURRENT_ROLE
            "CURRENT_ROLE",  // CURRENT_SCHEMA
            "CURRENT_SCHEMA",  // CURRENT_TIME
            "CURRENT_TIME",  // CURRENT_TIMESTAMP
            "CURRENT_TIMESTAMP",  // CURRENT_USER
            "CURRENT_USER",  // DAY
            "DAY",  // DEFAULT
            "DEFAULT",  // DISTINCT
            "DISTINCT",  // ELSE
            "ELSE",  // END
            "END",  // EXCEPT
            "EXCEPT",  // EXISTS
            "EXISTS",  // FALSE
            "FALSE",  // FETCH
            "FETCH",  // FOR
            "FOR",  // FOREIGN
            "FOREIGN",  // FROM
            "FROM",  // FULL
            "FULL",  // GROUP
            "GROUP",  // HAVING
            "HAVING",  // HOUR
            "HOUR",  // IF
            "IF",  // IN
            "IN",  // INNER
            "INNER",  // INTERSECT
            "INTERSECT",  // INTERSECTS
            "INTERSECTS",  // INTERVAL
            "INTERVAL",  // IS
            "IS",  // JOIN
            "JOIN",  // KEY
            "KEY",  // LEFT
            "LEFT",  // LIKE
            "LIKE",  // LIMIT
            "LIMIT",  // LOCALTIME
            "LOCALTIME",  // LOCALTIMESTAMP
            "LOCALTIMESTAMP",  // MINUS
            "MINUS",  // MINUTE
            "MINUTE",  // MONTH
            "MONTH",  // NATURAL
            "NATURAL",  // NOT
            "NOT",  // NULL
            "NULL",  // OFFSET
            "OFFSET",  // ON
            "ON",  // OR
            "OR",  // ORDER
            "ORDER",  // PRIMARY
            "PRIMARY",  // QUALIFY
            "QUALIFY",  // RIGHT
            "RIGHT",  // ROW
            "ROW",  // ROWNUM
            "ROWNUM",  // SECOND
            "SECOND",  // SELECT
            "SELECT",  // SESSION_USER
            "SESSION_USER",  // SET
            "SET",  // SOME
            "SOME",  // SYMMETRIC
            "SYMMETRIC",  // SYSTEM_USER
            "SYSTEM_USER",  // TABLE
            "TABLE",  // TO
            "TO",  // TRUE
            "TRUE",  // UESCAPE
            "UESCAPE",  // UNION
            "UNION",  // UNIQUE
            "UNIQUE",  // UNKNOWN
            "UNKNOWN",  // USER
            "USER",  // USING
            "USING",  // VALUE
            "VALUE",  // VALUES
            "VALUES",  // WHEN
            "WHEN",  // WHERE
            "WHERE",  // WINDOW
            "WINDOW",  // WITH
            "WITH",  // YEAR
            "YEAR",  // _ROWID_
            "_ROWID_",  // PARAMETER
            "?",  // END
            null,  // VALUE
            null,  // EQUAL
            "=",  // BIGGER_EQUAL
            ">=",  // BIGGER
            ">",  // SMALLER
            "<",  // SMALLER_EQUAL
            "<=",  // NOT_EQUAL
            "<>",  // AT
            "@",  // MINUS_SIGN
            "-",  // PLUS_SIGN
            "+",  // STRING_CONCAT
            "||",  // OPEN_PAREN
            "(",  // CLOSE_PAREN
            ")",  // SPATIAL_INTERSECTS
            "&&",  // ASTERISK
            "*",  // COMMA
            ",",  // DOT
            ".",  // OPEN_BRACE
            "{",  // CLOSE_BRACE
            "}",  // SLASH
            "/",  // PERCENT
            "%",  // SEMICOLON
            ";",  // COLON
            ":",  // OPEN_BRACKET
            "[",  // CLOSE_BRACKET
            "]",  // TILDE
            "~",  // COLON_COLON
            "::",  // COLON_EQ
            ":=",  // NOT_TILDE
            "!~")
    }
}