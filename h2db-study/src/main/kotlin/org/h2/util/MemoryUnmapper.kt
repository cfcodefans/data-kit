package org.h2.util

import org.h2.engine.SysProperties
import java.lang.reflect.Method
import java.nio.ByteBuffer

/**
 * Unsafe memory unmapper.
 *
 * @see SysProperties#NIO_CLEANER_HACK
 */
object MemoryUnmapper {
    private var ENABLED = false

    private var UNSAFE: Any? = null

    private var INVOKE_CLEANER: Method? = null

    init {
        var enabled = SysProperties.NIO_CLEANER_HACK
        var unsafe: Any? = null
        var invokeCleaner: Method? = null

        if (enabled) {
            try {
                val clazz: Class<*> = Class.forName("sun.misc.Unsafe")
                unsafe = clazz.getDeclaredField("theUnsafe")
                    .also { it.isAccessible = true }
                    .let { it[null] }

                // This method exists only on Java 9 and later versions
                invokeCleaner = clazz.getMethod("invokeCleaner", ByteBuffer::class.java)
            } catch (e: ReflectiveOperationException) {
                // Java 8
                unsafe = null
                // invokeCleaner can be only null here
            } catch (e: Throwable) {
                // Should be a SecurityException, but catch everything to be safe
                enabled = false
                unsafe = null
                // invokeCleaner can be only null here
            }
        }
        ENABLED = enabled
        UNSAFE = unsafe
        INVOKE_CLEANER = invokeCleaner
    }

    /**
     * Tries to unmap memory for the specified byte buffer using Java internals
     * in unsafe way if {@link SysProperties#NIO_CLEANER_HACK} is enabled and
     * access is not denied by a security manager.
     *
     * @param buffer
     *            mapped byte buffer
     * @return whether operation was successful
     */
    fun unmap(buffer: ByteBuffer): Boolean {
        if (!ENABLED) return false

        return try {
            // Java 9 or later
            if (INVOKE_CLEANER != null) {
                INVOKE_CLEANER!!.invoke(UNSAFE, buffer)
                return true
            }

            // Java 8
            buffer.javaClass
                .getMethod("cleaner")
                .let { md ->
                    md.isAccessible = true
                    md.invoke(buffer)
                }?.let { cleaner -> cleaner.javaClass.getMethod("clean").invoke(cleaner) }

            true
        } catch (e: Throwable) {
            false
        }
    }
}