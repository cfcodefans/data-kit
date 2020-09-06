package org.h2.security.auth

/**
 * Exception thrown when an issue occurs during the authentication configuration
 */
class AuthConfigException : RuntimeException {
    private val serialVersionUID = 1L
    constructor(message: String?) : super(message)
    constructor(cause: Throwable?) : super(cause)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}