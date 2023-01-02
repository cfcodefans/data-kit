package org.h2.security.auth

import org.h2.api.CredentialsValidator
import org.h2.api.UserToRolesMapper
import org.h2.engine.Database
import org.h2.engine.Right
import org.h2.engine.Role
import org.h2.engine.User
import org.h2.engine.UserBuilder
import org.h2.util.StringUtils

/**
 * Default authenticator implementation.
 * <p>
 * When client connectionInfo contains property AUTHREALM={realName} credentials
 * (typically user id and password) are validated by
 * {@link org.h2.api.CredentialsValidator) configured for that realm.
 * </p>
 * <p>
 * When client connectionInfo doesn't contains AUTHREALM property credentials
 * are validated internally on the database
 * </p>
 * <p>
 * Rights assignment can be managed through {@link org.h2.api.UserToRolesMapper}
 * </p>
 * <p>
 * Default configuration has a realm H2 that validate credentials through JAAS
 * api (appName=h2). To customize configuration set h2.authConfigFile system
 * property to refer to a valid h2auth.xml config file
 * </p>
 */
open class DefaultAuthenticator(private val skipDefaultInitialization: Boolean = false) : Authenticator {
    companion object {
        const val DEFAULT_REALMNAME: String = "H2"
        var instance: DefaultAuthenticator? = null
            private set
            @JvmStatic get() {
                if (field == null)
                    field = DefaultAuthenticator()
                return field
            }
    }

    val realms: MutableMap<String, CredentialsValidator> = HashMap()
    var userToRolesMappers: List<UserToRolesMapper> = ArrayList()
        private set
    /**
     * If set create external users in the database if not present.
     * true if creation external user is allowed,
     * otherwise returns false
     */
    var allowUserRegistration: Boolean = false

    /**
     * If set save users externals defined during the authentication.
     * if true, user will be persisted,
     * otherwise returns false
     */
    var persistUsers: Boolean = false

    /**
     * When set create roles not found in the database. If not set roles not
     * found in the database are silently skipped.
     * true if not found roles will be created,
     * roles are silently skipped.
     */
    var createMissingRoles: Boolean = false
    var initialized: Boolean = false

    /**
     * Create authenticator and optionally skip the default configuration. This
     * option is useful when the authenticator is configured at code level
     * @param skipDefaultInitialization if true default initialization is skipped
     */

    /**
     * Add an authentication realm. Realms are case insensitive
     * @param name realm name
     * @param credentialsValidator credentials validator for realm
     */
    fun addRealm(name: String, credentialsValidator: CredentialsValidator): Unit {
        realms[StringUtils.toUpperEnglish(name)] = credentialsValidator
    }

    fun setUserToRolesMappers(vararg userToRolesMappers: UserToRolesMapper): Unit {
        this.userToRolesMappers = userToRolesMappers.toList()
    }

    @Throws(AuthenticationException::class)
    override fun authenticate(authenticationInfo: AuthenticationInfo, database: Database): User? {
        val userName = authenticationInfo.getFullyQualifiedName()
        var user: User? = database.findUser(userName)
        if (user == null && !allowUserRegistration) throw AuthenticationException("User $userName not found in db")

        val validator = realms[authenticationInfo.realm] ?: throw AuthenticationException("realm ${authenticationInfo.realm} not configured")
        try {
            if (!validator.validateCredentials(authenticationInfo)) return null
        } catch (e: Exception) {
            throw AuthenticationException(e)
        }
        if (user == null) {
            synchronized(database.systemSession) {
                user = UserBuilder.buildUser(authenticationInfo, database, persistUsers)
                database.addDatabaseObject(database.systemSession, user)
                database.systemSession.commit(false)
            }
        }
        user!!.revokeTemporaryRightsOnRoles()
        updateRoles(authenticationInfo, user!!, database)
        return user!!
    }

    @Throws(AuthenticationException::class)
    private fun updateRoles(authenticationInfo: AuthenticationInfo, user: User, database: Database): Boolean {
        var updatedDb = false
        val roles: MutableSet<String> = HashSet()
        for (currentUserToRolesMapper in userToRolesMappers) {
            val currentRoles = currentUserToRolesMapper.mapUserToRoles(authenticationInfo)
            if (currentRoles != null && !currentRoles.isEmpty()) {
                roles.addAll(currentRoles)
            }
        }
        for (currentRoleName in roles) {
            if (currentRoleName == null || currentRoleName.isEmpty()) {
                continue
            }
            var currentRole: Role = database.findRole(currentRoleName)
            if (currentRole == null && isCreateMissingRoles()) {
                synchronized(database.systemSession) {
                    currentRole = Role(database, database.allocateObjectId(), currentRoleName, false)
                    database.addDatabaseObject(database.systemSession, currentRole)
                    database.systemSession.commit(false)
                    updatedDb = true
                }
            }
            if (currentRole == null) {
                continue
            }
            if (user.getRightForRole(currentRole) == null) {
                // NON PERSISTENT
                val currentRight = Right(database, -1, user, currentRole)
                currentRight.setTemporary(true)
                user.grantRole(currentRole, currentRight)
            }
        }
        return updatedDb
    }

    override fun init(database: Any) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}