package org.h2.bnf

import org.h2.util.StringUtils

/**
 * A single terminal rule in a BNF object.
 */
class RuleElement(val name: String, topic: String) : Rule {

    private val keyword: Boolean = name.length == 1 || name == StringUtils.toUpperEnglish(name)
    val type: Int = if (StringUtils.toLowerEnglish(topic).startsWith("function"))
        Sentence.FUNCTION
    else
        Sentence.KEYWORD

    var link: Rule? = null

    override fun accept(visitor: BnfVisitor) {
        visitor.visitRuleElement(keyword, name, link)
    }

    override fun setLinks(ruleMap: HashMap<String, RuleHead>): Unit {
        if (link != null) link!!.setLinks(ruleMap)
        if (keyword) return

        val test: String = Bnf.getRuleMapKey(name)
        for (i in test.indices) {
            ruleMap[test.substring(i)]?.let {
                link = it.rule
                return
            }
        }
        throw AssertionError("Unknown $name/$test")
    }

    override fun autoComplete(sentence: Sentence): Boolean {
        sentence.stopIfRequired()
        if (!keyword) return link!!.autoComplete(sentence)

        var query: String = sentence.query
        var q: String = query.trim()
        var up: String = sentence.queryUpper.trim()

        if (up.startsWith(name)) {
            query = query.substring(name.length)
            while ("_" != name && Bnf.startWithSpace(query)) {
                query = query.substring(1)
            }
            sentence.query = query
            return true
        }

        if (q.isEmpty() || name.startsWith(up)) {
            if (q.length < name.length) {
                sentence.add(name, name.substring(q.length), type)
            }
        }
        return false
    }
}