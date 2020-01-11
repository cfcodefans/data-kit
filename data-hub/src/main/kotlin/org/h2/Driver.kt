package org.h2

import org.h2.engine.Constants
import java.sql.Connection
import java.sql.DriverManager
import java.sql.DriverPropertyInfo
import java.sql.SQLException
import java.util.*
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
        fun load():Driver {
            try {
                if (!registered) {
                    registered = true
                    DriverManager.registerDriver(INSTANCE)
                }
            } catch (e:SQLException) {

            }
            return INSTANCE
        }

        init {
            load()
        }
    }

    /**
     * Get the minor version number of the driver.
     * This method should not be called by an application
     *
     * @return the minor version number
     */
    override fun getMinorVersion(): Int = Constants.VERSION_MINOR

    override fun getParentLogger(): Logger {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getPropertyInfo(p0: String?, p1: Properties?): Array<DriverPropertyInfo> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun jdbcCompliant(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Check if the driver understands this URL.
     * This method should not be called by an application
     *
     * @param url the database URL
     * @return if the driver understands the URL
     */
    override fun acceptsURL(url: String?): Boolean {
        url ?: return false
        return url.startsWith(Constants.)
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