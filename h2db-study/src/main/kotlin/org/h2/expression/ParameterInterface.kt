package org.h2.expression

import org.h2.message.DbException
import org.h2.value.TypeInfo
import org.h2.value.Value

/**
 * The interface for client side (remote) and server side parameters.
 */
interface ParameterInterface {
    /**
     * Set the value of the parameter.
     * @param value the new value
     * @param closeOld if the old value (if one is set) should be closed
     */
    fun setValue(value: Value?, closeOld: Boolean)

    /**
     * Get the value of the parameter if set.
     *
     * @return the value or null
     */
    fun getParamValue(): Value?

    /**
     * Check if the value is set.
     *
     * @throws DbException if not set.
     */
    @Throws(DbException::class)
    fun checkSet()

    /**
     * Is the value of a parameter set.
     *
     * @return true if set
     */
    fun isValueSet(): Boolean

    /**
     * Returns the expected data type if no value is set, or the
     * data type of the value if one is set.
     *
     * @return the data type
     */
    fun getType(): TypeInfo?

    /**
     * Check if this column is nullable.
     *
     * @return Column.NULLABLE_*
     */
    fun getNullable(): Int
}