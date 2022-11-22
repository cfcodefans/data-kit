package org.h2.engine

import org.h2.api.ErrorCode
import org.h2.command.dml.SetTypes
import org.h2.message.DbException
import org.h2.store.fs.FileUtils
import org.h2.util.IOUtils
import org.h2.util.NetworkConnectionInfo
import org.h2.util.SortedProperties
import org.h2.util.StringUtils
import org.h2.util.TimeZoneProvider
import org.h2.util.Utils
import java.io.IOException
import java.util.Properties

/**
 * Encapsulates the connection settings, including username and password.
 */
class ConnectionInfo(private var prop: Properties = Properties(),
                     private var originalURL: String? = null,
                     var url: String? = null,
                     private var user: String? = null,
                     var filePasswordHash: ByteArray? = null,
                     var fileEncryptionKey: ByteArray? = null,
                     private var userPasswordHash: ByteArray? = null,

                     private val timeZone: TimeZoneProvider? = null,
                     /**,
                      * The database name,
                      */
                     private var name: String? = null,
                     private var nameNormalized: String? = null,
                     private var remote: Boolean = false,
                     private var ssl: Boolean = false,
                     var persistent: Boolean = false,
                     private var unnamed: Boolean = false,
                     private val networkConnectionInfo: NetworkConnectionInfo? = null) : Cloneable {

    companion object {

        private val KNOWN_SETTINGS: HashSet<String> = HashSet<String>(SetTypes.TYPES)
        private val IGNORED_BY_PARSER: HashSet<String> = HashSet<String>(32)

        init {
            val commonSettings = arrayOf("ACCESS_MODE_DATA", "AUTO_RECONNECT", "AUTO_SERVER", "AUTO_SERVER_PORT", //
                "CACHE_TYPE", //
                "FILE_LOCK", //
                "JMX", //
                "NETWORK_TIMEOUT", //
                "OLD_INFORMATION_SCHEMA", "OPEN_NEW", //
                "PAGE_SIZE", //
                "RECOVER")
            commonSettings.forEach { s -> if (!KNOWN_SETTINGS.add(s)) throw DbException.getInternalError(s) }

            val settings = arrayOf( //
                "AUTHREALM", "AUTHZPWD", "AUTOCOMMIT", //
                "CIPHER", "CREATE", //
                "FORBID_CREATION", //
                "IGNORE_UNKNOWN_SETTINGS", "IFEXISTS", "INIT", //
                "NO_UPGRADE", //
                "PASSWORD", "PASSWORD_HASH", //
                "RECOVER_TEST", //
                "USER")

            settings.forEach { s -> if (!KNOWN_SETTINGS.add(s)) throw DbException.getInternalError(s) }

            IGNORED_BY_PARSER.addAll(commonSettings)
            IGNORED_BY_PARSER.addAll(settings)
        }

        private fun isKnownSetting(s: String): Boolean = KNOWN_SETTINGS.contains(s)

        /**
         * Returns whether setting with the specified name should be ignored by parser.
         * @param name the name of the setting
         * @return whether setting with the specified name should be ignored by parser
         */
        fun isIgnoredByParser(name: String?): Boolean = IGNORED_BY_PARSER.contains(name)

        private fun remapURL(url: String): String {
            val urlMap: String? = SysProperties.URL_MAP
            if (urlMap.isNullOrEmpty()) return url

            try {
                val prop: SortedProperties = SortedProperties.loadProperties(urlMap)
                var url2: String? = prop.getProperty(url)
                if (url2 == null) {
                    prop[url] = ""
                    prop.store(urlMap)
                } else {
                    url2 = url2.trim { it <= ' ' }
                    if (url2.isNotEmpty()) return url2
                }
            } catch (e: IOException) {
                throw DbException.convert(e)
            }
            return url
        }
    }

    /**
     * Create a connection info object.
     *
     * @param name the database name (including tags), but without the "jdbc:h2:" prefix
     */
    constructor(name: String?) : this(name = name, url = Constants.START_URL + name) {
        parseName()
    }

    /**
     * Create a connection info object.
     *
     * @param u the database URL (must start with jdbc:h2:)
     * @param info the connection properties or {@code null}
     * @param user the user name or {@code null}
     * @param password the password as {@code String} or {@code char[]}, or {@code null}
     */
    constructor(u: String, info: Properties?, user: String?, password: Any?) : this() {
        remapURL(u).also {
            originalURL = it
            url = it
        }

        if (!url!!.startsWith(Constants.START_URL)) throw getFormatException()
        info?.let { readProperties(it) }
        user?.let { prop.put("USER", user) }
    }


    private fun parseName() {
        if ("." == name) name = "mem:"

        if (name!!.startsWith("tcp:")) {
            remote = true
            name = name!!.substring("tcp:".length)
        } else if (name!!.startsWith("ssl:")) {
            remote = true
            ssl = true
            name = name!!.substring("ssl:".length)
        } else if (name!!.startsWith("mem:")) {
            persistent = false
            if ("mem:" == name) unnamed = true
        } else if (name!!.startsWith("file:")) {
            name = name!!.substring("file:".length)
            persistent = true
        } else {
            persistent = true
        }
        if (persistent && !remote) {
            name = IOUtils.nameSeparatorsToNative(name!!)
        }
    }

    /**
     * Generate a URL format exception.
     * @return the exception
     */
    private fun getFormatException(): DbException {
        return DbException.get(ErrorCode.URL_FORMAT_ERROR_2, Constants.URL_FORMAT, url!!)
    }

    private fun readProperties(info: Properties) {
        val list = info.keys.toTypedArray()
        var s: DbSettings? = null
        for (k in list) {
            val key = StringUtils.toUpperEnglish(k.toString())
            if (prop.containsKey(key)) throw DbException.get(ErrorCode.DUPLICATE_PROPERTY_1, key)

            val value = info[k]
            if (isKnownSetting(key)) {
                prop[key] = value
            } else {
                if (s == null) s = getDbSettings()
                if (s.containsKey(key)) prop[key] = value
            }
        }
    }

    fun getDbSettings(): DbSettings {
        val defaultSettings = DbSettings.DEFAULT

        return prop.entries.mapNotNull { en ->
            val key = en.key.toString()
            if (!isKnownSetting(key) && defaultSettings.containsKey(key)) key to prop.getProperty(key) else null
        }.toMap()
            .let { DbSettings.getInstance(HashMap(it)) }
    }

    /**
     * Remove a String property if it is set and return the value.
     *
     * @param key the property name
     * @param defaultValue the default value
     * @return the value
     */
    fun removeProperty(key: String?, defaultValue: String?): String? {
        if (SysProperties.CHECK && !isKnownSetting(key!!)) throw DbException.getInternalError(key)
        val x = prop.remove(key)
        return x?.toString() ?: defaultValue
    }

    /**
     * Remove a boolean property if it is set and return the value.
     *
     * @param key the property name
     * @param defaultValue the default value
     * @return the value
     */
    fun removeProperty(key: String?, defaultValue: Boolean): Boolean {
        return Utils.parseBoolean(removeProperty(key, null), defaultValue, false)
    }

    /**
     * Clear authentication properties.
     */
    fun cleanAuthenticationInfo() {
        removeProperty("AUTHREALM", false)
        removeProperty("AUTHZPWD", false)
    }

    /**
     * Overwrite a property.
     *
     * @param key the property name
     * @param value the value
     */
    fun setProperty(key: String?, value: String?) {
        // value is null if the value is an object
        if (value != null) prop.setProperty(key, value)
    }

    /**
     * Switch to server mode, and set the server name and database key.
     *
     * @param serverKey the server name, '/', and the security key
     */
    fun setServerKey(serverKey: String?) {
        remote = true
        persistent = false
        name = serverKey
    }

    /**
     * Get the unique and normalized database name (excluding settings).
     *
     * @return the database name
     */
    fun getName(): String? {
        if (!persistent) return name

        if (nameNormalized == null) {
            if (!FileUtils.isAbsolute(name!!)
                && !name!!.contains("./")
                && !name!!.contains(".\\")
                && !name!!.contains(":/")
                && !name!!.contains(":\\")) {
                // the name could start with "./", or
                // it could start with a prefix such as "nioMapped:./"
                // for Windows, the path "\test" is not considered
                // absolute as the drive letter is missing,
                // but we consider it absolute
                throw DbException.get(ErrorCode.URL_RELATIVE_TO_CWD, originalURL!!)
            }
            val suffix = Constants.SUFFIX_MV_FILE
            val n = FileUtils.toRealPath(name + suffix)
            val fileName: String = FileUtils.getName(n)
            if (fileName.length < suffix.length + 1) {
                throw DbException.get(ErrorCode.INVALID_DATABASE_NAME_1, name!!)
            }
            nameNormalized = n.substring(0, n.length - suffix.length)
        }

        return nameNormalized
    }

    /**
     * Get the value of the given property.
     *
     * @param key the property key
     * @return the value as a String
     */
    fun getProperty(key: String?): String? = (prop[key] as? String)?.toString()

    /**
     * Get the value of the given property.
     *
     * @param key the property key
     * @param defaultValue the default value
     * @return the value as a String
     */
    fun getProperty(key: String?, defaultValue: String?): String? {
        if (SysProperties.CHECK && !isKnownSetting(key!!)) {
            throw DbException.getInternalError(key)
        }
        return getProperty(key) ?: defaultValue
    }

    /**
     * Get a boolean property if it is set and return the value.
     *
     * @param key the property name
     * @param defaultValue the default value
     * @return the value
     */
    fun getProperty(key: String?, defaultValue: Boolean): Boolean {
        return Utils.parseBoolean(getProperty(key, null), defaultValue, false)
    }

    /**
     * Get the value of the given property.
     *
     * @param key the property key
     * @param defaultValue the default value
     * @return the value as a String
     */
    fun getProperty(key: String?, defaultValue: Int): Int {
        if (SysProperties.CHECK && !isKnownSetting(key!!)) {
            throw DbException.getInternalError(key)
        }
        return getProperty(key)?.toInt() ?: defaultValue
    }

    /**
     * Get the value of the given property.
     *
     * @param setting the setting id
     * @param defaultValue the default value
     * @return the value as an integer
     */
    fun getIntProperty(setting: Int, defaultValue: Int): Int {
        return try {
            getProperty(SetTypes.getTypeName(setting), null)?.toInt() ?: defaultValue
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }
}