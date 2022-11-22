package org.h2.util

import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Network connection information.
 */
class NetworkConnectionInfo(var server: String? = null,
                            var clientAddr: ByteArray?,
                            var clientPort: Int = 0,
                            var clientInfo: String? = null) {
    /**
     * Creates new instance of network connection information.
     *
     * @param server the protocol and port of the server
     * @param clientAddr the client address
     * @param clientPort the client port
     * @throws UnknownHostException if clientAddr cannot be resolved
     */
    @Throws(UnknownHostException::class)
    constructor(server: String?, clientAddr: String?, clientPort: Int) : this(server, InetAddress.getByName(clientAddr).address, clientPort, null)

    /**
     * Returns the client address and port.
     *
     * @return the client address and port
     */
    fun getClient(): String {
        return NetUtils.ipToShortForm(StringBuilder(), clientAddr!!, true).append(':').append(clientPort).toString()
    }
}