package org.h2.security.auth

/**
 * Low level interface to implement full authentication process.
 */
interface Authenticator {
    /**
     * Perform user authentication
     *
     * @param authenticationInfo authentication info.
     * @param database target database instance.
     * @return valid database user or null if user doesn't exists in the database
     */
    @Throws(AuthenticationException::class)
    fun authenticate(authenticationInfo: AuthenticationInfo, database: Any): Any //User

    /**
     * Initialize the authenticator. This method is invoked by databases when
     * the authenticator is set when the authenticator is set
     *
     * @param database target database
     */
    @Throws(AuthConfigException::class)
    fun init(database: Any): Unit
}