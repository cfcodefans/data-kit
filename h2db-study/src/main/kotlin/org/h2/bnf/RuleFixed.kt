package org.h2.bnf

open class RuleFixed(val type: Int) : Rule {
    companion object {
        const val YMD = 0
        const val HMS = 1
        const val NANOS = 2
        const val ANY_EXCEPT_SINGLE_QUOTE = 3
        const val ANY_EXCEPT_DOUBLE_QUOTE = 4
        const val ANY_UNTIL_EOL = 5
        const val ANY_UNTIL_END = 6
        const val ANY_WORD = 7
        const val ANY_EXCEPT_2_DOLLAR = 8
        const val HEX_START = 10
        const val CONCAT = 11
        const val AZ_UNDERSCORE = 12
        const val AF = 13
        const val DIGIT = 14
        const val OPEN_BRACKET = 15
        const val CLOSE_BRACKET = 16
        const val JSON_TEXT = 17
    }

    override fun accept(visitor: BnfVisitor) {
        visitor.visitRuleFixed(type)
    }

    override fun autoComplete(sentence: Sentence): Boolean {
        sentence.stopIfRequired()
        var s: String = sentence.query
        var removeTrailingSpaces: Boolean = false

        when (type) {
            YMD -> {
                s = s.trimStart { "0123456789-".contains(it) }
                if (s.isEmpty()) sentence.add("2006-01-01", "1", Sentence.KEYWORD)
                // needed for timestamps
                removeTrailingSpaces = true
            }
            HMS -> {
                s = s.trimStart { "0123456789:".contains(it) }
                if (s.isEmpty()) sentence.add("12:00:00", "1", Sentence.KEYWORD)
            }
            NANOS -> {
                s = s.trimStart { it.isDigit() }
                if (s.isEmpty()) sentence.add("nanoseconds", "0", Sentence.KEYWORD)
                removeTrailingSpaces = true
            }
            ANY_EXCEPT_SINGLE_QUOTE -> {

            }
            else -> throw AssertionError("type=" + type)
        }

        return false
    }
}