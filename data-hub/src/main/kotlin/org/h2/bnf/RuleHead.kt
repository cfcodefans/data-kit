package org.h2.bnf

/**
 * Represents the head of a BNF rule.
 */
data class RuleHead(val section: String,
                    val topic: String,
                    val rule: Rule) {
}