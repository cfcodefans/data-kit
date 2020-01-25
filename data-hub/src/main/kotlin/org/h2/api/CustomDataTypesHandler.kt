package org.h2.api

import org.h2.store.DataHandler
import org.h2.value.DataType
import org.h2.value.ExtTypeInfo
import org.h2.value.TypeInfo
import org.h2.value.Value

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
    fun getDataTypeByName(name: String): DataType

    /**
     * Get custom data type given its integer id
     * @param type identifier of a data type
     * @return custom data type
     */
    fun getDataTypeById(type: Int): DataType

    /**
     * Get type info for the given data type identity
     * @param type identifier of a data type
     * @param precision precision
     * @param scale scale
     * @param extTypeInfo the extended type informatioin, or null
     * @return type information
     */
    fun getTypeInfoById(type: Int, precision: Long, scale: Int, extTypeInfo: ExtTypeInfo?): TypeInfo

    /**
     * Get order for custom data type given its integer id
     * @param type identifier of a data type
     * @return order associated with custom data type
     */
    fun getDataTypeOrder(type: Int): Int

    /**
     * Convert the provided source value into value of given target data type
     * Shall implement conversions to and from custom data types.
     * @param source source value
     * @param targetType identifier of target data type
     * @return converted value
     */
    fun convert(source: Value, targetType: Int): Value

    /**
     * Get custom data type class name given its integer id
     * @param type identifier of a data type
     * @return class name
     */
    fun getDataTypeClassName(type: Int): String

    /**
     * Get custom data type identifier given corresponding Java class
     * @param cls Java class object
     * @return type identifier
     */
    fun getTypedIdFromClass(cls: Class<*>): Int

    /**
     * Get {@link org.h2.value.Value} object
     * corresponding to give data type identifier and data.
     * @param type custom data type identifier
     * @param data underlying data type value
     * @param dataHandler data handler object
     * @return Value object
     */
    fun getValue(type: Int, data: Any, dataHandler: DataHandler): Value

    /**
     * Converts {@link org.h2.value.Value} object
     * to the specified class.
     * @param value the value to convert
     * @param cls the target class
     * @return result
     */
    fun getObject(value: Value, cls: Class<*>): Any

    /**
     * Check if type supports add operation
     * @param type custom data type identifier
     * @return True, if custom data type supports add operation
     */
    fun supportsAdd(type: Int): Boolean

    /**
     * Get compatible type identifier that would not overflow
     * after many add operations.
     * @param type identifier of a type
     * @return resulting type identifier
     */
    fun getAddProofType(type: Int): Int
}