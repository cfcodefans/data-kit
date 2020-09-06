package org.h2.bnf

import org.h2.util.StringUtils

/**
 * A single terminal rule in a BNF object.
 */
class RuleElement(val name: String, topic: String) : Rule {

    val keyword: Boolean = name.length == 1 || name == StringUtils.toUpperEnglish(name)
    val type: Int = if (StringUtils.toLowerEnglish(topic).startsWith("function"))
        Sentence.FUNCTION
    else
        Sentence.KEYWORD

    var link: Rule? = null

    override fun accept(visitor: BnfVisitor) {
        visitor.visitRuleElement(keyword, name, link)
    }

    override fun setLinks(ruleMap: HashMap<String, RuleHead>) {
        if (link != null) link!!.setLinks(ruleMap)
        if (keyword) return


    }
}