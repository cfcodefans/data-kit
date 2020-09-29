package org.h2.util

import org.h2.api.ErrorCode
import org.h2.engine.SysProperties
import org.h2.message.DbException
import org.h2.security.CipherFactory
import java.io.IOException
import java.net.*
import java.util.concurrent.TimeUnit
import kotlin.experimental.and

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
    fun getHostName(localAddress: String?): String? = runCatching { InetAddress.getByName(localAddress).hostName }.getOrElse { "unknown" }

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
                        val addr = address[offset++].toInt()
                        a[i] = (addr and 0xff shl 8 or addr and 0xff).toShort()
                        if (a[i].toInt() == 0) {
                            currentLen++
                            if (currentLen > maxLen) {
                                maxLen = currentLen
                                maxStart = i - currentLen + 1
                            }
                        } else {
                            currentLen = 0
                        }
                        i++
                    }
                }
                if (addBrackets) builder.append('[')
                val start: Int = if (maxLen > 1) {
                    for (i in 0 until maxStart) {
                        builder.append(Integer.toHexString((a[i].toInt() and 0xffff))).append(':')
                    }
                    if (maxStart == 0) builder.append(':')
                    builder.append(':')
                    maxStart + maxLen
                } else {
                    0
                }
                for (i in start..7) {
                    builder.append(Integer.toHexString((a[i].toInt() and 0xffff)))
                    if (i < 7) builder.append(':')
                }
                if (addBrackets) builder.append(']')
            }
            else -> StringUtils.convertBytesToHex(builder, address)
        }
        return builder
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
            if (cachedBindAddress == null) {
                cachedBindAddress = InetAddress.getByName(host)
            }
        }
        return cachedBindAddress
    }

    /**
     * Get the local host address as a string.
     * For performance, the result is cached for one second.
     *
     * @return the local host address
     */
    @JvmStatic
    @Synchronized
    fun getLocalAddress(): String? {
        val now = System.nanoTime()
        if (cachedLocalAddressTime + TimeUnit.MILLISECONDS.toNanos(CACHE_MILLIS.toLong()) > now
                && cachedLocalAddress != null) {
            return cachedLocalAddress
        }
        val bind: InetAddress? = runCatching { getBindAddress() }.getOrElse {
            runCatching { InetAddress.getLocalHost() }
                    .onFailure { throw DbException.convert(it) }
                    .getOrNull()
        }
        var address: String = bind?.hostAddress ?: "localhost"
        if (bind is Inet6Address) {
            if (address.indexOf('%') >= 0) {
                address = "localhost"
            } else if (address.indexOf(':') >= 0 && !address.startsWith("[")) {
                // adds'[' and ']' if required for
                // Inet6Address that contain a ':'.
                address = "[$address]"
            }
        }

        if (address == "127.0.0.1") {
            address = "localhost"
        }
        cachedLocalAddress = address
        cachedLocalAddressTime = now
        return address
    }

    /**
     * Check if a socket is connected to a local address.
     *
     * @param socket the socket
     * @return true if it is
     */
    @Throws(UnknownHostException::class)
    fun isLocalAddress(socket: Socket): Boolean {
        val test = socket.inetAddress
        if (test.isLoopbackAddress) {
            return true
        }
        val localhost = InetAddress.getLocalHost()
        // localhost.getCanonicalHostName() is very slow
        val host = localhost.hostAddress
        return InetAddress.getAllByName(host).contains(test)
    }

    private fun createServerSocketTry(port: Int, ssl: Boolean): ServerSocket? = try {
        val bindAddress = getBindAddress()
        when {
            ssl -> CipherFactory.createServerSocket(port, bindAddress)
            bindAddress == null -> ServerSocket(port)
            else -> ServerSocket(port, 0, bindAddress)
        }
    } catch (be: BindException) {
        throw DbException.get(ErrorCode.EXCEPTION_OPENING_PORT_2, be, port.toString(), be.toString())
    } catch (e: IOException) {
        throw DbException.convertIOException(e, "port: $port ssl: $ssl")
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
    fun createServerSocket(port: Int, ssl: Boolean): ServerSocket? {
        return try {
            createServerSocketTry(port, ssl)
        } catch (e: Exception) {
            // try again
            createServerSocketTry(port, ssl)
        }
    }
}