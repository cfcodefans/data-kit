package org.h2.value

import org.h2.api.ErrorCode
import org.h2.engine.Constants
import org.h2.message.DbException
import java.math.BigDecimal

/**
 * Base class for BigDecimal-based values.
 */
abstract class ValueBigDecimalBase(val value: BigDecimal?) : Value() {

    companion object {

    }

    override var type: TypeInfo? = null

    init {
        if (value != null) {
            if (value.javaClass != BigDecimal::class.java) {
                throw DbException.get(ErrorCode.INVALID_CLASS_2, BigDecimal::class.java.name, value.javaClass.name)
            }
            val length = value.precision()
            if (length > Constants.MAX_NUMERIC_PRECISION) {
                throw DbException.getValueTooLongException(getTypeName(getValueType()), value.toString(), length.toLong())
            }
        }
    }


}