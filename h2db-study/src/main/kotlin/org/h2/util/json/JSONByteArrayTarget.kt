package org.h2.util.json

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * JSON byte array target.
 */
class JSONByteArrayTarget : JSONTarget<ByteArray> {
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

}