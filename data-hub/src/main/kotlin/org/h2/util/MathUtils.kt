package org.h2.util

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.lang.reflect.Method
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.concurrent.ThreadLocalRandom

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
    var secureRandom: SecureRandom? = null
        private set
        @Synchronized
        get() {
            if (field != null) return field
            try {
                field = SecureRandom.getInstance("SHA1PRNG")
                // On some systems, secureRandom generateSeed() is very slow.
                // In this case it is initialized using our own seed implementation
                // and afterwards (in the thread) using the regular algorithm
                val runnable: Runnable = Runnable {
                    try {
                        val sr: SecureRandom = SecureRandom.getInstance("SHA1PRNG")
                        val seed: ByteArray = sr.generateSeed(20)
                        synchronized(field!!) {
                            field!!.setSeed(seed)
                            seeded = true
                        }
                    } catch (e: Exception) {
                        // NoSuchAlgorithmException
                        warn("SecureRandom", e)
                    }
                }

                try {
                    val t: Thread = Thread(runnable, "Generate Seed")
                    // let the process terminate even if generating the seed is really slow
                    t.isDaemon = true
                    t.start()
                    Thread.yield()
                    try {
                        // normally, generateSeed takes less than 200 ms
                        t.join(400)
                    } catch (e: InterruptedException) {
                        warn("InterruptedException", e)
                    }
                    if (!seeded) {
                        val seed: ByteArray = generateAlternativeSeed()
                        // this never reduces randomness
                        synchronized(field!!) {
                            field!!.setSeed(seed)
                        }
                    }
                } catch (e: SecurityException) {
                    // workaround for the Google App Engine: don't use a thread
                    runnable.run()
                    generateAlternativeSeed()
                }

            } catch (e: Exception) {
                // NoSuchAlgorithmException
                warn("SecureRandom", e)
                field = SecureRandom()
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

    /**
     * Get the value that is equal to or higher than this value, and that is a
     * power of two.
     * @param x the original value
     * @return the next power of two value
     * @throws IllegalArgumentException if x < 0 or x > 0x400000000
     */
    @JvmStatic
    @Throws(IllegalArgumentException::class)
    fun nextPowerOf2(x: Int): Int {
        if (x == 0) return 1
        if (x < 0 || x > 0x4000_0000) {
            throw java.lang.IllegalArgumentException("Argument out of range [0x0-0x40000000]. Argument was: $x")
        }
        var x1: Int = x - 1
        x1 = x1 or x1 shr 1
        x1 = x1 or x1 shr 2
        x1 = x1 or x1 shr 4
        x1 = x1 or x1 shr 8
        x1 = x1 or x1 shr 16
        return x1 + 1
    }

    /**
     * Convert a long value to an int value. Values larger than the biggest int
     * value is converted to the biggest int value, and values smaller than the
     * smallest int value are converted to the smallest int value.
     * @param l the value to convert
     * @return then converted int value
     */
    @JvmStatic
    fun convertLongToInt(l: Long): Int {
        if (l <= Int.MIN_VALUE) return Int.MIN_VALUE
        if (l >= Int.MAX_VALUE) return Int.MAX_VALUE
        return l.toInt()
    }

    /**
     * get a cryptographically secure pseudo random long value.
     * @return the random long value
     */
    @JvmStatic
    fun secureRandomLong(): Long = secureRandom!!.nextLong()

    /**
     * Get a number of pseudo random bytes.
     * @param bytes the target array
     */
    @JvmStatic
    fun randomBytes(bytes: ByteArray): Unit {
        ThreadLocalRandom.current().nextBytes(bytes)
    }

    /**
     * Get a number of cryptographically secure pseudo random bytes.
     * @param len the number of bytes
     * @return the random bytes
     */
    @JvmStatic
    fun secureRandomBytes(len: Int): ByteArray {
        val _len: Int = if (len <= 0) 1 else len
        val buff: ByteArray = ByteArray(_len)
        secureRandom!!.nextBytes(buff)
        return buff
    }

    /**
     * Get a pseudo random int value between 0 (including and the given value
     * (excluding). The value is not cryptographically secure.
     * @param lowerThan the value returned will be lower than this value
     * @return the random long value
     */
    @JvmStatic
    fun randomInt(lowerThan: Int): Int = ThreadLocalRandom.current().nextInt(lowerThan)

    /**
     * Get a cryptographically secure pseudo random int value between 0
     * (including and the given value (excluding).
     * @param lowerThan the value returned will be lower than this value
     * @return the random long value
     */
    @JvmStatic
    fun secureRandomInt(lowerThan: Int): Int = secureRandom!!.nextInt(lowerThan)
}