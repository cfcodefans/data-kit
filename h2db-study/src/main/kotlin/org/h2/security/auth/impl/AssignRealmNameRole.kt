package org.h2.security.auth.impl

import org.h2.api.UserToRolesMapper
import org.h2.security.auth.AuthenticationInfo
import org.h2.security.auth.ConfigProperties

/**
 * Assign to user a role based on realm name
 * *<p>
 *     Configuration parameters:
 * </p>
 * <ul>
 *     <li>roleNameFormat, optional by default is @{realm{</li>
 *     </ul>
 */
class AssignRealmNameRole(private var roleNameFormat: String?) : UserToRolesMapper {
    constructor() : this("@%s")

    override fun configure(configProperties: ConfigProperties) {
        roleNameFormat = configProperties.getStringValue("roleNameFormat", roleNameFormat)
    }

    override fun mapUserToRoles(authenticationInfo: AuthenticationInfo): Collection<String> {
        return listOf(roleNameFormat!!.format(authenticationInfo.realm))
    }
}