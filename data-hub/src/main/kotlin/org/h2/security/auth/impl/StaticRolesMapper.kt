package org.h2.security.auth.impl

import org.h2.api.UserToRolesMapper
import org.h2.security.auth.AuthenticationInfo
import org.h2.security.auth.ConfigProperties

/**
 * Assign static roles to authenticated users
 * <p>Configuration parameters:</p>
 * <ul>
 *     <li>roles role list separated by comma</li>
 *     </ul>
 */
class StaticRolesMapper(private vararg var roles: String) : UserToRolesMapper {
    override fun mapUserToRoles(authenticationInfo: AuthenticationInfo) = roles.toList()

    override fun configure(configProperties: ConfigProperties) {
        roles = configProperties.getStringValue("roles", "")
                .split(",")
                .toHashSet()
                .toTypedArray()
    }
}