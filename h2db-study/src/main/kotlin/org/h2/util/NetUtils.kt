package org.h2.util

import org.h2.api.ErrorCode
import org.h2.engine.SysProperties
import org.h2.message.DbException
import org.h2.security.CipherFactory
import org.h2.util.Utils.uncheckedSleep
import java.io.IOException
import java.net.BindException
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.experimental.and
import kotlin.math.min

/**
 * This utility class contains socket helper functions.
 */
object NetUtils {
    private const val CACHE_MILLIS = 1000
    private var cachedBindAddress: InetAddress? = null
    private var cachedLocalAddress: String? = null
    private var cachedLocalAddressTime: Long = 0

    /**
     * Get the host name of a local address, if available.
     * @param localAddress the local address
     * @return the host name, or another text if not available
     */
    fun getHostName(localAddress: String?): String? = try {
        InetAddress.getByName(localAddress).hostName
    } catch (e: Exception) {
        "unknown"
    }

    /**
     * Create a loopback socket (a socket that is connected to localhost) on
     * this port.
     *
     * @param port the port
     * @param ssl if SSL should be used
     * @return the socket
     */
    @Throws(IOException::class)
    fun createLoopbackSocket(port: Int, ssl: Boolean): Socket? {
        val local: String = NetUtils.getLocalAddress()
        return try {
            createSocket(local, port, ssl)
        } catch (e: IOException) {
            try {
                createSocket("localhost", port, ssl)
            } catch (e2: IOException) {
                // throw the original exception
                throw e
            }
        }
    }

    /**
     * Create a client socket that is connected to the given address and port.
     *
     * @param server to connect to (including an optional port)
     * @param defaultPort the default port (if not specified in the server
     * address)
     * @param ssl if SSL should be used
     * @return the socket
     */
    @Throws(IOException::class)
    fun createSocket(server: String, defaultPort: Int, ssl: Boolean): Socket? {
        var _server = server
        var port = defaultPort
        // IPv6: RFC 2732 format is '[a:b:c:d:e:f:g:h]' or
        // '[a:b:c:d:e:f:g:h]:port'
        // RFC 2396 format is 'a.b.c.d' or 'a.b.c.d:port' or 'hostname' or
        // 'hostname:port'
        val startIndex = if (server.startsWith("[")) server.indexOf(']') else 0
        val idx = server.indexOf(':', startIndex)
        if (idx >= 0) {
            port = Integer.decode(server.substring(idx + 1))
            _server = server.substring(0, idx)
        }
        val address = InetAddress.getByName(_server)
        return createSocket(address, port, ssl)
    }

    /**
     * Create a client socket that is connected to the given address and port.
     *
     * @param address the address to connect to
     * @param port the port
     * @param ssl if SSL should be used
     * @return the socket
     */
    @Throws(IOException::class)
    fun createSocket(address: InetAddress?, port: Int, ssl: Boolean): Socket? {
        val start = System.nanoTime()
        for (i in 0 until Int.MAX_VALUE) {
            try {
                if (ssl) return CipherFactory.createSocket(address, port)
                val socket = Socket()
                socket.connect(InetSocketAddress(address, port), SysProperties.SOCKET_CONNECT_TIMEOUT)
                return socket
            } catch (e: IOException) {
                if (System.nanoTime() - start >=
                    TimeUnit.MILLISECONDS.toNanos(SysProperties.SOCKET_CONNECT_TIMEOUT.toLong())
                ) {
                    // either it was a connect timeout,
                    // or list of different exceptions
                    throw e
                }
                if (i >= SysProperties.SOCKET_CONNECT_RETRY) throw e
                // wait a bit and retry
                uncheckedSleep(min(256, i * i).toLong())
            }
        }
        return null
    }

    /**
     * Appends short representation of the specified IP address to the string
     * builder.
     * @param builder string builder to append to, or {@code null}
     * @param address IP address
     * @param addBrackets if ({@code true}, add brackets around IPv6 addresses
     * @return the specified or the new string builder with short representation
     *         of specified address
     */
    fun ipToShortForm(sb: StringBuilder?, address: ByteArray, addBrackets: Boolean): StringBuilder {
        val builder: StringBuilder = sb ?: when (address.size) {
            4 -> 15
            16 -> if (addBrackets) 41 else 39
            else -> 32
        }.let { StringBuilder(it) }
        when (address.size) {
            4 -> {
                val ff = UByte.MAX_VALUE.toByte()
                builder.append(address[0] and ff).append('.') //
                    .append(address[1] and ff).append('.') //
                    .append(address[2] and ff).append('.') //
                    .append(address[3] and ff).toString()
            }

            16 -> {
                val a = ShortArray(8)
                var maxStart = 0
                var maxLen = 0
                var currentLen = 0
                run {
                    var i = 0
                    var offset = 0
                    while (i < 8) {
                        a[i] = (address[offset++].toInt() and 0xff shl 8 or address[offset++].toInt() and 0xff).toShort()
                        if (a[i] == 0.toShort()) {
                            currentLen++
                            if (currentLen > maxLen) {
                                maxLen = currentLen
                                maxStart = i - currentLen + 1
                            }
                        } else currentLen = 0
                        i++
                    }
                }
                if (addBrackets) builder.append('[')
                val start: Int = if (maxLen > 1) {
                    for (i in 0 until maxStart) {
                        builder.append(Integer.toHexString(a[i].toInt() and 0xffff)).append(':')
                    }
                    if (maxStart == 0) builder.append(':')
                    builder.append(':')
                    maxStart + maxLen
                } else 0
                for (i in start..7) {
                    builder.append(Integer.toHexString(a[i].toInt() and 0xffff))
                    if (i < 7) {
                        builder.append(':')
                    }
                }
                if (addBrackets) builder.append(']')
            }

            else -> StringUtils.convertBytesToHex(builder, address)
        }
        return builder
    }

    /**
     * Create a server socket. The system property h2.bindAddress is used if
     * set. If SSL is used and h2.enableAnonymousTLS is true, an attempt is
     * made to modify the security property jdk.tls.legacyAlgorithms
     * (in newer JVMs) to allow anonymous TLS.
     *
     *
     * This system change is effectively permanent for the lifetime of the JVM.
     * @see CipherFactory.removeAnonFromLegacyAlgorithms
     * @param port the port to listen on
     * @param ssl if SSL should be used
     * @return the server socket
     */
    fun createServerSocket(port: Int, ssl: Boolean): ServerSocket = try {
        createServerSocketTry(port, ssl)
    } catch (e: java.lang.Exception) {
        // try again
        createServerSocketTry(port, ssl)
    }

    /**
     * Get the bind address if the system property h2.bindAddress is set, or
     * null if not.
     *
     * @return the bind address
     */
    @Throws(UnknownHostException::class)
    private fun getBindAddress(): InetAddress? {
        val host = SysProperties.BIND_ADDRESS
        if (host.isNullOrEmpty()) return null

        synchronized(NetUtils::class.java) {
            if (cachedBindAddress == null) cachedBindAddress = InetAddress.getByName(host)
        }
        return cachedBindAddress
    }

    private fun createServerSocketTry(port: Int, ssl: Boolean): ServerSocket {
        return try {
            val bindAddress = getBindAddress()
            if (ssl) return CipherFactory.createServerSocket(port, bindAddress)

            if (bindAddress == null) {
                ServerSocket(port)
            } else {
                ServerSocket(port, 0, bindAddress)
            }
        } catch (be: BindException) {
            throw DbException.get(ErrorCode.EXCEPTION_OPENING_PORT_2, be, port.toString(), be.toString())
        } catch (e: IOException) {
            throw DbException.convertIOException(e, "port: $port ssl: $ssl")
        }
    }

    /**
     * Close a server socket and ignore any exceptions.
     *
     * @param socket the socket
     * @return null
     */
    fun closeSilently(socket: ServerSocket?): ServerSocket? {
        kotlin.runCatching { socket?.close() }
        return null
    }

    /**
     * Get the local host address as a string.
     * For performance, the result is cached for one second.
     *
     * @return the local host address
     */
    @Synchronized
    fun getLocalAddress(): String {
        val now: Long = System.nanoTime()
        if (cachedLocalAddress != null && now - cachedLocalAddressTime < CACHE_MILLIS * 1000000L) {
            return cachedLocalAddress!!
        }

        cachedLocalAddress = (getBindAddress() ?: InetAddress.getLocalHost())
            ?.let { bind ->
                var address: String = bind.hostAddress
                if (bind is Inet6Address) {
                    if (address.indexOf('%') >= 0) {
                        address = "localhost"
                    } else if (address.indexOf(':') >= 0 && !address.startsWith("[")) {
                        // adds'[' and ']' if required for
                        // Inet6Address that contain a ':'.
                        address = "[$address]"
                    }
                }
                if (address == "127.0.0.1") "localhost" else address
            }
        cachedLocalAddressTime = now
        return cachedLocalAddress!!
    }

}