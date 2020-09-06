package org.h2.security.auth.impl

import org.h2.api.CredentialsValidator
import org.h2.security.auth.AuthenticationInfo
import org.h2.security.auth.ConfigProperties
import java.io.IOException
import javax.security.auth.callback.*
import javax.security.auth.login.LoginContext

class AuthenticationInfoCallbackHandler(private val authenticationInfo: AuthenticationInfo) : CallbackHandler {
    @Throws(IOException::class, UnsupportedCallbackException::class)
    override fun handle(callbacks: Array<out Callback>?) {
        callbacks ?: return
        for (callback in callbacks!!) {
            when (callback) {
                is NameCallback -> callback.name = authenticationInfo.getUserName()
                is PasswordCallback -> callback.password = authenticationInfo.password!!.toCharArray()
            }
        }
    }
}

/**
 * Validate credentials by using standard Java Authentication and Authorization Service
 *
 * <p>
 * Configuration parameters:
 * </p>
 * <ul>
 *    <li>appName inside the JAAS configuration (by default h2)</li>
 * </ul>
 */
/**
 * Create the validator with the given name of JAAS configuration
 * @param appName = name of JAAS configuration
 */
class JaasCredentialsValidator(private var appName: String?) : CredentialsValidator {
    companion object {
        const val DEFAULT_APPNAME: String = "h2"
    }

    constructor() : this(DEFAULT_APPNAME)

    @Throws(Exception::class)
    override fun validateCredentials(authenticationInfo: AuthenticationInfo): Boolean {
        val loginContext: LoginContext = LoginContext(appName, AuthenticationInfoCallbackHandler(authenticationInfo))
        loginContext.login()
        return true
    }

    override fun configure(configProperties: ConfigProperties) {
        appName = configProperties.getStringValue("appName", appName)
    }
}