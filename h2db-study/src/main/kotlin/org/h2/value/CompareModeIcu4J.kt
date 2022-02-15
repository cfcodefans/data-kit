package org.h2.value

import org.h2.message.DbException
import org.h2.util.JdbcUtils
import org.h2.util.StringUtils
import java.text.Collator
import java.util.Locale

/**
 * An implementation of CompareMode that uses the ICU4J Collator.
 */
open class CompareModeIcu4J(name: String, strength: Int) : CompareMode(name, strength) {
    companion object {
        private fun getIcu4jCollator(name: String, strength: Int): Comparator<String?> = try {
            var result: Comparator<String?>? = null
            val collatorClass: Class<*> = JdbcUtils.loadUserClass<Any>("com.ibm.icu.text.Collator")
            val getInstanceMethod = collatorClass.getMethod("getInstance", Locale::class.java)

            val length = name.length
            if (length == 2) {
                val locale = Locale(StringUtils.toLowerEnglish(name), "")
                if (compareLocaleNames(locale, name)) {
                    result = getInstanceMethod.invoke(null, locale) as Comparator<String?>
                }
            } else if (length == 5) {
                // LL_CC (language_country)
                val idx = name.indexOf('_')
                if (idx >= 0) {
                    val language = StringUtils.toLowerEnglish(name.substring(0, idx))
                    val country = name.substring(idx + 1)
                    val locale = Locale(language, country)
                    if (compareLocaleNames(locale, name)) {
                        result = getInstanceMethod.invoke(null, locale) as Comparator<String?>
                    }
                }
            }
            if (result == null) {
                result = (collatorClass.getMethod("getAvailableLocales").invoke(null) as Array<Locale>)
                        .firstOrNull { locale -> compareLocaleNames(locale, name) }
                        ?.let { locale -> getInstanceMethod.invoke(null, locale) as Comparator<String?> }
                        ?: throw DbException.getInvalidValueException("collator", name)
            }

            collatorClass.getMethod("setStrength", Int::class.javaPrimitiveType).invoke(result, strength)
            result
        } catch (e: Exception) {
            throw DbException.convert(e)
        }
    }

    private var collator: Comparator<String?>? = null

    @Volatile
    private var caseInsensitive: CompareModeIcu4J? = null

    init {
        collator = CompareModeIcu4J.getIcu4jCollator(name, strength)
    }

    override fun equalsChars(a: String, ai: Int, b: String, bi: Int, ignoreCase: Boolean): Boolean {
        return compareString(a.substring(ai, ai + 1), b.substring(bi, bi + 1), ignoreCase) == 0
    }

    override fun compareString(a: String, b: String?, ignoreCase: Boolean): Int {
        if (ignoreCase && strength > Collator.SECONDARY) {
            var i = caseInsensitive
            if (i == null) {
                i = CompareModeIcu4J(name, Collator.SECONDARY)
                caseInsensitive = i
            }
            return i.compareString(a, b, false)
        }
        return collator!!.compare(a, b)
    }
}