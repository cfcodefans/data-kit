package org.h2.security.auth

/**
 * Configuration for class that maps users to their roles.
 * @see org.h2.api.UserToRolesMapper
 */
data class UserToRolesMapperConfig(
        var className: String? = null,
        var properties: MutableList<PropertyConfig>? = arrayListOf()
) : () -> MutableList<PropertyConfig> {
    override fun invoke(): MutableList<PropertyConfig> {
        if (properties == null) properties = arrayListOf()
        return properties!!
    }
}