package org.h2.util

import org.h2.util.StringUtils.quoteIdentifier

object ParserUtil {
    /**
     * A keyword.
     */
    const val KEYWORD: Int = 1

    /**
     * An identifier (table name, column name,...).
     */
    const val IDENTIFIER: Int = 2

    /**
     * The token "ALL".
     */
    const val ALL: Int = IDENTIFIER + 1

    /**
     * The token "ARRAY"
     */
    const val ARRAY: Int = ALL + 1

    /**
     * The token "CASE"
     */
    const val CASE: Int = ARRAY + 1

    /**
     * The token "CHECK"
     */
    const val CHECK: Int = CASE + 1

    /**
     * The token "CONSTRAINT"
     */
    const val CONSTRAINT: Int = CHECK + 1

    /**
     * The token "CROSS"
     */
    const val CROSS: Int = CONSTRAINT + 1

    /**
     * The token "CURRENT_DATE".
     */
    const val CURRENT_DATE: Int = CROSS + 1

    /**
     * The token "CURRENT_SCHEMA"
     */
    const val CURRENT_SCHEMA: Int = CURRENT_DATE + 1

    /**
     * The token "CURRENT_TIME"
     */
    const val CURRENT_TIME: Int = CURRENT_SCHEMA + 1

    /**
     * The token "CURRENT_TIMESTAMP"
     */
    const val CURRENT_TIMESTAMP: Int = CURRENT_TIME + 1

    /**
     * The token "CURRENT_USER"
     */
    const val CURRENT_USER: Int = CURRENT_TIMESTAMP + 1

    /**
     * The token "DISTINCT"
     */
    const val DISTINCT: Int = CURRENT_USER + 1

    /**
     * The token "EXCEPT"
     */
    const val EXCEPT: Int = DISTINCT + 1

    /**
     * The token "EXISTS"
     */
    const val EXISTS: Int = EXCEPT + 1

    /**
     * The token "FALSE"
     */
    const val FALSE: Int = EXISTS + 1

    /**
     * The token "FETCH"
     */
    const val FETCH: Int = FALSE + 1

    /**
     * The token "FOR"
     */
    const val FOR: Int = FETCH + 1

    /**
     * The token "FOREIGN"
     */
    const val FOREIGN: Int = FOR + 1

    /**
     * The token "FROM"
     */
    const val FROM: Int = FOREIGN + 1

    /**
     * The token "FULL"
     */
    const val FULL: Int = FROM + 1

    /**
     * The token "GROUP"
     */
    const val GROUP: Int = FULL + 1

    /**
     * The token "HAVING"
     */
    const val HAVING: Int = GROUP + 1

    /**
     * The token "IF"
     */
    const val IF: Int = HAVING + 1

    /**
     * The token "INNER"
     */
    const val INNER: Int = IF + 1

    /**
     * The token "INTERSECT"
     */
    const val INTERSECT: Int = INNER + 1

    /**
     * The token "INTERSECTS"
     */
    const val INTERSECTS: Int = INTERSECT + 1

    /**
     * The token "INTERVAL"
     */
    const val INTERVAL: Int = INTERSECTS + 1

    /**
     * The token "IS"
     */
    const val IS: Int = INTERVAL + 1

    /**
     * The token "JOIN"
     */
    const val JOIN: Int = IS + 1

    /**
     * The token "LEFT"
     */
    const val LEFT: Int = JOIN + 1

    /**
     * The token "LIKE"
     */
    const val LIKE: Int = LEFT + 1

    /**
     * The token "LIMIT"
     */
    const val LIMIT: Int = LIKE + 1

    /**
     * The token "LOCALTIME"
     */
    const val LOCALTIME: Int = LIMIT + 1

    /**
     * The token "LOCALTIMESTAMP"
     */
    const val LOCALTIMESTAMP: Int = LOCALTIME + 1

    /**
     * The token "MINUS"
     */
    const val MINUS: Int = LOCALTIMESTAMP + 1

    /**
     * The token "NATURAL".
     */
    const val NATURAL: Int = MINUS + 1

    /**
     * The token "NOT"
     */
    const val NOT: Int = NATURAL + 1

    /**
     * The token "NULL"
     */
    const val NULL: Int = NOT + 1

    /**
     * The token "OFFSET"
     */
    const val OFFSET: Int = NULL + 1

    /**
     * The token "ON"
     */
    const val ON: Int = OFFSET + 1

    /**
     * The token "ORDER"
     */
    const val ORDER: Int = ON + 1

    /**
     * The token "PRIMARY"
     */
    const val PRIMARY: Int = ORDER + 1

    /**
     * The token "QUALIFY"
     */
    const val QUALIFY: Int = PRIMARY + 1

    /**
     * The token "RIGHT"
     */
    const val RIGHT: Int = QUALIFY + 1

    /**
     * The token "ROW"
     */
    const val ROW: Int = RIGHT + 1

    /**
     * The token "_ROWID_"
     */
    const val _ROWID_: Int = ROW + 1

    /**
     * The tken "ROWNUM"
     */
    const val ROWNUM: Int = _ROWID_ + 1

    /**
     * The token "SELECT"
     */
    const val SELECT: Int = ROWNUM + 1

    /**
     * The token "TABLE".
     */
    const val TABLE: Int = SELECT + 1

    /**
     * The token "TRUE"
     */
    const val TRUE: Int = TABLE + 1

    /**
     * The token "UNION"
     */
    const val UNION: Int = TRUE + 1

    /**
     * The token "UNIQUE"
     */
    const val UNIQUE: Int = UNION + 1

    /**
     * The token "UNKNOWN"
     */
    const val UNKNOWN: Int = UNIQUE + 1

    /**
     * The token "USING"
     */
    const val USING: Int = UNKNOWN + 1

    /**
     * The token "VALUES"
     */
    const val VALUES: Int = USING + 1

    /**
     * The token "WHERE"
     */
    const val WHERE: Int = VALUES + 1

    /**
     * The token "WINDOW".
     */
    const val WINDOW: Int = WHERE + 1

    /**
     * The token "WITH"
     */
    const val WITH: Int = WINDOW + 1

    private const val UPPER_OR_OTHER_LETTER = 1 shl Character.UPPERCASE_LETTER.toInt() or
            (1 shl Character.MODIFIER_LETTER.toInt()) or
            (1 shl Character.OTHER_LETTER.toInt())

    private const val UPPER_OR_OTHER_LETTER_OR_DIGIT = (UPPER_OR_OTHER_LETTER
            or 1 shl Character.DECIMAL_DIGIT_NUMBER.toInt())

    private const val LOWER_OR_OTHER_LETTER = 1 shl Character.LOWERCASE_LETTER.toInt() or
            (1 shl Character.MODIFIER_LETTER.toInt()) or
            (1 shl Character.OTHER_LETTER.toInt())

    private const val LOWER_OR_OTHER_LETTER_OR_DIGIT = (LOWER_OR_OTHER_LETTER
            or 1 shl Character.DECIMAL_DIGIT_NUMBER.toInt())

    private const val LETTER = 1 shl Character.UPPERCASE_LETTER.toInt() or
            (1 shl Character.LOWERCASE_LETTER.toInt()) or
            (1 shl Character.TITLECASE_LETTER.toInt()) or
            (1 shl Character.MODIFIER_LETTER.toInt()) or
            (1 shl Character.OTHER_LETTER.toInt())

    private const val LETTER_OR_DIGIT = (LETTER or 1 shl Character.DECIMAL_DIGIT_NUMBER.toInt())

    /**
     * Is this a simple identifier (in the JDBC specification sense).
     *
     */
    @JvmStatic
    fun isSimpleIdentifier(s: String, databaseToUpper: Boolean, databaseToLower: Boolean): Boolean {
        if (s.isNullOrBlank()) return false

        var startFlags: Int = 0
        var partFlags: Int = 0

        if (databaseToUpper) {
            if (databaseToLower) {
                throw IllegalArgumentException("databaseToUpper && databaseToLower")
            }
            startFlags = UPPER_OR_OTHER_LETTER
            partFlags = UPPER_OR_OTHER_LETTER_OR_DIGIT
        } else {
            if (databaseToLower) {
                startFlags = LOWER_OR_OTHER_LETTER
                partFlags = LOWER_OR_OTHER_LETTER_OR_DIGIT
            } else {
                startFlags = LETTER
                partFlags = LETTER_OR_DIGIT
            }
        }
        val c: Char = s[0]
        if (startFlags ushr Character.getType(c) and 1 == 0 && c != '_') {
            return false
        }
        val len: Int = s.length
        for (c in s) {
            if (partFlags ushr Character.getType(c) and 1 == 0 && c != '_') return false
        }
        return getSaveTokenType(s, !databaseToUpper, 0, len, true) == IDENTIFIER
    }

    /**
     * Get the token type.
     *
     * @param s the string with token
     * @param ignoreCase true if case should be ignored, false if only upper case tokens are detected as keywords
     * @param start start index of token
     * @param end index of token, exclusive; must be greater than the start index
     * @param additionalKeywords whether TOP, INTERSECTS, and "current data / time" functions are keywords
     * @return the token type
     */
    @JvmStatic
    fun getSaveTokenType(s: String, ignoreCase: Boolean, start: Int, end: Int, additionalKeywords: Boolean): Int {
        /**
         * JdbcDatabaseMeteData.getSQLKeywords() and tests should be updated when new
         * non-SQL:2003 keywords are introduced here
         */
        var c: Char = s[start]
        if (ignoreCase) {
            // Convert a-z to A-Z and 0x7f to _ (need special handling).
            c = (c.toInt() and 0xffdf).toChar()
        }
        when (c) {
            'A' -> return when {
                eq("ALL", s, ignoreCase, start, end) -> ALL
                eq("ARRAY", s, ignoreCase, start, end) -> ARRAY
                additionalKeywords
                        && (eq("ARRAY", s, ignoreCase, start, end)
                        || eq("AS", s, ignoreCase, start, end)) -> KEYWORD
                else -> IDENTIFIER
            }
            'B' -> return when {
                additionalKeywords
                        && (eq("BETWEEN", s, ignoreCase, start, end)
                        || eq("BOTH", s, ignoreCase, start, end)) -> KEYWORD
                else -> IDENTIFIER
            }
            'C' -> return when {
                eq("CASE", s, ignoreCase, start, end) -> CASE
                eq("CHECK", s, ignoreCase, start, end) -> CHECK
                eq("CONSTRAINT", s, ignoreCase, start, end) -> CONSTRAINT
                eq("CROSS", s, ignoreCase, start, end) -> CROSS
                eq("CURRENT_DATE", s, ignoreCase, start, end) -> CURRENT_DATE
                eq("CURRENT_SCHEMA", s, ignoreCase, start, end) -> CURRENT_SCHEMA
                eq("CURRENT_TIME", s, ignoreCase, start, end) -> CURRENT_TIME
                eq("CURRENT_TIMESTAMP", s, ignoreCase, start, end) -> CURRENT_TIMESTAMP
                eq("CURRENT_USER", s, ignoreCase, start, end) -> CURRENT_USER
                else -> IDENTIFIER
            }
            'D' -> return when {
                eq("DISTINCT", s, ignoreCase, start, end) -> DISTINCT
                else -> IDENTIFIER
            }
            'E' -> return when {
                eq("EXCEPT", s, ignoreCase, start, end) -> EXCEPT
                eq("EXISTS", s, ignoreCase, start, end) -> EXISTS
                else -> IDENTIFIER
            }
            'F' -> return when {
                eq("FETCH", s, ignoreCase, start, end) -> FETCH
                eq("FROM", s, ignoreCase, start, end) -> FROM
                eq("FOR", s, ignoreCase, start, end) -> FOR
                eq("FOREIGN", s, ignoreCase, start, end) -> FOREIGN
                eq("FULL", s, ignoreCase, start, end) -> FULL
                eq("FALSE", s, ignoreCase, start, end) -> FALSE
                additionalKeywords && eq("FILTER", s, ignoreCase, start, end) -> KEYWORD
                else -> IDENTIFIER
            }
            'G' -> return when {
                eq("GROUP", s, ignoreCase, start, end) -> GROUP
                additionalKeywords && eq("GROUPS", s, ignoreCase, start, end) -> KEYWORD
                else -> IDENTIFIER
            }
            'H' -> return when {
                eq("HAVING", s, ignoreCase, start, end) -> HAVING
                else -> IDENTIFIER
            }
            'I' -> return when {
                eq("IF", s, ignoreCase, start, end) -> IF
                eq("INNER", s, ignoreCase, start, end) -> INNER
                eq("INTERSECT", s, ignoreCase, start, end) -> INTERSECT
                eq("INTERSECTS", s, ignoreCase, start, end) -> INTERSECTS
                eq("INTERVAL", s, ignoreCase, start, end) -> INTERVAL
                eq("IS", s, ignoreCase, start, end) -> IS
                additionalKeywords
                        && (eq("ILIKE", s, ignoreCase, start, end)
                        || eq("IN", s, ignoreCase, start, end)) -> KEYWORD
                else -> IDENTIFIER
            }
            'J' -> return when {
                eq("JOIN", s, ignoreCase, start, end) -> JOIN
                else -> IDENTIFIER
            }
            'L' -> return when {
                eq("LEFT", s, ignoreCase, start, end) -> LEFT
                eq("LIMIT", s, ignoreCase, start, end) -> LIMIT
                eq("LIKE", s, ignoreCase, start, end) -> LIKE
                eq("LOCALTIME", s, ignoreCase, start, end) -> LOCALTIME
                eq("LOCALTIMESTAMP", s, ignoreCase, start, end) -> LOCALTIMESTAMP
                additionalKeywords && eq("LEADING", s, ignoreCase, start, end) -> KEYWORD
                else -> IDENTIFIER
            }
            'M' -> return when {
                eq("MINUS", s, ignoreCase, start, end) -> HAVING
                else -> IDENTIFIER
            }
            'N' -> return when {
                eq("NOT", s, ignoreCase, start, end) -> NOT
                eq("NATURAL", s, ignoreCase, start, end) -> NATURAL
                eq("NULL", s, ignoreCase, start, end) -> NULL
                else -> IDENTIFIER
            }
            'O' -> return when {
                eq("OFFSET", s, ignoreCase, start, end) -> OFFSET
                eq("ON", s, ignoreCase, start, end) -> ON
                eq("ORDER", s, ignoreCase, start, end) -> ORDER
                additionalKeywords
                        && (eq("OR", s, ignoreCase, start, end)
                        || eq("OVER", s, ignoreCase, start, end)) -> KEYWORD
                else -> IDENTIFIER
            }
            'P' -> return when {
                eq("PRIMARY", s, ignoreCase, start, end) -> PRIMARY
                additionalKeywords && eq("PARTITION", s, ignoreCase, start, end) -> KEYWORD
                else -> IDENTIFIER
            }
            'Q' -> return when {
                eq("QUALIFY", s, ignoreCase, start, end) -> QUALIFY
                else -> IDENTIFIER
            }
            'R' -> return when {
                eq("RIGHT", s, ignoreCase, start, end) -> RIGHT
                eq("ROW", s, ignoreCase, start, end) -> ROW
                eq("ROWNUM", s, ignoreCase, start, end) -> ROWNUM
                additionalKeywords
                        && (eq("RANGE", s, ignoreCase, start, end)
                        || eq("REGEXP", s, ignoreCase, start, end)
                        || eq("ROWS", s, ignoreCase, start, end)) -> KEYWORD
                else -> IDENTIFIER
            }
            'S' -> return when {
                eq("SELECT", s, ignoreCase, start, end) -> SELECT
                additionalKeywords
                        && (eq("SYSDATE", s, ignoreCase, start, end)
                        || eq("SYSTIME", s, ignoreCase, start, end)
                        || eq("SYSTIMESTAMP", s, ignoreCase, start, end)) -> KEYWORD
                else -> IDENTIFIER
            }
            'T' -> return when {
                eq("TABLE", s, ignoreCase, start, end) -> TABLE
                eq("TRUE", s, ignoreCase, start, end) -> TRUE
                additionalKeywords
                        && (eq("TODAY", s, ignoreCase, start, end)
                        || eq("TOP", s, ignoreCase, start, end)
                        || eq("TRAILING", s, ignoreCase, start, end)) -> KEYWORD
                else -> IDENTIFIER
            }
            'U' -> return when {
                eq("UNION", s, ignoreCase, start, end) -> UNION
                eq("UNIQUE", s, ignoreCase, start, end) -> UNIQUE
                eq("UNKNOWN", s, ignoreCase, start, end) -> UNKNOWN
                eq("USING", s, ignoreCase, start, end) -> USING
                else -> IDENTIFIER
            }
            'V' -> return when {
                eq("VALUES", s, ignoreCase, start, end) -> VALUES
                else -> IDENTIFIER
            }
            'W' -> return when {
                eq("WHERE", s, ignoreCase, start, end) -> WHERE
                eq("WINDOW", s, ignoreCase, start, end) -> WINDOW
                eq("WITH", s, ignoreCase, start, end) -> WITH
                else -> IDENTIFIER
            }
            '_' -> return when {// Cannot use eq() because 0x7f can be converted to '_' (0x5f)
                end - start == 7
                        && "_ROWID_".regionMatches(0, s, start, 7, ignoreCase = ignoreCase) -> {
                    _ROWID_
                }
                else -> IDENTIFIER
            }
            else -> return IDENTIFIER
        }
    }

    @JvmStatic
    private fun eq(expected: String, s: String, ignoreCase: Boolean, start: Int, end: Int): Boolean {
        val len: Int = expected.length
        //First letter was already checked
        return end - start == len
                && expected.regionMatches(1, s, start + 1, len, ignoreCase)
    }

    /**
     * Add double quotes around an identifier if required and appends it to the
     * specified string builder.
     *
     * @param builder string builder to append to
     * @param s the identifier
     * @param sqlFlags formatting flags
     * @return the specified builder
     */
    fun quoteIdentifier(builder: StringBuilder, s: String?, sqlFlags: Int): StringBuilder {
        if (s == null) return builder.append("\"\"")

        return if (sqlFlags and HasSQL.QUOTE_ONLY_WHEN_REQUIRED != 0
                && isSimpleIdentifier(s, false, false))
            builder.append(s)
        else quoteIdentifier(builder, s)
    }

    /**
     * Check if this string is a SQL keyword.
     * @param s the token to check
     * @param ignoreCase true if case should be ignored, false if only upper case tokens are detected as keywords
     * @return true if it is a keyword
     */
    @JvmStatic
    fun isKeyword(s: String, ignoreCase: Boolean): Boolean = if (s.isEmpty())
        false
    else
        getSaveTokenType(s, ignoreCase, 0, s.length, false) != IDENTIFIER
}