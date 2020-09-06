package org.h2.security.auth

class AuthenticationException : Exception {
    private val serialVersionUID = 1L

    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(cause: Throwable?) : super(cause)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}