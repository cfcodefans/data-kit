package org.h2.security.auth.impl

import com.google.common.hash.Hashing
import org.h2.api.CredentialsValidator
import org.h2.security.auth.AuthenticationInfo
import org.h2.security.auth.ConfigProperties
import org.h2.util.StringUtils
import org.h2.util.Utils
import java.util.regex.Pattern
import kotlin.random.Random

/**
 * This credentials validator matches the user and password with the configured
 * Usage should be limited to test purposes
 */
class StaticUserCredentialsValidator : CredentialsValidator {

    private var userNamePattern: Pattern? = null
    private lateinit var salt: ByteArray
    private lateinit var hashWithSalt: ByteArray
    private var password: String? = null

    constructor(userNamePattern: String?, password: String) {
        if (userNamePattern != null)
            this.userNamePattern = Pattern.compile(userNamePattern.toUpperCase())
        salt = Random.Default.nextBytes(256)
        hashWithSalt = Hashing.sha256().hashBytes(password.toByteArray() + salt).asBytes()
    }

    override fun validateCredentials(authenticationInfo: AuthenticationInfo): Boolean {
        if (userNamePattern?.matcher(authenticationInfo.getUserName())?.matches() == false) {
            return false
        }
        if (password != null) return password == authenticationInfo.password
        return Utils.compareSecure(hashWithSalt,
                Hashing.sha256()
                        .hashBytes(authenticationInfo.password!!.toByteArray() + salt)
                        .asBytes())
    }

    override fun configure(configProperties: ConfigProperties) {
        configProperties.getStringValue("userNamePattern", null).let {
            this.userNamePattern = Pattern.compile(it)
        }
        password = configProperties.getStringValue("password", password)!!
        configProperties.getStringValue("salt", null)?.let {
            salt = StringUtils.convertHexToBytes(it)
        }
        configProperties.getStringValue("hash", null)?.let {
            hashWithSalt = Hashing.sha256()
                    .hashBytes(StringUtils.convertHexToBytes(it) + salt)
                    .asBytes()
        }
    }


}