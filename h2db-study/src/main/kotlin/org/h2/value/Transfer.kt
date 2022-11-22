package org.h2.value

import org.h2.api.ErrorCode
import org.h2.engine.Constants
import org.h2.engine.Session
import org.h2.message.DbException
import org.h2.util.StringUtils
import org.h2.util.Utils
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket

/**
 * The transfer class is used to send and receive Value objects.
 * It is used on both the client side, and on the server side.
 */
class Transfer(private val session: Session?, private var socket: Socket?) : Closeable {
    companion object {
        private const val BUFFER_SIZE = 64 * 1024
        private const val LOB_MAGIC = 0x1234
        private const val LOB_MAC_SALT_LENGTH = 16

        private const val NULL = 0
        private const val BOOLEAN = 1
        private const val TINYINT = 2
        private const val SMALLINT = 3
        private const val INTEGER = 4
        private const val BIGINT = 5
        private const val NUMERIC = 6
        private const val DOUBLE = 7
        private const val REAL = 8
        private const val TIME = 9
        private const val DATE = 10
        private const val TIMESTAMP = 11
        private const val VARBINARY = 12
        private const val VARCHAR = 13
        private const val VARCHAR_IGNORECASE = 14
        private const val BLOB = 15
        private const val CLOB = 16
        private const val ARRAY = 17
        private const val JAVA_OBJECT = 19
        private const val UUID = 20
        private const val CHAR = 21
        private const val GEOMETRY = 22

        // 1.4.192
        private const val TIMESTAMP_TZ = 24

        // 1.4.195
        private const val ENUM = 25

        // 1.4.198
        private const val INTERVAL = 26
        private const val ROW = 27

        // 1.4.200
        private const val JSON = 28
        private const val TIME_TZ = 29

        // 2.0.202
        private const val BINARY = 30
        private const val DECFLOAT = 31

        private val VALUE_TO_TI = IntArray(Value.TYPE_COUNT + 1)
        private val TI_TO_VALUE = IntArray(45)

        private fun addType(typeInformationType: Int, valueType: Int) {
            VALUE_TO_TI[valueType + 1] = typeInformationType
            TI_TO_VALUE[typeInformationType + 1] = valueType
        }

        init {
            addType(-1, Value.UNKNOWN)
            addType(NULL, Value.NULL)
            addType(BOOLEAN, Value.BOOLEAN)
            addType(TINYINT, Value.TINYINT)
            addType(SMALLINT, Value.SMALLINT)
            addType(INTEGER, Value.INTEGER)
            addType(BIGINT, Value.BIGINT)
            addType(NUMERIC, Value.NUMERIC)
            addType(DOUBLE, Value.DOUBLE)
            addType(REAL, Value.REAL)
            addType(TIME, Value.TIME)
            addType(DATE, Value.DATE)
            addType(TIMESTAMP, Value.TIMESTAMP)
            addType(VARBINARY, Value.VARBINARY)
            addType(VARCHAR, Value.VARCHAR)
            addType(VARCHAR_IGNORECASE, Value.VARCHAR_IGNORECASE)
            addType(BLOB, Value.BLOB)
            addType(CLOB, Value.CLOB)
            addType(ARRAY, Value.ARRAY)
            addType(JAVA_OBJECT, Value.JAVA_OBJECT)
            addType(UUID, Value.UUID)
            addType(CHAR, Value.CHAR)
            addType(GEOMETRY, Value.GEOMETRY)
            addType(TIMESTAMP_TZ, Value.TIMESTAMP_TZ)
            addType(ENUM, Value.ENUM)
            addType(26, Value.INTERVAL_YEAR)
            addType(27, Value.INTERVAL_MONTH)
            addType(28, Value.INTERVAL_DAY)
            addType(29, Value.INTERVAL_HOUR)
            addType(30, Value.INTERVAL_MINUTE)
            addType(31, Value.INTERVAL_SECOND)
            addType(32, Value.INTERVAL_YEAR_TO_MONTH)
            addType(33, Value.INTERVAL_DAY_TO_HOUR)
            addType(34, Value.INTERVAL_DAY_TO_MINUTE)
            addType(35, Value.INTERVAL_DAY_TO_SECOND)
            addType(36, Value.INTERVAL_HOUR_TO_MINUTE)
            addType(37, Value.INTERVAL_HOUR_TO_SECOND)
            addType(38, Value.INTERVAL_MINUTE_TO_SECOND)
            addType(39, Value.ROW)
            addType(40, Value.JSON)
            addType(41, Value.TIME_TZ)
            addType(42, Value.BINARY)
            addType(43, Value.DECFLOAT)
        }
    }

    private var `in`: DataInputStream? = null
    private var out: DataOutputStream? = null
    var version: Int = -1
    var ssl: Boolean = false
    lateinit var lobMacSalt: ByteArray

    /**
     * Initialize the transfer object. This method will try to open an input and
     * output stream.
     * @throws IOException on failure
     */
    @Synchronized
    @Throws(IOException::class)
    fun init() {
        if (socket == null) return
        `in` = DataInputStream(BufferedInputStream(socket!!.getInputStream(), BUFFER_SIZE))
        out = DataOutputStream(BufferedOutputStream(socket!!.getOutputStream(), BUFFER_SIZE))
    }

    /**
     * Write pending changes.
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun flush() = out!!.flush()

    /**
     * Write a boolean.
     *
     * @param x the value
     * @return itself
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun writeBoolean(x: Boolean): Transfer = apply {
        out!!.writeByte((if (x) 1 else 0).toByte().toInt())
    }

    /**
     * Read a boolean.
     *
     * @return the value
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun readBoolean(): Boolean = `in`!!.readByte().toInt() != 0

    /**
     * Write a byte.
     *
     * @param x the value
     * @return itself
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun writeByte(x: Byte): Transfer = apply {
        out!!.writeByte(x.toInt())
    }

    /**
     * Read a byte.
     *
     * @return the value
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun readByte(): Byte = `in`!!.readByte()

    /**
     * Write a short.
     *
     * @param x the value
     * @return itself
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    private fun writeShort(x: Short): Transfer = apply {
        out!!.writeShort(x.toInt())
    }

    /**
     * Read a short.
     *
     * @return the value
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    private fun readShort(): Short = `in`!!.readShort()

    /**
     * Write an int.
     *
     * @param x the value
     * @return itself
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun writeInt(x: Int): Transfer = apply {
        out!!.writeInt(x)
    }

    /**
     * Read an int.
     *
     * @return the value
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun readInt(): Int = `in`!!.readInt()

    /**
     * Write a long.
     *
     * @param x the value
     * @return itself
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun writeLong(x: Long): Transfer = apply {
        out!!.writeLong(x)
    }

    /**
     * Read a long.
     *
     * @return the value
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun readLong(): Long = `in`!!.readLong()

    /**
     * Write a double.
     *
     * @param i the value
     * @return itself
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    private fun writeDouble(i: Double): Transfer = apply {
        out!!.writeDouble(i)
    }

    /**
     * Write a float.
     *
     * @param i the value
     * @return itself
     */
    @Throws(IOException::class)
    private fun writeFloat(i: Float): Transfer = apply {
        out!!.writeFloat(i)
    }

    /**
     * Read a double.
     *
     * @return the value
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    private fun readDouble(): Double = `in`!!.readDouble()

    /**
     * Read a float.
     *
     * @return the value
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    private fun readFloat(): Float = `in`!!.readFloat()

    /**
     * Write a string. The maximum string length is Integer.MAX_VALUE.
     *
     * @param s the value
     * @return itself
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun writeString(s: String?): Transfer = apply {
        if (s == null) {
            out!!.writeInt(-1)
        } else {
            out!!.writeInt(s.length)
            out!!.writeChars(s)
        }
    }


    /**
     * Read a string.
     *
     * @return the value
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun readString(): String? {
        val len = `in`!!.readInt()
        if (len == -1) return null

        val buff = StringBuilder(len)
        for (i in 0 until len) {
            buff.append(`in`!!.readChar())
        }
        return StringUtils.cache(buff.toString())
    }


    /**
     * Write a byte array.
     *
     * @param data the value
     * @return itself
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun writeBytes(data: ByteArray?): Transfer = apply {
        if (data == null) {
            writeInt(-1)
        } else {
            writeInt(data.size)
            out!!.write(data)
        }
    }

    /**
     * Write a number of bytes.
     *
     * @param buff the value
     * @param off the offset
     * @param len the length
     * @return itself
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun writeBytes(buff: ByteArray?, off: Int, len: Int): Transfer = apply {
        out!!.write(buff, off, len)
    }

    /**
     * Read a byte array.
     *
     * @return the value
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun readBytes(): ByteArray? {
        val len = readInt()
        return if (len == -1) null else Utils.newBytes(len).apply { `in`!!.readFully(this) }
    }

    /**
     * Read a number of bytes.
     *
     * @param buff the target buffer
     * @param off the offset
     * @param len the number of bytes to read
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun readBytes(buff: ByteArray?, off: Int, len: Int) {
        `in`!!.readFully(buff, off, len)
    }


    /**
     * Close the transfer object and the socket.
     */
    @Synchronized
    override fun close() {
        if (socket == null) return
        try {
            out?.flush()
            socket!!.close()
        } catch (e: IOException) {
            DbException.traceThrowable(e)
        } finally {
            socket = null
        }
    }

    /**
     * Write value type, precision, and scale.
     *
     * @param type data type information
     * @return itself
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun writeTypeInfo(type: TypeInfo): Transfer = apply {
        if (version >= Constants.TCP_PROTOCOL_VERSION_20) writeTypeInfo20(type) else writeTypeInfo19(type)
    }

    @Throws(IOException::class)
    private fun writeTypeInfo20(type: TypeInfo) {
        val valueType: Int = type.valueType
        writeInt(VALUE_TO_TI[valueType + 1])
        when (valueType) {
            Value.UNKNOWN, Value.NULL, Value.BOOLEAN, Value.TINYINT, Value.SMALLINT, Value.INTEGER, Value.BIGINT, Value.DATE, Value.UUID -> {}
            Value.CHAR, Value.VARCHAR, Value.VARCHAR_IGNORECASE, Value.BINARY, Value.VARBINARY, Value.DECFLOAT, Value.JAVA_OBJECT, Value.JSON -> writeInt(type.getDeclaredPrecision().toInt())
            Value.CLOB, Value.BLOB -> writeLong(type.getDeclaredPrecision())
            Value.NUMERIC -> {
                writeInt(type.getDeclaredPrecision().toInt())
                writeInt(type.scale)
                writeBoolean(type.extTypeInfo != null)
            }

            Value.REAL, Value.DOUBLE, Value.INTERVAL_YEAR, Value.INTERVAL_MONTH, Value.INTERVAL_DAY, Value.INTERVAL_HOUR, Value.INTERVAL_MINUTE, Value.INTERVAL_YEAR_TO_MONTH, Value.INTERVAL_DAY_TO_HOUR, Value.INTERVAL_DAY_TO_MINUTE, Value.INTERVAL_HOUR_TO_MINUTE -> writeBytePrecisionWithDefault(
                type.getDeclaredPrecision())

            Value.TIME, Value.TIME_TZ, Value.TIMESTAMP, Value.TIMESTAMP_TZ -> writeByteScaleWithDefault(type.scale)
            Value.INTERVAL_SECOND, Value.INTERVAL_DAY_TO_SECOND, Value.INTERVAL_HOUR_TO_SECOND, Value.INTERVAL_MINUTE_TO_SECOND -> {
                writeBytePrecisionWithDefault(type.getDeclaredPrecision())
                writeByteScaleWithDefault(type.scale)
            }

            Value.ENUM -> writeTypeInfoEnum(type)
//TODO            Value.GEOMETRY -> writeTypeInfoGeometry(type)
            Value.ARRAY -> {
                writeInt(type.getDeclaredPrecision().toInt())
                writeTypeInfo(type.extTypeInfo as TypeInfo)
            }

            Value.ROW -> writeTypeInfoRow(type)
            else -> throw DbException.getUnsupportedException("value type $valueType")
        }
    }

    @Throws(IOException::class)
    private fun writeBytePrecisionWithDefault(precision: Long) {
        writeByte(if (precision >= 0) precision.toByte() else -1)
    }

    @Throws(IOException::class)
    private fun writeByteScaleWithDefault(scale: Int) {
        writeByte(if (scale >= 0) scale.toByte() else -1)
    }

    @Throws(IOException::class)
    private fun writeTypeInfoEnum(type: TypeInfo) = apply {
        val ext = type.extTypeInfo as ExtTypeInfoEnum?
        if (ext != null) {
            val c = ext.getCount()
            writeInt(c)
            for (i in 0 until c) {
                writeString(ext.getEnumerator(i))
            }
        } else {
            writeInt(0)
        }
    }

    @Throws(IOException::class)
    private fun writeTypeInfoRow(type: TypeInfo) {
        val fields = (type.extTypeInfo as ExtTypeInfoRow).fields
        writeInt(fields.size)
        for ((key, value) in fields) {
            writeString(key).writeTypeInfo(value)
        }
    }

    @Throws(IOException::class)
    private fun writeTypeInfo19(type: TypeInfo) {
        var valueType: Int = type.valueType
        when (valueType) {
            Value.BINARY -> valueType = Value.VARBINARY
            Value.DECFLOAT -> valueType = Value.NUMERIC
        }
        writeInt(VALUE_TO_TI[valueType + 1]).writeLong(type.precision).writeInt(type.scale)
    }

    /**
     * Read a type information.
     *
     * @return the type information
     * @throws IOException on failure
     */
    @Throws(IOException::class)
    fun readTypeInfo(): TypeInfo = if (version >= Constants.TCP_PROTOCOL_VERSION_20) {
        readTypeInfo20()
    } else {
        readTypeInfo19()
    }

    @Throws(IOException::class)
    private fun readTypeInfo20(): TypeInfo {
        val valueType = TI_TO_VALUE[readInt() + 1]
        var precision = -1L
        var scale = -1
        var ext: ExtTypeInfo? = null
        when (valueType) {
            Value.UNKNOWN, Value.NULL, Value.BOOLEAN, Value.TINYINT, Value.SMALLINT, Value.INTEGER, Value.BIGINT, Value.DATE, Value.UUID -> {}
            Value.CHAR, Value.VARCHAR, Value.VARCHAR_IGNORECASE, Value.BINARY, Value.VARBINARY, Value.DECFLOAT, Value.JAVA_OBJECT, Value.JSON -> precision = readInt().toLong()
            Value.CLOB, Value.BLOB -> precision = readLong()
            Value.NUMERIC -> {
                precision = readInt().toLong()
                scale = readInt()
                if (readBoolean()) {
                    ext = ExtTypeInfoNumeric.DECIMAL
                }
            }

            Value.REAL, Value.DOUBLE, Value.INTERVAL_YEAR, Value.INTERVAL_MONTH, Value.INTERVAL_DAY, Value.INTERVAL_HOUR, Value.INTERVAL_MINUTE, Value.INTERVAL_YEAR_TO_MONTH, Value.INTERVAL_DAY_TO_HOUR, Value.INTERVAL_DAY_TO_MINUTE, Value.INTERVAL_HOUR_TO_MINUTE -> precision =
                readByte().toLong()

            Value.TIME, Value.TIME_TZ, Value.TIMESTAMP, Value.TIMESTAMP_TZ -> scale = readByte().toInt()
            Value.INTERVAL_SECOND, Value.INTERVAL_DAY_TO_SECOND, Value.INTERVAL_HOUR_TO_SECOND, Value.INTERVAL_MINUTE_TO_SECOND -> {
                precision = readByte().toLong()
                scale = readByte().toInt()
            }

            Value.ENUM -> ext = readTypeInfoEnum()
//TODO            Value.GEOMETRY -> ext = readTypeInfoGeometry()
            Value.ARRAY -> {
                precision = readInt().toLong()
                ext = readTypeInfo()
            }

            Value.ROW -> ext = readTypeInfoRow()
            else -> throw DbException.getUnsupportedException("value type $valueType")
        }
        return TypeInfo.getTypeInfo(valueType, precision, scale, ext)
    }

    @Throws(IOException::class)
    private fun readTypeInfoEnum(): ExtTypeInfo? {
        val c = readInt()
        if (c <= 0) return null

        val enumerators = arrayOfNulls<String>(c)
        for (i in 0 until c) {
            enumerators[i] = readString()
        }
        return ExtTypeInfoEnum(enumerators)
    }

    @Throws(IOException::class)
    private fun readTypeInfoRow(): ExtTypeInfo? {
        val l = readInt()
        if (l <= 0) return null

        val fields = LinkedHashMap<String?, TypeInfo>()
        for (i in 0 until l) {
            val name: String? = readString()
            if (fields.putIfAbsent(name, readTypeInfo()) != null) {
                throw DbException.get(ErrorCode.DUPLICATE_COLUMN_NAME_1, name!!)
            }
        }
        return ExtTypeInfoRow(fields)
    }

    @Throws(IOException::class)
    private fun readTypeInfo19(): TypeInfo {
        return TypeInfo.getTypeInfo(TI_TO_VALUE[readInt() + 1], readLong(), readInt(), null)
    }
}