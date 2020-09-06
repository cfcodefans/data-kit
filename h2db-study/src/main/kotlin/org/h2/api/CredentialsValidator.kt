package org.h2.api

import org.h2.security.auth.AuthenticationInfo
import org.h2.security.auth.Configurable

/**
 * A class that implements this interface can be used to validate credentials
 * provided by client.
 * <p>
 *     <b>This feature is experimental and subject to change</b>
 *     </p>
 */
interface CredentialsValidator : Configurable {

    /**
     * Validate user credential.
     * @param authenticationInfo = authentication info
     * @return true if credentials are valid, otherwise false
     * @throws Exception
     *      any exception occurred (invalid credentials or internal
     *      issue) prevent user login
     */
    @Throws(Exception::class)
    fun validateCredentials(authenticationInfo: AuthenticationInfo): Boolean
}