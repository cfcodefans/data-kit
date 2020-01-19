package org.h2.expression

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
    fun setValue(value: Value, closeOld: Boolean)
}