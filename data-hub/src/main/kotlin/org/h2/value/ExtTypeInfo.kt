package org.h2.value

/**
 * Extended parameters of a data type.
 */
abstract class ExtTypeInfo {
    /**
     * Casts a specified value to this data type.
     * @param value value to cast
     * @return casted value
     */
    abstract fun cast(value: Value): Value

    /**
     * Return SQL including parentheses that should be appended to a type name.
     * @return SQL including parentheses that should be appended to a type name
     */
    abstract fun getCreateSQL(): String

    override fun toString(): String = getCreateSQL()
}