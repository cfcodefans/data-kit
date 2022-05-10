package org.h2.expression

import org.h2.value.ExtTypeInfoRow
import org.h2.value.TypeInfo
import org.h2.value.Value

/**
 * A list of expressions, as in (ID, NAME).
 * The result of this expression is a row or an array.
 */
class ExpressionList(private val list: Array<Expression?>,
                     private val isArray: Boolean = false) : Expression() {

    override var type: TypeInfo? = null

    fun initializeType() {
        type = if (isArray)
            TypeInfo.getTypeInfo(Value.ARRAY, list!!.size.toLong(), 0, TypeInfo.getHigherType(list))
        else
            TypeInfo.getTypeInfo(Value.ROW, 0, 0, ExtTypeInfoRow(list))
    }
}