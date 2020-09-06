package org.h2.security.auth

import org.h2.engine.ConnectionInfo

/**
 * Input data for authentication; it wraps ConnectionInfo
 */
class AuthenticationInfo(val connectionInfo: ConnectionInfo) {

    var password: String? = null
        private set
    var realm: String? = null
        private set

    internal var nestedIdentity: Any? = null

    init {
        this.realm = connectionInfo.getProperty("AUTHREALM", null)?.toUpperCase()
        this.password = connectionInfo.getProperty("AUTHZPWD", null)
    }

    fun clean(): Unit {
        this.password = null
        this.nestedIdentity = null
        connectionInfo.cleanAuthenticationInfo()
    }

    fun getUserName(): String = connectionInfo.userName
    fun getFullyQualifiedName(): String = if (realm == null)
        connectionInfo.userName
    else
        connectionInfo.userName + "@" + realm
}