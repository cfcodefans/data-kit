package org.h2.util

import java.net.InetAddress
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
    fun getHostName(localAddress: String?): String? = try {
        InetAddress.getByName(localAddress).hostName
    } catch (e: Exception) {
        "unknown"
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
                {
                    var i = 0
                    var offset = 0
                    while (i < 8) {

                        i++
                    }
                }
            }
            else -> StringUtils.convertBytesToHex(builder, address)
        }
        return builder
    }
}