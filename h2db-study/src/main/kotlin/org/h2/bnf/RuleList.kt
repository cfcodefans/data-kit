package org.h2.bnf

import org.h2.util.Utils

/**
 * Represents a sequence of BNF rules, or a list of alternative rules.
 */
open class RuleList(val or: Boolean, first: Rule, next: Rule) : Rule {
    val list: ArrayList<Rule> = Utils.newSmallArrayList()
    private var mapSet = false

    init {
        if (first is RuleList && first.or == or)
            list.addAll(first.list)
        else
            list.add(first)

        if (next is RuleList && next.or == or)
            list.addAll(next.list)
        else
            list.add(next)
    }

    override fun accept(visitor: BnfVisitor) {
        visitor.visitRuleList(or, list)
    }

    override fun setLinks(ruleMap: HashMap<String, RuleHead>) {
        if (mapSet) return
        for (r in list) {
            r.setLinks(ruleMap)
        }
        mapSet = true
    }

    override fun autoComplete(sentence: Sentence): Boolean {
        sentence.stopIfRequired()
        val old: String = sentence.query
        if (or) {
            for (r in list) {
                sentence.query = old
                if (r.autoComplete(sentence)) return true
            }
            return false
        }

        for (r in list) {
            if (!r.autoComplete(sentence)) {
                sentence.query = old
                return false
            }
        }
        return true
    }

    override fun toString(): String = list.joinToString((if (or) " | " else " "))
}