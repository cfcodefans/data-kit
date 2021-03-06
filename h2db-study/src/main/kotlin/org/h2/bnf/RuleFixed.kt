package org.h2.bnf

import java.util.HashMap

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
        TODO("Not yet implemented")
    }
}