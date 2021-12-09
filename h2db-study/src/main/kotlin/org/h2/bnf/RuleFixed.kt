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

    override fun setLinks(ruleMap: HashMap<String, RuleHead>) {
        // nothing to do
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
                while (true) {
                    s = s.trimStart { it == '\'' }
                    s = if (s.startsWith("''")) s.substring(2) else break
                }
                if (s.isEmpty()) {
                    sentence.add("anything", "Hello World", Sentence.KEYWORD)
                    sentence.add("'", "'", Sentence.KEYWORD)
                }
            }
            ANY_EXCEPT_2_DOLLAR -> {
                while (s.isNotEmpty() && !s.startsWith("$$")) {
                    s = s.substring(1)
                }
                if (s.isEmpty()) {
                    sentence.add("anything", "Hello World", Sentence.KEYWORD)
                    sentence.add("$$", "$$", Sentence.KEYWORD)
                }
            }
            ANY_EXCEPT_DOUBLE_QUOTE -> {
                while (true) {
                    s = s.trimStart { it == '\"' }
                    s = (if (s.startsWith("\"\"")) s.substring(2) else break)
                }
                if (s.isEmpty()) {
                    sentence.add("anything", "identifier", Sentence.KEYWORD)
                    sentence.add("\"", "\"", Sentence.KEYWORD)
                }
            }
            ANY_WORD, JSON_TEXT -> {
                s = s.trimStart()
                if (s.length == 0) {
                    sentence.add("anything", "anything", Sentence.KEYWORD)
                }
            }
            HEX_START -> {
                if (s.startsWith("0X") || s.startsWith("0x")) {
                    s = s.substring(2)
                } else if ("0" == s) {
                    sentence.add("0x", "x", Sentence.KEYWORD)
                } else if (s.isEmpty()) {
                    sentence.add("0x", "0x", Sentence.KEYWORD)
                }
            }
            CONCAT -> {
                if (s == "|") {
                    sentence.add("||", "|", Sentence.KEYWORD)
                } else if (s.startsWith("||")) {
                    s = s.substring(2)
                } else if (s.isEmpty()) {
                    sentence.add("||", "||", Sentence.KEYWORD)
                }
                removeTrailingSpaces = true
            }
            AZ_UNDERSCORE -> {
                if (s.isNotEmpty() && (Character.isLetter(s[0]) || s[0] == '_')) {
                    s = s.substring(1)
                }
                if (s.isEmpty()) {
                    sentence.add("character", "A", Sentence.KEYWORD)
                }
            }
            AF -> {
                if (s.isNotEmpty() && s[0].uppercaseChar() in 'A'..'F') {
                    s = s.substring(1)
                }
                if (s.isEmpty()) {
                    sentence.add("hex character", "0A", Sentence.KEYWORD)
                }
            }
            DIGIT -> {
                if (s.isNotEmpty() && Character.isDigit(s[0])) {
                    s = s.substring(1)
                }
                if (s.isEmpty()) {
                    sentence.add("digit", "1", Sentence.KEYWORD)
                }
            }
            OPEN_BRACKET -> {
                if (s.isEmpty()) {
                    sentence.add("[", "[", Sentence.KEYWORD)
                } else if (s[0] == '[') {
                    s = s.substring(1)
                }
                removeTrailingSpaces = true
            }
            CLOSE_BRACKET -> {
                if (s.isEmpty()) {
                    sentence.add("]", "]", Sentence.KEYWORD)
                } else if (s[0] == ']') {
                    s = s.substring(1)
                }
                removeTrailingSpaces = true
            }
            // no autocomplete support for comments
            // (comments are not reachable in the bnf tree)
//            ANY_UNTIL_EOL, ANY_UNTIL_END
            else -> throw AssertionError("type=$type")
        }

        if (s != sentence.query) {
            // can not always remove spaces here, because a repeat
            // rule for a-z would remove multiple words
            // but we have to remove spaces after '||'
            // and after ']'
            if (removeTrailingSpaces) {
                s = s.trimStart()
            }
            sentence.query = s
            return true
        }
        return false
    }
}