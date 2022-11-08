package org.h2.engine

import jdk.internal.org.jline.utils.Colors.s
import org.h2.api.ErrorCode
import org.h2.message.DbException
import org.h2.util.Utils

/**
 * The base class for settings.
 */
open class SettingsBase(val settings: HashMap<String, String>) {

    /**
     * Get the setting for the given key.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the setting
     */
    protected open operator fun get(key: String, defaultValue: String?): String {
        var v = settings[key]
        if (v != null) return v

        val buff = StringBuilder("h2.")
        var lastChar: Char = key[0]; for (c in key) {
            buff.append(if (lastChar == '_') c.uppercaseChar() else c.lowercaseChar())
            lastChar = c
        }

        v = Utils.getProperty(buff.toString(), defaultValue)
        settings[key] = v!!
        return v
    }

    /**
     * Get the setting for the given key.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the setting
     */
    protected open operator fun get(key: String, defaultValue: Int): Int = try {
        Integer.decode(get(key, defaultValue.toString()))
    } catch (e: NumberFormatException) {
        throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, e, "key:$key value:$s")
    }

    /**
     * Get the setting for the given key.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the setting
     */
    protected open operator fun get(key: String, defaultValue: Boolean): Boolean = try {
        Utils.parseBoolean(get(key, defaultValue.toString()), defaultValue, true)
    } catch (e: IllegalArgumentException) {
        throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, e, "key:$key value:$s")
    }

    /**
     * Set an entry in the key-value pair.
     *
     * @param key the key
     * @param value the value
     */
    open operator fun set(key: String, value: Boolean) {
        settings[key] = value.toString()
    }

    /**
     * Check if the settings contains the given key.
     * @param k the key
     * @return true if they do
     */
    protected open fun containsKey(k: String): Boolean = settings.containsKey(k)

    /**
     * Get all settings in alphabetical order.
     * @return the settings
     */
    open fun getSortedSettings(): Array<Map.Entry<String, String>> = settings.entries.sortedBy { en -> en.key }.toTypedArray()
}