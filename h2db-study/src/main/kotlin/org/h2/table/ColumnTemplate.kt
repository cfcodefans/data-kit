package org.h2.table

import org.h2.engine.SessionLocal
import org.h2.expression.Expression
import org.h2.schema.Domain

/**
 * Column or domain.
 */
interface ColumnTemplate {
    var domain: Domain?

    /**
     * Set the default expression.
     *
     * @param session the session
     * @param defaultExpression the default expression
     */
    fun setDefaultExpression(session: SessionLocal?, defaultExpression: Expression?)

    fun getDefaultExpression(): Expression?

    fun getEffectiveDefaultExpression(): Expression?

    fun getDefaultSQL(): String?

    /**
     * Set the on update expression.
     * @param session the session
     * @param onUpdateExpression the on update expression
     */
    fun setOnUpdateExpression(session: SessionLocal?, onUpdateExpression: Expression?)

    fun getOnUpdateExpression(): Expression?

    fun getEffectiveOnUpdateExpression(): Expression?

    fun getOnUpdateSQL(): String?

    /**
     * Prepare all expressions of this column or domain.
     * @param session the session
     */
    fun prepareExpressions(session: SessionLocal?)
}