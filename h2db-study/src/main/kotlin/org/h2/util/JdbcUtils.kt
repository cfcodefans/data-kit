package org.h2.util

import org.h2.api.CustomDataTypesHandler
import org.h2.api.ErrorCode
import org.h2.api.JavaObjectSerializer
import org.h2.engine.SysProperties
import org.h2.message.DbException
import org.h2.store.DataHandler
import java.sql.SQLException
import java.util.*

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

    /**
     *
     */
    @JvmStatic
    fun serialize(obj: Any, dataHandler: DataHandler?): ByteArray {
        try {
            dataHandler?.
        } catch (e: Throwable) {
            throw DbException.get(ErrorCode.SERIALIZATION_FAILED_1, e, e.toString())
        }
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
                && !allowedClassNames!!.contains(className)
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
    fun closeSilently(autoCloseable: AutoCloseable?) = autoCloseable?.let {
        try {
            autoCloseable.close()
        } catch (e: SQLException) {            /* ignored */
        }
    }
}