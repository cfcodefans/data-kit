package org.h2.security.auth.impl

import org.h2.api.CredentialsValidator
import org.h2.security.auth.AuthenticationInfo
import org.h2.security.auth.ConfigProperties
import java.util.*
import javax.naming.Context
import javax.naming.directory.DirContext
import javax.naming.directory.InitialDirContext

/**
 * Validate credentials by performing an LDAP bind
 * <p>Configuration parameters</p>
 * <ul>
 *     <li>bindDnPattern bind dn pattern with %u instead of username
 *     (example: uid=%u,ou=users,dc=example,dc=com)</li>
 *     <li>host ldap server</li>
 *     <li>port of ldap service; optional, by default 389 for insecure, 636 for secure</li>
 *     <li>secure, optional by default is true (use SSL)</li>
 *     </ul>
 */
class LdapCredentialsValidator : CredentialsValidator {
    private lateinit var bindDnPattern: String
    private lateinit var host: String
    private var port: Int = 0
    private var secure: Boolean = true
    private lateinit var url: String


    override fun validateCredentials(authenticationInfo: AuthenticationInfo): Boolean {
        val dn: String = bindDnPattern.replace("%u", authenticationInfo.getUserName())

        val env: Hashtable<String, String> = Hashtable()
        env[Context.INITIAL_CONTEXT_FACTORY] = "com.sun.jndi.ldap.LdapCtxFactory"
        env[Context.PROVIDER_URL] = url
        env[Context.SECURITY_PRINCIPAL] = dn
        env[Context.SECURITY_CREDENTIALS] = authenticationInfo.password
        if (secure) {
            env[Context.SECURITY_PROTOCOL] = "ssl"
        }

        var dirContext: DirContext? = null
        try {
            dirContext = InitialDirContext(env)
            authenticationInfo.nestedIdentity = dn
            return true
        } finally {
            dirContext?.close()
        }
    }

    override fun configure(configProperties: ConfigProperties) {
        bindDnPattern = configProperties.getStringValue("bindDnPattern")
        host = configProperties.getStringValue("host")
        secure = configProperties.getBooleanValue("secure", true)
        port = configProperties.getIntValue("port", if (secure) 636 else 389)
        url = "ldap${if (secure) "s" else ""}://$host:$port"
    }
}