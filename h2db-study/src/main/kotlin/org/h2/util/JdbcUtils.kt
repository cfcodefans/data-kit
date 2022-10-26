package org.h2.util

import org.h2.api.CustomDataTypesHandler
import org.h2.api.ErrorCode
import org.h2.api.JavaObjectSerializer
import org.h2.engine.SysProperties
import org.h2.message.DbException
import org.h2.util.Bits.readLong
import org.h2.value.ValueUuid
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass
import java.nio.charset.StandardCharsets
import java.sql.Connection
import java.sql.Driver
import java.sql.DriverManager
import java.sql.SQLException
import java.util.Properties
import javax.naming.Context
import javax.sql.DataSource

/**
 * This is a utility class with JDBC helper functions.
 */
object JdbcUtils {
    /**
     * The serializer to use.
     */
    var serializer: JavaObjectSerializer? = null

    /**
     * Custom data types handler to use.
     */
    var customDataTypesHandler: CustomDataTypesHandler? = null

    init {
        serializer = SysProperties.JAVA_OBJECT_SERIALIZER?.let {
            try {
                (loadUserClass<Any>(it).getDeclaredConstructor().newInstance() as JavaObjectSerializer)
            } catch (e: Exception) {
                throw DbException.convert(e)
            }
        }
        customDataTypesHandler = SysProperties.CUSTOM_DATA_TYPES_HANDLER?.let {
            try {
                loadUserClass<Any>(it).getDeclaredConstructor().newInstance() as CustomDataTypesHandler
            } catch (e: java.lang.Exception) {
                throw DbException.convert(e)
            }
        }
    }

    private val DRIVERS = arrayOf(
        "h2:", "org.h2.Driver",
        "Cache:", "com.intersys.jdbc.CacheDriver",
        "daffodilDB://", "in.co.daffodil.db.rmi.RmiDaffodilDBDriver",
        "daffodil", "in.co.daffodil.db.jdbc.DaffodilDBDriver",
        "db2:", "com.ibm.db2.jcc.DB2Driver",
        "derby:net:", "org.apache.derby.jdbc.ClientDriver",
        "derby://", "org.apache.derby.jdbc.ClientDriver",
        "derby:", "org.apache.derby.jdbc.EmbeddedDriver",
        "FrontBase:", "com.frontbase.jdbc.FBJDriver",
        "firebirdsql:", "org.firebirdsql.jdbc.FBDriver",
        "hsqldb:", "org.hsqldb.jdbcDriver",
        "informix-sqli:", "com.informix.jdbc.IfxDriver",
        "jtds:", "net.sourceforge.jtds.jdbc.Driver",
        "microsoft:", "com.microsoft.jdbc.sqlserver.SQLServerDriver",
        "mimer:", "com.mimer.jdbc.Driver",
        "mysql:", "com.mysql.jdbc.Driver",
        "odbc:", "sun.jdbc.odbc.JdbcOdbcDriver",
        "oracle:", "oracle.jdbc.driver.OracleDriver",
        "pervasive:", "com.pervasive.jdbc.v2.Driver",
        "pointbase:micro:", "com.pointbase.me.jdbc.jdbcDriver",
        "pointbase:", "com.pointbase.jdbc.jdbcUniversalDriver",
        "postgresql:", "org.postgresql.Driver",
        "sybase:", "com.sybase.jdbc3.jdbc.SybDriver",
        "sqlserver:", "com.microsoft.sqlserver.jdbc.SQLServerDriver",
        "teradata:", "com.ncr.teradata.TeraDriver")

    private val UUID_PREFIX: ByteArray = "\u00ac\u00ed\u0000\u0005sr\u0000\u000ejava.util.UUID\u00bc\u0099\u0003\u00f7\u0098m\u0085/\u0002\u0000\u0002J\u0000\u000cleastSigBitsJ\u0000\u000bmostSigBitsxp"
        .toByteArray(StandardCharsets.ISO_8859_1)

    /**
     * Get the driver class name for the given URL, or null if the URL is
     * unknown.
     *
     * @param url the database URL
     * @return the driver class name
     */
    fun getDriver(url: String): String? {
        if (!url.startsWith("jdbc:")) return null
        var url = url.substring("jdbc:".length)
        for (i in 0 until DRIVERS.size step 2) {
            if (url.startsWith(DRIVERS[i])) return DRIVERS[i + 1]
        }
        return null
    }

    /**
     * Load the driver class for the given URL, if the database URL is known.
     *
     * @param url the database URL
     */
    fun load(url: String) = getDriver(url)?.let { loadUserClass<Any>(it) }

    /**
     * Serialize the object to a byte array, using the serializer specified by
     * the connection info if set, or the default serializer.
     * @param obj the object to serialize
     * @param javaObjectSerializer provides the object serialize (may be null)
     * @return the byte array
     */
    @JvmStatic
    fun serialize(obj: Any, javaObjectSerializer: JavaObjectSerializer?): ByteArray {
        return try {
            (javaObjectSerializer ?: serializer)
                ?.serialize(obj)
                ?: kotlin.run {
                    val out = ByteArrayOutputStream()
                    ObjectOutputStream(out).writeObject(obj)
                    out.toByteArray()
                }
        } catch (e: Throwable) {
            throw DbException.get(ErrorCode.SERIALIZATION_FAILED_1, e, e.toString())
        }
    }

    /**
     * De-serialize the byte array to an object, eventually using the serializer
     * specified by the connection info.
     * @param data the byte array
     * @param javaObjectSerializer provides the object serializer (may be null)
     * @return the object
     * @throws DbException if serialization fails
     */
    @JvmStatic
    fun deserialize(data: ByteArray, javaObjectSerializer: JavaObjectSerializer?): Any? {
        return try {
            (javaObjectSerializer ?: serializer)
                ?.deserialize(data)
                ?: kotlin.run {
                    val bain = ByteArrayInputStream(data)
                    val ois = if (SysProperties.USE_THREAD_CONTEXT_CLASS_LOADER)
                        object : ObjectInputStream(bain) {
                            @Throws(IOException::class, ClassNotFoundException::class)
                            override fun resolveClass(desc: ObjectStreamClass): Class<*>? = try {
                                Class.forName(desc.name, true, Thread.currentThread().contextClassLoader)
                            } catch (e: ClassNotFoundException) {
                                super.resolveClass(desc)
                            }
                        }
                    else ObjectInputStream(bain)
                }
        } catch (e: Throwable) {
            throw DbException.get(ErrorCode.DESERIALIZATION_FAILED_1, e, e.toString());
        }
    }

    /**
     * De-serialize the byte array to a UUID object. This method is called on
     * the server side where regular de-serialization of user-supplied Java
     * objects may create a security hole if object was maliciously crafted.
     * Unlike [.deserialize], this method
     * does not try to de-serialize instances of other classes.
     *
     * @param data the byte array
     * @return the UUID object
     * @throws DbException if serialization fails
     */
    fun deserializeUuid(data: ByteArray): ValueUuid {
        if (data.size == 80) run {
            for (i in 0..63) {
                if (data[i] != JdbcUtils.UUID_PREFIX[i]) {
                    return@run
                }
            }
            return ValueUuid[readLong(data, 72), readLong(data, 64)]
        }
        throw DbException.get(ErrorCode.DESERIALIZATION_FAILED_1, "Is not a UUID")
    }

    private var allowAllClasses = false
    private var allowedClassNames: HashSet<String>? = null

    /**
     * In order to manage more than one class loader
     */
    private val userClassFactories: ArrayList<Utils.ClassFactory?> = ArrayList()

    private fun getUserClassFactories(): ArrayList<Utils.ClassFactory?> {
        return userClassFactories
    }

    /**
     * Add a class factory in order to manage more than one class loader.
     *
     * @param classFactory An object that implements ClassFactory
     */
    fun addClassFactory(classFactory: Utils.ClassFactory?) {
        getUserClassFactories().add(classFactory)
    }

    /**
     * Remove a class factory
     *
     * @param classFactory Already inserted class factory instance
     */
    fun removeClassFactory(classFactory: Utils.ClassFactory?) {
        getUserClassFactories().remove(classFactory)
    }

    private lateinit var allowedClassNamePrefixes: Array<String>

    /**
     * Load a class, but check if it is allowed to load this class first. To
     * perform access rights checking, the system property h2.allowedClasses
     * needs to be set to a list of class file name prefixes.
     *
     * @param className the name of the class
     * @return the class object
     */
    @SuppressWarnings("unchecked")
    @JvmStatic
    fun <Z> loadUserClass(className: String): Class<Z> {
        if (allowedClassNames == null) {
            val prefixes = ArrayList<String>()
            val classNames = HashSet<String>()
            for (p in StringUtils.arraySplit(SysProperties.ALLOWED_CLASSES, ',', true)) {
                when {
                    p == "*" -> allowAllClasses = true
                    p.endsWith("*") -> prefixes.add(p.take(p.length - 1))
                    else -> classNames.add(p)
                }
            }
            allowedClassNamePrefixes = prefixes.toTypedArray()
            allowedClassNames = classNames
        }

        if (!allowAllClasses
            && allowedClassNames?.contains(className) == false
            && !allowedClassNamePrefixes.any { className.startsWith(it) }) {
            throw DbException.get(ErrorCode.ACCESS_DENIED_TO_CLASS_1, className)
        }

        // Use provided class factory first.
        getUserClassFactories().firstOrNull { it?.match(className) ?: false }?.let { classFactory ->
            try {
                return classFactory.loadClass(className)!!.let { clz -> clz as Class<Z> }
            } catch (e: java.lang.Exception) {
                throw DbException.get(ErrorCode.CLASS_NOT_FOUND_1, e, className)
            }
        }
// Use local ClassLoader
        // Use local ClassLoader
        return try {
            (Class.forName(className) as Class<Z>)
        } catch (e: ClassNotFoundException) {
            try {
                (Class.forName(className, true, Thread.currentThread().contextClassLoader) as Class<Z>)
            } catch (e2: Exception) {
                throw DbException.get(ErrorCode.CLASS_NOT_FOUND_1, e, className)
            }
        } catch (e: NoClassDefFoundError) {
            throw DbException.get(ErrorCode.CLASS_NOT_FOUND_1, e, className)
        } catch (e: Error) {
            // UnsupportedClassVersionError
            throw DbException.get(ErrorCode.GENERAL_ERROR_1, e, className)
        }
    }

    /**
     * Close a statement without throwing an exception.
     * @param autoCloseable the statement or null
     */
    @JvmStatic
    fun closeSilently(autoCloseable: AutoCloseable?) = autoCloseable?.let {
        try {
            autoCloseable.close()
        } catch (e: SQLException) {            /* ignored */
        }
    }

    /**
     * Open a new database connection with the given settings.
     *
     * @param driver the driver class name
     * @param url the database URL
     * @param user the user name
     * @param password the password
     * @return the database connection
     */
    @Throws(SQLException::class)
    fun getConnection(driver: String?, url: String?,
                      user: String?, password: String?): Connection? {
        val prop = Properties()
        user?.let { prop.setProperty("user", it) }
        password?.let { prop.setProperty("password", it) }
        return getConnection(driver, url, prop, null)
    }

    /**
     * Open a new database connection with the given settings.
     *
     * @param driver the driver class name
     * @param url the database URL
     * @param prop the properties containing at least the user name and password
     * @param networkConnectionInfo the network connection information, or `null`
     * @return the database connection
     */
    @Throws(SQLException::class)
    fun getConnection(driver: String?, url: String?, prop: Properties?,
                      networkConnectionInfo: NetworkConnectionInfo?): Connection? {
        val connection = getConnection(driver, url!!, prop!!)
        //TODO
//        if (networkConnectionInfo != null && connection is JdbcConnection) {
//            connection.session.setNetworkConnectionInfo(networkConnectionInfo)
//        }
        return connection
    }

    @JvmStatic
    @Throws(SQLException::class)
    fun getConnection(driver: String?, url: String, prop: Properties): Connection? {
        if (driver.isNullOrBlank()) load(url)
        else {
            val d: Class<*> = loadUserClass<Any>(driver)
            try {
                if (Driver::class.java.isAssignableFrom(d)) {
                    val driverInstance = d.getDeclaredConstructor().newInstance() as Driver
                    /*
                     * fix issue #695 with drivers with the same jdbc
                     * subprotocol in classpath of jdbc drivers (as example
                     * redshift and postgresql drivers)
                     */
                    return driverInstance.connect(url, prop)
                        ?: throw SQLException("Driver $driver is not suitable for $url", "08001")
                } else if (Context::class.java.isAssignableFrom(d)) {
                    // JNDI context
                    val context = d.getDeclaredConstructor().newInstance() as Context
                    val ds = context.lookup(url) as DataSource
                    val user = prop.getProperty("user")
                    val password = prop.getProperty("password")
                    return if (user.isNullOrBlank() && password.isNullOrBlank()) {
                        ds.connection
                    } else ds.getConnection(user, password)
                }
            } catch (e: Exception) {
                throw DbException.toSQLException(e)!!
            }
        }
        return DriverManager.getConnection(url, prop)
    }
}