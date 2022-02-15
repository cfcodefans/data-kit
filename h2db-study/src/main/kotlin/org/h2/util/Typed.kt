package org.h2.util

import org.h2.value.TypeInfo

/**
 * An object with data type.
 */
interface Typed {
    /**
     * Returns the data type.
     *
     * @return the data type
     */
    var type: TypeInfo?
}