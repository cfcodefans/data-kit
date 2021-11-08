package org.h2.security.auth

import org.h2.util.Utils.parseBoolean

/**
 * wrapper for configuration properties
 */
class ConfigProperties {
    private var properties: MutableMap<String?, String?> = HashMap()

    constructor(configProperties: Collection<PropertyConfig>) {
        for ((name, value) in configProperties) {
            if (properties.containsKey(name)) {
                throw AuthConfigException("duplicate property $name")
            }
            properties[name] = value
        }
    }

    constructor(vararg configProperties: PropertyConfig) : this(configProperties.toList())

    /**
     * Returns the string value of specified property.
     *
     * @param name property name.
     * @param defalutValue default value.
     * @return the string property value or {@code defaultValue} if the property is missing.
     */
    fun getStringValue(name: String?, defaultValue: String?): String? = properties.getOrDefault(name, defaultValue)

    /**
     * Returns the string value of specified property.
     *
     * @param name property name
     * @return the string property value.
     * @throws AuthConfigException if the property is missing.
     */
    fun getStringValue(name: String?): String = properties[name]
        ?: throw AuthConfigException("missing config property $name")

    /**
     * Returns the integer value of specified property.
     *
     * @param name property name.
     * @param defaultValue default value.
     * @return the integer property value or {@code defaultValue} if the property is missing.
     */
    fun getIntValue(name: String?, defaultValue: Int): Int = properties[name]?.toInt() ?: defaultValue

    /**
     * Returns the integer value of specified property.
     *
     * @param name property name.
     * @return the integer property value.
     * @throws AuthConfigException if the property is missing.
     */
    fun getIntValue(name: String?): Int = properties[name]?.toInt()
        ?: throw AuthConfigException("missing config property $name")

    fun getBooleanValue(name: String?, defaultValue: Boolean): Boolean = properties[name]?.let { parseBoolean(it, defaultValue, true) }
        ?: defaultValue
}