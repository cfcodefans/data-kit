package org.h2.api

/**
 * Custom data type handler
 * Provides means to plun-in custom data type support
 *
 * Please keep in mind that this feature may not possibly
 * provide the same ABI stability level as other features
 * as it exposes many of the H2 internals. You may be
 * required to update your code occasionally due to internal
 * change in H2 if you are going to use this feature
 */
interface CustomDataTypesHandler {
    /**
     * Get custom data type given its name
     * @param name data type name
     * @return custom data type
     */
    fun getDataTypeByName(name:String):DataType
}