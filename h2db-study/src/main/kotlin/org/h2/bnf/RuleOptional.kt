package org.h2.bnf

/**
 * Represents an optional BNF rule.
 */
open class RuleOptional(val rule: Rule) : Rule {

    private var mapSet: Boolean = false

    override fun accept(visitor: BnfVisitor) {
        if (rule is RuleList) {
            val ruleList = rule
            if (ruleList.or) {
                visitor.visitRuleOptional(ruleList.list)
                return
            }
        }
        visitor.visitRuleOptional(rule)
    }

    override fun setLinks(ruleMap: HashMap<String, RuleHead>) {
        if (mapSet) return
        rule.setLinks(ruleMap)
        mapSet = true
    }

    override fun autoComplete(sentence: Sentence): Boolean {
        sentence.stopIfRequired()
        rule.autoComplete(sentence)
        return true
    }

    override fun toString(): String = "[$rule]"
}