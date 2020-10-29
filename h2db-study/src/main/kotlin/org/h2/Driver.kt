package org.h2

import org.h2.engine.Constants
import org.h2.message.DbException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.DriverPropertyInfo
import java.sql.SQLException
import java.util.Properties
import java.util.logging.Logger

class Driver : java.sql.Driver {
    companion object {
        @JvmStatic
        val DEFAULT_URL: String = "jdbc:default:connection"

        @JvmStatic
        val INSTANCE: Driver = Driver()

        @JvmStatic
        val DEFAULT_CONNECTION: ThreadLocal<Connection> = ThreadLocal()

        @JvmStatic
        var registered: Boolean = false

        @JvmStatic
        @Synchronized
        fun load(): Driver {
            if (registered) return INSTANCE
            try {
                DriverManager.registerDriver(INSTANCE)
                registered = true
            } catch (e: SQLException) {
                DbException.traceThrowable(e)
            }
            return INSTANCE
        }

        init {
            load()
        }

        @JvmStatic
        @Synchronized
        fun unload(): Unit {
            if (!registered) return
            try {
                registered = false
                DriverManager.deregisterDriver(INSTANCE)
            } catch (e: SQLException) {
                DbException.traceThrowable(e)
            }
        }

        /**
         * INTERNAL
         * Sets, on a pre-thread basis, the default-connection for
         * user-defined functions
         */
        @JvmStatic
        fun setDefaultConnection(c: Connection?): Unit {
            if (c == null) {
                DEFAULT_CONNECTION.remove()
            } else {
                DEFAULT_CONNECTION.set(c)
            }
        }

        /**
         * INTERNAL
         */
        @JvmStatic
        fun setThreadContextClassLoader(thread: Thread) {
            // Apache Tomcat: use the classloader of the driver to avoid the
            // following log message:
            // org.apache.catalina.loader.WebappClassLoader clearReferencesThreads
            // SEVERE: The web application appears to have started a thread named
            // ... but has failed to stop it.
            // This is very likely to create a memory leak.
            try {
                thread.contextClassLoader = Driver::class.java.classLoader
            } catch (t: Throwable) {
            }
        }
    }

    /**
     * Get the minor version number of the driver.
     * This method should not be called by an application
     *
     * @return the minor version number
     */
    override fun getMinorVersion(): Int = Constants.VERSION_MINOR

    /**
     * [Not supported]
     */
    override fun getParentLogger(): Logger? = null

    /**
     * Get the list of supported properties.
     * This method should not be called by an application.
     *
     * @param url the database URL
     * @param info the connection properties
     * @return a zero length array
     */
    override fun getPropertyInfo(p0: String?, p1: Properties?): Array<DriverPropertyInfo> = emptyArray()

    /**
     * Check if this driver is compliant to the JDBC specification.
     * This method should not be called by an application.
     *
     * @return true
     */
    override fun jdbcCompliant(): Boolean = true

    /**
     * Check if the driver understands this URL.
     * This method should not be called by an application
     *
     * @param url the database URL
     * @return if the driver understands the URL
     */
    override fun acceptsURL(url: String?): Boolean {
        url ?: return false
        return url.startsWith(Constants.START_URL)
                || (DEFAULT_URL == url && DEFAULT_CONNECTION.get() != null)
    }

    override fun connect(p0: String?, p1: Properties?): Connection {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Get the major version number of the driver.
     * This method should not be called by an application.
     *
     * @return the major version number
     */
    override fun getMajorVersion(): Int = Constants.VERSION_MAJOR
}