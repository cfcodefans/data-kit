package org.h2.value

import org.h2.value.HasSQL.Companion.QUOTE_ONLY_WHEN_REQUIRED

/**
 * Extended parameters of a data type.
 */
abstract class ExtTypeInfo : HasSQL {
    override fun toString(): String = getSQL(QUOTE_ONLY_WHEN_REQUIRED)!!
}