package org.h2.util

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.lang.reflect.Method
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

/**
 * This is a utility class with mathematical helper functions.
 */
object MathUtils {
    /**
     * Print a message to system output if there was a problem initializing the
     * random number generator
     *
     * @param s the message to print
     * @param t the stack trace
     */
    @JvmStatic
    internal fun warn(s: String, t: Throwable?): Unit {
        //not a fatal problem, but maybe reduced security
        println("Warning: $s")
        t?.printStackTrace()
    }

    /**
     * true if the secure random object is seeded.
     */
    @JvmStatic
    @Volatile
    internal var seeded: Boolean = false

    /**
     * The secure random object.
     */
    var cachedSecureRandom: SecureRandom? = null
        private set
        @Synchronized
        get() {
            if (field != null) return field
            try {
                cachedSecureRandom = SecureRandom.getInstance("SHA1PRNG")
                // On some systems, secureRandom generateSeed() is very slow.
                // In this case it is initialized using our own seed implementation
                // and afterwards (in the thread) using the regular algorithm
                val runnable: Runnable = Runnable {
                    try {
                        val sr: SecureRandom = SecureRandom.getInstance("SHA1PRNG")
                        val seed: ByteArray = sr.generateSeed(20)
                        synchronized(cachedSecureRandom!!) {
                            cachedSecureRandom!!.setSeed(seed)
                            seeded = true
                        }
                    } catch (e: Exception) {
                        // NoSuchAlgorithmException
                        warn("SecureRandom", e)
                    }
                }


            } catch (e: Exception) {
                // NoSuchAlgorithmException
                warn("SecureRandom", e)
                cachedSecureRandom = SecureRandom()
            }
            return field
        }

    /**
     * Generate a seed value, using as much unpredictable data as possible.
     * @return the seed
     */
    fun generateAlternativeSeed(): ByteArray {
        try {
            val bout: ByteArrayOutputStream = ByteArrayOutputStream()
            val out: DataOutputStream = DataOutputStream(bout)

            //milliseconds and nanoseconds
            out.writeLong(System.currentTimeMillis())
            out.writeLong(System.nanoTime())

            // memory
            out.writeInt(Object().hashCode())
            val rt: Runtime = Runtime.getRuntime()
            out.writeLong(rt.freeMemory())
            out.writeLong(rt.maxMemory())
            out.writeLong(rt.totalMemory())

            // environment
            try {
                val s: String = System.getProperties().toString()
                // can't use writeUTF, as the string
                // might be larger than 64KB
                out.writeInt(s.length)
                out.write(s.toByteArray(StandardCharsets.UTF_8))
            } catch (e: Exception) {
                warn("generateAlternativeSeed", e)
            }

            // hostname and ip addresses (if any)
            try {
                // workaround for the Google App Engine: don't use InetAddress
                val inetAddrClz: Class<*> = Class.forName("java.net.InetAddress")
                val localhost: Any = inetAddrClz.getMethod("getLocalHost").invoke(null)
                val hostname: String = inetAddrClz.getMethod("getHostName").invoke(localhost).toString()
                out.writeUTF(hostname)

                val list: Array<Any> = inetAddrClz.getMethod("getAllByName", String.javaClass).invoke(null, hostname) as Array<Any>
                val addrGetter: Method = inetAddrClz.getMethod("getAddress")

                list.forEach { out.write(addrGetter.invoke(it) as ByteArray) }
            } catch (e: Throwable) {
                warn("generateAlternativeSeed", e)
            }

            // timing (a second thread is already running usually)
            for (j in 0..15) {
                var i: Int = 0
                val end: Long = System.currentTimeMillis()
                while (end == System.currentTimeMillis()) i++
                out.writeInt(i)
            }

            out.close()
            return bout.toByteArray()
        } catch (e: IOException) {
            warn("generateAlternativeSeed", e)
            return ByteArray(1)
        }
    }
}