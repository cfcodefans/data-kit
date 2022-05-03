package org.h2.util.json

import org.h2.util.ByteStack
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.nio.charset.StandardCharsets

/**
 * JSON byte array target.
 */
class JSONByteArrayTarget : JSONTarget<ByteArray>() {
    companion object {
        private val NULL_BYTES = "null".toByteArray(StandardCharsets.ISO_8859_1)

        private val FALSE_BYTES = "false".toByteArray(StandardCharsets.ISO_8859_1)

        private val TRUE_BYTES = "true".toByteArray(StandardCharsets.ISO_8859_1)

        private val U00_BYTES = "\\u00".toByteArray(StandardCharsets.ISO_8859_1)

        /**
         * Encodes a JSON string and appends it to the specified output stream.
         *
         * @param baos
         * the output stream to append to
         * @param s
         * the string to encode
         * @return the specified output stream
         */
        fun encodeString(baos: ByteArrayOutputStream, s: String): ByteArrayOutputStream? {
            baos.write('"'.code)
            var i = 0
            val length = s.length
            while (i < length) {
                val c = s[i]
                when (c) {
                    '\b' -> {
                        baos.write('\\'.code)
                        baos.write('b'.code)
                    }
                    '\t' -> {
                        baos.write('\\'.code)
                        baos.write('t'.code)
                    }
                    '\u000c' -> {
                        baos.write('\\'.code)
                        baos.write('f'.code)
                    }
                    '\n' -> {
                        baos.write('\\'.code)
                        baos.write('n'.code)
                    }
                    '\r' -> {
                        baos.write('\\'.code)
                        baos.write('r'.code)
                    }
                    '"' -> {
                        baos.write('\\'.code)
                        baos.write('"'.code)
                    }
                    '\\' -> {
                        baos.write('\\'.code)
                        baos.write('\\'.code)
                    }
                    else -> if (c >= ' ') {
                        if (c.code < 0x80) {
                            baos.write(c.code)
                        } else if (c.code < 0x800) {
                            baos.write(0xc0 or c.code shr 6)
                            baos.write(0x80 or c.code and 0x3f)
                        } else if (!Character.isSurrogate(c)) {
                            baos.write(0xe0 or c.code shr 12)
                            baos.write(0x80 or c.code shr 6 and 0x3f)
                            baos.write(0x80 or c.code and 0x3f)
                        } else {
                            var c2: Char = 0.toChar()
                            require(!(!Character.isHighSurrogate(c)
                                    || ++i >= length
                                    || !Character.isLowSurrogate(s[i].also { c2 = it })))
                            val uc = Character.toCodePoint(c, c2)
                            baos.write(0xf0 or uc shr 18)
                            baos.write(0x80 or uc shr 12 and 0x3f)
                            baos.write(0x80 or uc shr 6 and 0x3f)
                            baos.write(0x80 or uc and 0x3f)
                        }
                    } else {
                        baos.write(U00_BYTES, 0, 4)
                        baos.write(JSONStringTarget.HEX[c.code ushr 4 and 0xf].code)
                        baos.write(JSONStringTarget.HEX[c.code and 0xf].code)
                    }
                }
                i++
            }
            baos.write('"'.code)
            return baos
        }
    }

    private val baos: ByteArrayOutputStream = ByteArrayOutputStream()

    private val stack: ByteStack = ByteStack()

    private var needSeparator: Boolean = false

    private var afterName: Boolean = false

    private fun beforeValue() {
        check((!afterName && stack.peek(-1) == JSONStringTarget.OBJECT.toInt()).not())
        if (needSeparator) {
            check(stack.isEmpty().not())
            needSeparator = false
            baos.write(','.code)
        }
    }

    private fun afterValue() {
        needSeparator = true
        afterName = false
    }

    override fun startObject() = apply {
        beforeValue()
        afterName = false
        stack.push(JSONStringTarget.OBJECT)
        baos.write('{'.code)
    }

    override fun endObject() = apply {
        check(!(afterName || stack.poll(-1) != JSONStringTarget.OBJECT.toInt()))
        baos.write('}'.code)
        afterValue()
    }

    override fun startArray() = apply {
        beforeValue()
        afterName = false
        stack.push(JSONStringTarget.ARRAY)
        baos.write('['.code)
    }

    override fun endArray() = apply {
        check(stack.poll(-1) == JSONStringTarget.ARRAY.toInt())
        baos.write(']'.code)
        afterValue()
    }

    override fun member(name: String?) = apply {
        check(!(afterName || stack.peek(-1) != JSONStringTarget.OBJECT.toInt()))
        afterName = true
        beforeValue()
        encodeString(baos, name!!)!!.write(':'.code)
    }

    override fun valueNull() = apply {
        beforeValue()
        baos.write(NULL_BYTES, 0, 4)
        afterValue()
    }

    override fun valueFalse() = apply {
        beforeValue()
        baos.write(FALSE_BYTES, 0, 5)
        afterValue()
    }

    override fun valueTrue() = apply {
        beforeValue()
        baos.write(TRUE_BYTES, 0, 4)
        afterValue()
    }

    override fun valueNumber(number: BigDecimal?) = apply {
        beforeValue()
        val s = number.toString()
        var index = s.indexOf('E')
        val b = s.toByteArray(StandardCharsets.ISO_8859_1)
        if (index >= 0 && s[++index] == '+') {
            baos.write(b, 0, index)
            baos.write(b, index + 1, b.size - index - 1)
        } else {
            baos.write(b, 0, b.size)
        }
        afterValue()
    }

    override fun valueString(string: String?) = apply {
        beforeValue()
        encodeString(baos, string!!)
        afterValue()
    }

    override fun isPropertyExpected(): Boolean {
        return !afterName && stack.peek(-1) == JSONStringTarget.OBJECT.toInt()
    }

    override fun isValueSeparatorExpected(): Boolean = needSeparator

    override fun getResult(): ByteArray? {
        check(stack.isEmpty() && baos.size() != 0)
        return baos.toByteArray()
    }
}