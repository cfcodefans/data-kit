package org.h2.bnf

/**
 * represents a non-standard syntax.
 */
open class RuleExtension(val rule: Rule, val compatibility: Boolean) : Rule {

    private var mapSet: Boolean = false

    override fun accept(visitor: BnfVisitor) {
        visitor.visitRuleExtension(rule, compatibility)
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

    override fun toString(): String {
        return (if (compatibility) "@c@ " else "@h2@ ") + rule.toString()
    }
}