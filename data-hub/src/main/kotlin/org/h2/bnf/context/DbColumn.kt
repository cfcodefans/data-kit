package org.h2.bnf.context

/**
 * Keeps the meta data information of a column.
 * This class is used by the H2 Console
 */
data class DbColumn(val name: String,
                    val quotedName: String,
                    val dataType: String,
                    val position: Int) {

    constructor(contents:DbCon)
}