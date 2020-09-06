package org.h2.security.auth

/**
 * describe how to perform objects runtime configuration
 */
interface Configurable {
    /**
     * configure the component
     * @param configProperties = configuration properties
     */
    fun configure(configProperties: ConfigProperties)
}