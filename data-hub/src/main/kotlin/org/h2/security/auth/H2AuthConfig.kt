package org.h2.security.auth

/**
 * Describe configuration of H2 DefaultAuthenticator.
 */
data class H2AuthConfig(
        /**
         * Allow user registration flag. if set to {@code true}
         * creates external users in the database if not present.
         */
        var allowUserRegistration: Boolean = true,
        /**
         * When set create roles not found in the database. if not set roles not
         * found in the database are silently skipped.
         */
        var createMissingRoles: Boolean = true
) {
    var realms: MutableList<RealmConfig>? = arrayListOf()
        get() {
            if (field == null) field = arrayListOf()
            return field
        }

    var userToRolesMappers: MutableList<UserToRolesMapperConfig>? = arrayListOf()
        get() {
            if (field == null) field = arrayListOf()
            return field
        }
}