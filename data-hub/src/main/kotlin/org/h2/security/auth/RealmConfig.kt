package org.h2.security.auth

/**
 * Configuration for authentication realm.
 */
data class RealmConfig(
        var name: String? = null,
        var validatorClass: String? = null,
        var properties: MutableList<PropertyConfig>? = arrayListOf()
) : () -> MutableList<PropertyConfig> {
    override fun invoke(): MutableList<PropertyConfig> {
        if (properties == null) properties = arrayListOf()
        return properties!!
    }
}