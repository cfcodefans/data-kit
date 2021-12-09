package org.h2.bnf

/**
 * Represents a loop in a BNF object.
 */
open class RuleRepeat(val rule: Rule, val comma: Boolean = false) : Rule {

    override fun accept(visitor: BnfVisitor) {
        visitor.visitRuleRepeat(comma, rule)
    }

    override fun setLinks(ruleMap: HashMap<String, RuleHead>) {}

    override fun autoComplete(sentence: Sentence): Boolean {
        sentence.stopIfRequired()
        while (rule.autoComplete(sentence)) {
        }

        sentence.query = sentence.query.trimStart()
        return true
    }

    override fun toString(): String {
        return if (comma) ", ..." else " ..."
    }
}