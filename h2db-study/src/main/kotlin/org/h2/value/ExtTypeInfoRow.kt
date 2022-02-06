package org.h2.value

import org.h2.api.ErrorCode
import org.h2.engine.Constants
import org.h2.message.DbException
import org.h2.util.ParserUtil

/**
 * Extended parameters of the ROW data type.
 */
class ExtTypeInfoRow(private val fields: LinkedHashMap<String, TypeInfo>) : ExtTypeInfo() {

    init {
        if (fields.size > Constants.MAX_COLUMNS) {
            throw DbException.get(ErrorCode.TOO_MANY_COLUMNS_1, "${Constants.MAX_COLUMNS}")
        }
    }

    /**
     * Creates new instance of extended parameters of ROW data type.
     *
     * @param fields fields
     * @param degree number of fields to use
     */
    constructor(fields: Array<Typed>, degree: Int) : this(fields = fields.let {
        if (degree > Constants.MAX_COLUMNS) throw DbException.get(ErrorCode.TOO_MANY_COLUMNS_1, "${Constants.MAX_COLUMNS}")
        it.take(degree)
                .mapIndexed { index, typed -> "C${1 + index}" to typed.type!! }
                .toMap()
    }.let { LinkedHashMap<String, TypeInfo>(it) })

    /**
     * Creates new instance of extended parameters of ROW data type.
     *
     * @param fields fields
     */
    constructor(fields: Array<Typed>) : this(fields, fields.size)

    /**
     * Returns fields.
     * @return fields
     */
    fun getFields(): Set<Map.Entry<String, TypeInfo>> = fields.entries

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder {
        builder.append('(')
        var f = false
        for ((key, value) in fields) {
            if (f) {
                builder.append(", ")
            }
            f = true
            ParserUtil.quoteIdentifier(builder, key, sqlFlags).append(' ')
            value.getSQL(builder, sqlFlags)
        }
        return builder.append(')')
    }

    private var hash = 0

    override fun hashCode(): Int {
        if (hash != 0) return hash

        hash = fields.entries.fold(67378403) { h, entry ->
            (h * 31 + entry.key.hashCode()) * 37 + entry.value.hashCode()
        }
        return hash
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
}