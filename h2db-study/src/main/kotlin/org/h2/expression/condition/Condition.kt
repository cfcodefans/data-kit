package org.h2.expression.condition

import org.h2.engine.SessionLocal
import org.h2.expression.Expression
import org.h2.expression.function.CastSpecification
import org.h2.value.TypeInfo
import org.h2.value.Value


/**
 * Represents a condition returning a boolean value, or NULL.
 */
abstract class Condition : Expression() {
    companion object {
        /**
         * Add a cast around the expression (if necessary) so that the type is boolean.
         * @param session the session
         * @param expression the expression
         * @return the new expression
         */
        fun castToBoolean(session: SessionLocal?, expression: Expression): Expression = if (expression.type!!.valueType == Value.BOOLEAN)
            expression
        else CastSpecification(expression, TypeInfo.TYPE_BOOLEAN)
    }

    override var type: TypeInfo? = TypeInfo.TYPE_BOOLEAN

}