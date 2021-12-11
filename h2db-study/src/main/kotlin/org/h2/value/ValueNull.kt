package org.h2.value

import java.sql.PreparedStatement

/**
 * Implementation of NULL. NULL is not a regular data type.
 */
class ValueNull : Value() {
    companion object {
        /**
         * The main NULL instance.
         */
        @JvmStatic
        val INSTANCE: ValueNull = ValueNull()
        /**
         * The precision of NULL
         */
        const val PRECISION: Int = 1
        /**
         * The display size of the textual representation of NULL.
         */
        const val DISPLAY_SIZE: Int = 4
    }

    override fun getSQL(builder: StringBuilder): StringBuilder {
        TODO("Not yet implemented")
    }

    override fun getType(): TypeInfo = TypeInfo.TYPE_NULL

    override fun getValueType(): Int = NULL

    override fun getString(): String? = null

    override fun getObject(): Any? {
        TODO("Not yet implemented")
    }

    override fun set(prep: PreparedStatement, parameterIndex: Int) {
        TODO("Not yet implemented")
    }

    override fun hashCode(): Int {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean {
        TODO("Not yet implemented")
    }
}