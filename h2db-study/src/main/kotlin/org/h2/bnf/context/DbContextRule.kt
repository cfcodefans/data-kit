package org.h2.bnf.context

import org.h2.bnf.BnfVisitor
import org.h2.bnf.Rule
import org.h2.bnf.RuleHead
import org.h2.bnf.Sentence
import org.h2.util.ParserUtil
import org.h2.util.StringUtils
import java.util.*

/**
 * A BNF terminal rule that is linked to the database context information.
 * This class is used by the H2 Console, to support auto-complete.
 */
/**
 * BNF terminal rule Constructor
 * @param contents Extract rule from this component
 * @param type Rule type, one of
 * {@link DbContextRule#COLUMN},
 * {@link DbContextRule#TABLE},
 * {@link DbContextRule#TABLE_ALIAS},
 * {@link DbContextRule#NEW_TABLE_ALIAS},
 * {@link DbContextRule#COLUMN_ALIAS},
 * {@link DbContextRule#SCHEMA}
 */
class DbContextRule(@JvmField val contents: DbContents,
                    @JvmField val type: Int) : Rule {
    companion object {
        const val COLUMN: Int = 0
        const val TABLE: Int = 1
        const val TABLE_ALIAS: Int = 2
        const val NEW_TABLE_ALIAS: Int = 3
        const val COLUMN_ALIAS: Int = 4
        const val SCHEMA: Int = 5
        const val PROCEDURE: Int = 6

        private fun beginOfName(up: String): Int {
            var i = 0
            while (i < up.length) {
                val ch = up[i]
                if (ch != '_' && !Character.isLetterOrDigit(ch)) break
                i++
            }
            return i
        }
    }

    lateinit var columnType: String

    override fun setLinks(ruleMap: HashMap<String, RuleHead>) {}
    override fun accept(visitor: BnfVisitor) {}

    override fun autoComplete(sentence: Sentence): Boolean {
        val query: String = sentence.query
        var s: String = query
        val up: String = sentence.queryUpper
        when (type) {
            SCHEMA -> {
                val schemas: Array<DbSchema> = contents.schemas ?: emptyArray()
                var best: String? = null
                var bestSchema: DbSchema? = null
                for (schema in schemas) {
                    val name: String = StringUtils.toUpperEnglish(schema.name!!)
                    if (up.startsWith(name)) {
                        if (best == null || name.length > best.length) {
                            best = name
                            bestSchema = schema
                        }
                    } else if (s.isEmpty() || name.startsWith(up)) {
                        if (s.length < name.length) {
                            sentence.add(name, name.substring(s.length), type)
                            sentence.add(schema.quotedName + ".",
                                    schema.quotedName?.substring(s.length) + ".",
                                    Sentence.CONTEXT)
                        }
                    }
                }
                if (best != null) {
                    sentence.lastMatchedSchema = bestSchema
                    s = s.substring(best.length)
                }
            }

            TABLE -> {
                val schema: DbSchema = sentence.lastMatchedSchema ?: contents.defaultSchema!!
                val tables: Array<DbTableOrView> = schema.tables
                var best: String? = null
                var bestTable: DbTableOrView? = null

                for (table in tables) {
                    var compare = up
                    var name = StringUtils.toUpperEnglish(table.name)
                    if (table.quotedName!!.length > name.length) {
                        name = table.quotedName
                        compare = query
                    }
                    if (compare.startsWith(name)) {
                        if (best == null || name.length > best.length) {
                            best = name
                            bestTable = table
                        }
                    } else if (s.isEmpty() || name.startsWith(compare)) {
                        if (s.length < name.length) {
                            sentence.add(table.quotedName, table.quotedName.substring(s.length), Sentence.CONTEXT)
                        }
                    }
                }
                if (best != null) {
                    sentence.lastMatchedTable = bestTable!!
                    sentence.addTable(bestTable!!)
                    s = s.substring(best.length)
                }
            }
            NEW_TABLE_ALIAS -> {
                s = autoCompleteTableAlias(sentence, true)
            }
            TABLE_ALIAS -> {
                s = autoCompleteTableAlias(sentence, false)
            }
            COLUMN_ALIAS -> this.run {
                if (query.indexOf(' ') < 0) return@run
                var i = beginOfName(up)
                if (i == 0) return@run
                val alias = up.substring(0, i)
                if (ParserUtil.isKeyword(alias, false)) return@run
                s = s.substring(alias.length)
            }
            COLUMN -> {

            }
            else -> {
            }
        }
    }

    private fun autoCompleteTableAlias(sentence: Sentence, newAlias: Boolean): String {
        var s = sentence.query
        var up = sentence.queryUpper

        var i = beginOfName(up)
        if (i == 0) {
            return s
        }
        val alias = up.substring(0, i)
        if ("SET" == alias || ParserUtil.isKeyword(alias, false)) {
            return s
        }
        if (newAlias) {
            sentence.addAlias(alias, sentence.lastTable)
        }
        val map: java.util.HashMap<String, DbTableOrView?> = sentence.aliases
        if (map.containsKey(alias) || sentence.lastTable == null) {
            if (newAlias && s.length == alias.length) {
                return s
            }
            s = s.substring(alias.length)
            if (s.isEmpty()) {
                sentence.add("$alias.", ".", Sentence.CONTEXT)
            }
            return s
        }
        val tables: HashSet<DbTableOrView> = sentence.tables
        var best: String? = null
        for (table in tables) {
            val tableName = StringUtils.toUpperEnglish(table.name)
            if (alias.startsWith(tableName) &&
                    (best == null || tableName.length > best.length)) {
                sentence.lastMatchedTable = table
                best = tableName
            } else if (s.isEmpty() || tableName.startsWith(alias)) {
                sentence.add("$tableName.",
                        tableName.substring(s.length) + ".",
                        Sentence.CONTEXT)
            }
        }
        if (best != null) {
            s = s.substring(best.length)
            if (s.isEmpty()) {
                sentence.add("$alias.", ".", Sentence.CONTEXT)
            }
            return s
        }
        return s
    }


}