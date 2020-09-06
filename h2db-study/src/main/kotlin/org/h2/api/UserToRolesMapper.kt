package org.h2.api

import org.h2.security.auth.AuthConfigException
import org.h2.security.auth.AuthenticationException
import org.h2.security.auth.AuthenticationInfo
import org.h2.security.auth.Configurable

/**
 * A class that implements this interface can be used during authentication to
 * map external users to database roles.
 * <p>
 *     <b>This feature is experimental and subject to change</b>
 * </p>
 */
interface UserToRolesMapper : Configurable {

    /**
     * Map user identified by authentication info to a set of granted roles.
     *
     * @param authenicationInfo authentication information
     * @return list of roles to be assigned to the user temporary
     * @throws AuthenticationException on authentication exception
     */
    @Throws(AuthenticationException::class)
    fun mapUserToRoles(authenticationInfo: AuthenticationInfo): Collection<String>
}