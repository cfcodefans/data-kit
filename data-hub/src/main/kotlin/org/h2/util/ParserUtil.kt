package org.h2.util

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
    const val JSON: Int = IS + 1

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
        for (i in 1..len) {
            val c: Char = s[i]
            if (partFlags ushr Character.getType(c) and 1 == 0 && c != '_') {
                return false
            }
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
            'A' -> {
                if (eq("ALL", s, ignoreCase, start, end)) return ALL
            }
        }
    }

    @JvmStatic
    private fun eq(expected: String, s: String, ignoreCase: Boolean, start: Int, end: Int): Boolean {
        val len: Int = expected.length
        //First letter was already checked
        return end - start == len
                && expected.regionMatches(1, s, start + 1, len, ignoreCase)
    }
}