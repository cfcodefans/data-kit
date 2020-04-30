package org.h2.bnf

/**
 * Represents a BNF rule.
 */
interface Rule {
    /**
     * Update cross references.
     * @param ruleMap the reference map
     */
    fun setLinks(ruleMap: HashMap<String, RuleHead>)

    /**
     * Add the next possible token(s). if there was a match, the query in the sentence is updated (the matched token is removed).
     */
    fun autoComplete(sentence: Sentence): Boolean

    /**
     * Call the visit method in the given visitor.
     * @param visitor the visitor
     */
    fun accept(visitor: BnfVisitor)
}