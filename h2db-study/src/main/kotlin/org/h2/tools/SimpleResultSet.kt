package org.h2.tools

import org.h2.api.ErrorCode
import org.h2.jdbc.JdbcResultSetBackwardsCompat
import org.h2.message.DbException
import org.h2.util.*
import org.h2.value.DataType
import java.io.InputStream
import java.io.Reader
import java.lang.Long.*
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URL
import java.sql.*
import java.sql.Date
import java.util.*
import kotlin.collections.ArrayList

/**
 * This class is a simple result set and meta data implementation.
 * It can be used in Java functions that return a result set.
 * Only the most basic methods are implemented, the others throw an exception.
 * This implementation is standalone, and only relies on standard classes.
 * It can be extended easily if required.
 *
 * An application can create a result set using the following code:
 * <pre>
 * SimpleResultSet rs = new SimpleResultSet();
 * rs.addColumn(&quot;ID&quot;, Types.INTEGER, 10, 0);
 * rs.addColumn(&quot;NAME&quot;, Types.VARCHAR, 255, 0);
 * rs.addRow(0, &quot;Hello&quot; });
 * rs.addRow(1, &quot;World&quot; });
 * </pre>
 */
open class SimpleResultSet : ResultSet, ResultSetMetaData, JdbcResultSetBackwardsCompat {
    companion object {
        /**
         * INTERNAL
         */
        fun getUnsupportedException(): SQLException {
            return DbException.getJdbcSQLException(ErrorCode.FEATURE_NOT_SUPPORTED_1)!!
        }

        /**
         * A simple array implementation,
         * backed by an object array
         */
        class SimpleArray internal constructor(private val value: Array<Any>) : java.sql.Array {
            /**
             * Get the object array.
             *
             * @return the object array
             */
            override fun getArray(): Any = value

            /**
             * INTERNAL
             */
            @Throws(SQLException::class)
            override fun getArray(map: Map<String?, Class<*>?>?): Any = throw getUnsupportedException()

            /**
             * INTERNAL
             */
            @Throws(SQLException::class)
            override fun getArray(index: Long, count: Int): Any = throw getUnsupportedException()

            /**
             * INTERNAL
             */
            @Throws(SQLException::class)
            override fun getArray(index: Long, count: Int, map: Map<String?, Class<*>?>?): Any =
                throw getUnsupportedException()

            /**
             * Get the base type of this array.
             *
             * @return Types.NULL
             */
            override fun getBaseType(): Int = Types.NULL

            /**
             * Get the base type name of this array.
             *
             * @return "NULL"
             */
            override fun getBaseTypeName(): String = "NULL"

            /**
             * INTERNAL
             */
            @Throws(SQLException::class)
            override fun getResultSet(): ResultSet = throw getUnsupportedException()

            /**
             * INTERNAL
             */
            @Throws(SQLException::class)
            override fun getResultSet(map: Map<String?, Class<*>?>?): ResultSet = throw getUnsupportedException()

            /**
             * INTERNAL
             */
            @Throws(SQLException::class)
            override fun getResultSet(index: Long, count: Int): ResultSet = throw getUnsupportedException()

            /**
             * INTERNAL
             */
            @Throws(SQLException::class)
            override fun getResultSet(
                index: Long, count: Int,
                map: Map<String?, Class<*>?>?,
            ): ResultSet = throw getUnsupportedException()

            /**
             * INTERNAL
             */
            override fun free() {
                // nothing to do
            }
        }

        @Throws(SQLException::class)
        private fun asInputStream(o: Any?): InputStream? {
            return if (o is Blob) o.binaryStream else o as InputStream?
        }

        @Throws(SQLException::class)
        private fun asReader(o: Any?): Reader? {
            return if (o is Clob) o.characterStream else o as Reader?
        }
    }

    private var rows: ArrayList<Array<Any?>>? = ArrayList()
    private var currentRow: Array<Any?>? = null
    private var rowId: Int = -1
    private var wasNull: Boolean = false
    private var source: SimpleRowSource? = null
    private var columns: ArrayList<SimpleColumnInfo>? = Utils.newSmallArrayList()
    private var autoClose = true

    /**
     * This constructor is used if the result set is later populated with addRow
     */
    constructor() {
        this.rows = Utils.newSmallArrayList()
    }

    /**
     * This constructor is used if the result set should retrieve the rows using the specified row source object.
     * @param _source the row source
     */
    constructor(_source: SimpleRowSource) {
        this.source = _source
    }

    /**
     * Adds a column to the result set.
     * All columns must be added before adding rows.
     * This method uses the default SQL type names.
     *
     * @param name null is replaced with C1, C2,...
     * @param sqlType the value returned in getColumnType(..)
     * @param precision the precision
     * @param scale the scale
     */
    open fun addColumn(name: String?, sqlType: Int, precision: Int, scale: Int) {
        val valueType: Int = DataType.convertSQLTypeToValueType(sqlType)
        addColumn(name, sqlType, DataType.getDataType(valueType).name,
            precision, scale)
    }

    /**
     * Adds a column to the result set.
     * All columns must be added before adding rows.
     *
     * @param name null is replaced with C1, C2,...
     * @param sqlType the value returned in getColumnType(..)
     * @param sqlTypeName the type name return in getColumnTypeName(..)
     * @param precision the precision
     * @param scale the scale
     */
    open fun addColumn(
        name: String?, sqlType: Int, sqlTypeName: String?,
        precision: Int, scale: Int,
    ) {
        check(!rows.isNullOrEmpty()) { "Cannot add a column after adding rows" }
        columns!!.add(SimpleColumnInfo(name ?: "C" + (columns!!.size + 1),
            sqlType, sqlTypeName, precision, scale))
    }

    /**
     * Add a new row to the result set.
     * Do not use this method when using a RowSource.
     *
     * @param row the row as an array of objects
     */
    open fun addRow(vararg row: Any?) {
        checkNotNull(rows) { "Cannot add a row when using RowSource" }
        rows!!.add(row as Array<Any?>)
    }

    /**
     * Returns ResultSet.CONCUR_READ_ONLY.
     *
     * @return CONCUR_READ_ONLY
     */
    override fun getConcurrency(): Int = ResultSet.CONCUR_READ_ONLY

    /**
     * Returns ResultSet.FETCH_FORWARD.
     *
     * @return FETCH_FORWARD
     */
    override fun getFetchDirection(): Int = ResultSet.FETCH_FORWARD

    /**
     * Returns 0.
     *
     * @return 0
     */
    override fun getFetchSize(): Int = 0

    /**
     * Returns the row number (1, 2,...) or 0 for no row.
     *
     * @return 0
     */
    override fun getRow(): Int {
        return if (currentRow == null) 0 else rowId + 1
    }

    /**
     * Returns the result set type. This is ResultSet.TYPE_FORWARD_ONLY for
     * auto-close result sets, and ResultSet.TYPE_SCROLL_INSENSITIVE for others.
     *
     * @return TYPE_FORWARD_ONLY or TYPE_SCROLL_INSENSITIVE
     */
    override fun getType(): Int {
        return if (autoClose) {
            ResultSet.TYPE_FORWARD_ONLY
        } else ResultSet.TYPE_SCROLL_INSENSITIVE
    }

    /**
     * Closes the result set and releases the resources.
     */
    override fun close() {
        currentRow = null
        rows = null
        columns = null
        rowId = -1
        source?.close()
        source = null
    }

    /**
     * Moves the cursor to the next row of the result set.
     *
     * @return true if successful, false if there are no more rows
     */
    @Throws(SQLException::class)
    override fun next(): Boolean {
        if (source != null) {
            rowId++
            currentRow = source!!.readRow()
            if (currentRow != null) {
                return true
            }
        } else if (rows != null && rowId < rows!!.size) {
            rowId++
            if (rowId < rows!!.size) {
                currentRow = rows!![rowId]
                return true
            }
            currentRow = null
        }
        if (autoClose) close()
        return false
    }

    /**
     * Moves the current position to before the first row, that means the result
     * set is reset.
     */
    @Throws(SQLException::class)
    override fun beforeFirst() {
        if (autoClose) {
            throw DbException.getJdbcSQLException(ErrorCode.RESULT_SET_NOT_SCROLLABLE)
        }
        rowId = -1
        source?.reset()
    }

    /**
     * Returns whether the last column accessed was null.
     *
     * @return true if the last column accessed was null
     */
    override fun wasNull(): Boolean = wasNull

    /**
     * Searches for a specific column in the result set. A case-insensitive
     * search is made.
     *
     * @param columnLabel the column label
     * @return the column index (1,2,...)
     * @throws SQLException if the column is not found or if the result set is
     * closed
     */
    @Throws(SQLException::class)
    override fun findColumn(columnLabel: String?): Int {
        if (columnLabel == null || columns == null) throw DbException.getJdbcSQLException(ErrorCode.COLUMN_NOT_FOUND_1,
            columnLabel)
        for (i in columns!!.indices) {
            if (columnLabel.equals(getColumn(i)!!.name, ignoreCase = true)) {
                return i + 1
            }
        }
        throw DbException.getJdbcSQLException(ErrorCode.COLUMN_NOT_FOUND_1, columnLabel)
    }

    @Throws(SQLException::class)
    private fun getColumn(i: Int): SimpleColumnInfo? {
        checkColumnIndex(i + 1)
        return columns!![i]
    }

    @Throws(SQLException::class)
    private fun checkColumnIndex(columnIndex: Int) {
        if (columnIndex < 1 || columnIndex > columns!!.size) {
            throw DbException.getInvalidValueException("columnIndex", columnIndex).getSQLException()!!
        }
    }

    /**
     * Returns a reference to itself.
     *
     * @return this
     */
    override fun getMetaData(): ResultSetMetaData? = this

    /**
     * Returns null.
     *
     * @return null
     */
    override fun getWarnings(): SQLWarning? = null

    /**
     * Returns null.
     *
     * @return null
     */
    override fun getStatement(): Statement? = null

    /**
     * INTERNAL
     */
    override fun clearWarnings() = // nothing to do
        Unit

    @Throws(SQLException::class)
    private operator fun get(columnIndex: Int): Any? {
        var columnIndex = columnIndex
        if (currentRow == null) {
            throw DbException.getJdbcSQLException(ErrorCode.NO_DATA_AVAILABLE)
        }
        checkColumnIndex(columnIndex)
        columnIndex--
        val o = if (columnIndex < currentRow!!.size) currentRow!![columnIndex] else null
        wasNull = o == null
        return o
    }

    // ---- get ---------------------------------------------
    /**
     * Returns the value as a java.sql.Array.
     *
     * @param columnIndex (1,2,...)
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getArray(columnIndex: Int): java.sql.Array? {
        val o = get(columnIndex) as Array<Any>?
        return o?.let { SimpleArray(it) }
    }


    /**
     * Returns the value as a java.sql.Array.
     *
     * @param columnLabel the column label
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getArray(columnLabel: String?): java.sql.Array? = getArray(findColumn(columnLabel))

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getAsciiStream(columnIndex: Int): InputStream? = throw getUnsupportedException()

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getAsciiStream(columnLabel: String?): InputStream? = throw getUnsupportedException()


    /**
     * Returns the value as a java.math.BigDecimal.
     *
     * @param columnIndex (1,2,...)
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getBigDecimal(columnIndex: Int): BigDecimal? {
        var o = get(columnIndex)
        return if (o is BigDecimal) o else o?.let { BigDecimal(o.toString()) }
    }

    /**
     * Returns the value as a java.math.BigDecimal.
     *
     * @param columnLabel the column label
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getBigDecimal(columnLabel: String?): BigDecimal? = getBigDecimal(findColumn(columnLabel))


    @Deprecated("INTERNAL")
    @Throws(SQLException::class)
    override fun getBigDecimal(columnIndex: Int, scale: Int): BigDecimal? = throw getUnsupportedException()


    @Deprecated("INTERNAL")
    @Throws(SQLException::class)
    override fun getBigDecimal(columnLabel: String?, scale: Int): BigDecimal? = throw getUnsupportedException()

    /**
     * Returns the value as a java.io.InputStream.
     *
     * @param columnIndex (1,2,...)
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getBinaryStream(columnIndex: Int): InputStream? = asInputStream(get(columnIndex))

    /**
     * Returns the value as a java.io.InputStream.
     *
     * @param columnLabel the column label
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getBinaryStream(columnLabel: String?): InputStream? = getBinaryStream(findColumn(columnLabel))

    /**
     * Returns the value as a java.sql.Blob.
     * This is only supported if the
     * result set was created using a Blob object.
     *
     * @param columnIndex (1,2,...)
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getBlob(columnIndex: Int): Blob? = get(columnIndex) as Blob?

    /**
     * Returns the value as a java.sql.Blob.
     * This is only supported if the
     * result set was created using a Blob object.
     *
     * @param columnLabel the column label
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getBlob(columnLabel: String?): Blob? = getBlob(findColumn(columnLabel))

    /**
     * Returns the value as a boolean.
     *
     * @param columnIndex (1,2,...)
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getBoolean(columnIndex: Int): Boolean {
        return when (val o = get(columnIndex)) {
            null -> false
            is Boolean -> o
            is Number -> when (o) {
                is Double, is Float -> o.toDouble() != 0.0
                is BigDecimal -> o.signum() != 0
                is BigInteger -> o.signum() != 0
                else -> o.toLong() != 0L
            }
            else -> Utils.parseBoolean(o.toString(), false, true)
        }
    }

    /**
     * Returns the value as a boolean.
     *
     * @param columnLabel the column label
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getBoolean(columnLabel: String?): Boolean = getBoolean(findColumn(columnLabel))

    /**
     * Returns the value as a byte.
     *
     * @param columnIndex (1,2,...)
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getByte(columnIndex: Int): Byte {
        return when (val o = get(columnIndex)) {
            null -> 0
            is Number -> o.toByte()
            else -> java.lang.Byte.decode(o.toString())
        }
    }

    /**
     * Returns the value as a byte.
     *
     * @param columnLabel the column label
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getByte(columnLabel: String?): Byte = getByte(findColumn(columnLabel))

    /**
     * Returns the value as a byte array.
     *
     * @param columnIndex (1,2,...)
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getBytes(columnIndex: Int): ByteArray? {
        return when (val o = get(columnIndex)) {
            null, is ByteArray -> o as ByteArray?
            is UUID -> Bits.uuidToBytes(o)
            else -> JdbcUtils.serialize(o, null)
        }
    }

    /**
     * Returns the value as a byte array.
     *
     * @param columnLabel the column label
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getBytes(columnLabel: String?): ByteArray? = getBytes(findColumn(columnLabel))


    /**
     * Returns the value as a java.io.Reader.
     * This is only supported if the
     * result set was created using a Clob or Reader object.
     *
     * @param columnIndex (1,2,...)
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getCharacterStream(columnIndex: Int): Reader? {
        return asReader(get(columnIndex))
    }

    /**
     * Returns the value as a java.io.Reader.
     * This is only supported if the
     * result set was created using a Clob or Reader object.
     *
     * @param columnLabel the column label
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getCharacterStream(columnLabel: String?): Reader? = getCharacterStream(findColumn(columnLabel))


    /**
     * Returns the value as a java.sql.Clob.
     * This is only supported if the
     * result set was created using a Clob object.
     *
     * @param columnIndex (1,2,...)
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getClob(columnIndex: Int): Clob? {
        return get(columnIndex) as Clob?
    }

    /**
     * Returns the value as a java.sql.Clob.
     * This is only supported if the
     * result set was created using a Clob object.
     *
     * @param columnLabel the column label
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getClob(columnLabel: String?): Clob? {
        return getClob(findColumn(columnLabel))
    }

    /**
     * Returns the value as an java.sql.Date.
     *
     * @param columnIndex (1,2,...)
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getDate(columnIndex: Int): Date? {
        return get(columnIndex) as Date?
    }

    /**
     * Returns the value as a java.sql.Date.
     *
     * @param columnLabel the column label
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getDate(columnLabel: String?): Date? {
        return getDate(findColumn(columnLabel))
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getDate(columnIndex: Int, cal: Calendar?): Date? {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getDate(columnLabel: String?, cal: Calendar?): Date? {
        throw getUnsupportedException()
    }

    /**
     * Returns the value as an double.
     *
     * @param columnIndex (1,2,...)
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getDouble(columnIndex: Int): Double {
        return when (val o = get(columnIndex)) {
            is Number -> o.toDouble()
            null -> 0.0
            else -> o.toString().toDouble()
        }
    }

    /**
     * Returns the value as a double.
     *
     * @param columnLabel the column label
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getDouble(columnLabel: String?): Double {
        return getDouble(findColumn(columnLabel))
    }

    /**
     * Returns the value as a float.
     *
     * @param columnIndex (1,2,...)
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getFloat(columnIndex: Int): Float {
        return when (val o = get(columnIndex)) {
            is Number -> o.toFloat()
            null -> 0.0f
            else -> o.toString().toFloat()
        }
    }

    /**
     * Returns the value as a float.
     *
     * @param columnLabel the column label
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getFloat(columnLabel: String?): Float {
        return getFloat(findColumn(columnLabel))
    }

    /**
     * Returns the value as an int.
     *
     * @param columnIndex (1,2,...)
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getInt(columnIndex: Int): Int {
        return when (val o = get(columnIndex)) {
            is Number -> o.toInt()
            null -> 0
            else -> Integer.decode(o.toString())
        }
    }

    /**
     * Returns the value as an int.
     *
     * @param columnLabel the column label
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getInt(columnLabel: String?): Int {
        return getInt(findColumn(columnLabel))
    }

    /**
     * Returns the value as a long.
     *
     * @param columnIndex (1,2,...)
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getLong(columnIndex: Int): Long {
        return when (val o = get(columnIndex)) {
            is Number -> o.toLong()
            null -> 0
            else -> decode(o.toString())
        }
    }

    /**
     * Returns the value as a long.
     *
     * @param columnLabel the column label
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getLong(columnLabel: String?): Long {
        return getLong(findColumn(columnLabel))
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getNCharacterStream(columnIndex: Int): Reader? {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getNCharacterStream(columnLabel: String?): Reader? {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getNClob(columnIndex: Int): NClob? {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getNClob(columnLabel: String?): NClob? {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getNString(columnIndex: Int): String? {
        return getString(columnIndex)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getNString(columnLabel: String?): String? {
        return getString(columnLabel)
    }

    /**
     * Returns the value as an Object.
     *
     * @param columnIndex (1,2,...)
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getObject(columnIndex: Int): Any? {
        return get(columnIndex)
    }

    /**
     * Returns the value as an Object.
     *
     * @param columnLabel the column label
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getObject(columnLabel: String?): Any? {
        return getObject(findColumn(columnLabel))
    }

    /**
     * Returns the value as an Object of the specified type.
     *
     * @param columnIndex the column index (1, 2, ...)
     * @param type the class of the returned value
     * @return the value
     */
    @Throws(SQLException::class)
    override fun <T> getObject(columnIndex: Int, type: Class<T>): T? {
        if (get(columnIndex) == null) return null
        return when (type) {
            BigDecimal::class.java -> getBigDecimal(columnIndex) as T?
            BigInteger::class.java -> getBigDecimal(columnIndex)!!.toBigInteger() as T
            String::class.java -> getString(columnIndex) as T
            Boolean::class.java -> getBoolean(columnIndex) as T
            Byte::class.java -> getByte(columnIndex) as T
            Short::class.java -> getShort(columnIndex) as T
            Int::class.java -> getInt(columnIndex) as T
            Long::class.java -> getLong(columnIndex) as T
            Float::class.java -> getFloat(columnIndex) as T
            Double::class.java -> getDouble(columnIndex) as T
            Date::class.java -> getDate(columnIndex) as T?
            Time::class.java -> getTime(columnIndex) as T
            Timestamp::class.java -> getTimestamp(columnIndex) as T
            UUID::class.java -> getObject(columnIndex) as T?
            ByteArray::class.java -> getBytes(columnIndex) as T?
            java.sql.Array::class.java -> getArray(columnIndex) as T?
            Blob::class.java -> getBlob(columnIndex) as T?
            Clob::class.java -> getClob(columnIndex) as T?
            else -> throw getUnsupportedException()
        }
    }

    /**
     * Returns the value as an Object of the specified type.
     *
     * @param columnName the column name
     * @param type the class of the returned value
     * @return the value
     */
    @Throws(SQLException::class)
    override fun <T> getObject(columnName: String?, type: Class<T>?): T? = getObject(findColumn(columnName), type!!)

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getObject(columnIndex: Int, map: Map<String?, Class<*>?>?): Any? = throw getUnsupportedException()

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getObject(columnLabel: String?, map: Map<String?, Class<*>?>?): Any? = throw getUnsupportedException()

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getRef(columnIndex: Int): Ref? = throw getUnsupportedException()

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getRef(columnLabel: String?): Ref? = throw getUnsupportedException()

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getRowId(columnIndex: Int): RowId? = throw getUnsupportedException()

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getRowId(columnLabel: String?): RowId? = throw getUnsupportedException()

    /**
     * Returns the value as a short.
     *
     * @param columnIndex (1,2,...)
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getShort(columnIndex: Int): Short {
        return when (val o = get(columnIndex)) {
            is Number -> o.toShort()
            null -> 0
            else -> o.toString().toShort()
        }
    }

    /**
     * Returns the value as a short.
     *
     * @param columnLabel the column label
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getShort(columnLabel: String?): Short = throw getUnsupportedException()

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getSQLXML(columnIndex: Int): SQLXML? = throw getUnsupportedException()

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getSQLXML(columnLabel: String?): SQLXML? = throw getUnsupportedException()

    /**
     * Returns the value as a String.
     *
     * @param columnIndex (1,2,...)
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getString(columnIndex: Int): String? {
        val o = get(columnIndex) ?: return null
        return when (columns!![columnIndex - 1].type) {
            Types.CLOB -> {
                val c = o as Clob
                c.getSubString(1, MathUtils.convertLongToInt(c.length()))
            }
            else -> o.toString()
        }
    }

    override fun getString(columnLabel: String?): String {
        return getString(columnLabel)
    }

    /**
     * Returns the value as an java.sql.Time.
     *
     * @param columnIndex (1,2,...)
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getTime(columnIndex: Int): Time? = get(columnIndex) as Time?

    /**
     * Returns the value as a java.sql.Time.
     *
     * @param columnLabel the column label
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getTime(columnLabel: String?): Time? = getTime(findColumn(columnLabel))

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getTime(columnIndex: Int, cal: Calendar?): Time? = throw getUnsupportedException()

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getTime(columnLabel: String?, cal: Calendar?): Time? = throw getUnsupportedException()

    /**
     * Returns the value as an java.sql.Timestamp.
     *
     * @param columnIndex (1,2,...)
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getTimestamp(columnIndex: Int): Timestamp? = get(columnIndex) as Timestamp?

    /**
     * Returns the value as a java.sql.Timestamp.
     *
     * @param columnLabel the column label
     * @return the value
     */
    @Throws(SQLException::class)
    override fun getTimestamp(columnLabel: String?): Timestamp? = getTimestamp(findColumn(columnLabel))

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getTimestamp(columnIndex: Int, cal: Calendar?): Timestamp? {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getTimestamp(columnLabel: String?, cal: Calendar?): Timestamp? {
        throw getUnsupportedException()
    }


    @Deprecated("INTERNAL")
    @Throws(SQLException::class)
    override fun getUnicodeStream(columnIndex: Int): InputStream? {
        throw getUnsupportedException()
    }


    @Deprecated("INTERNAL")
    @Throws(SQLException::class)
    override fun getUnicodeStream(columnLabel: String?): InputStream? {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getURL(columnIndex: Int): URL? {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getURL(columnLabel: String?): URL? {
        throw getUnsupportedException()
    }

    // ---- update ---------------------------------------------
    @Throws(SQLException::class)
    private fun checkClosed() {
        if (columns == null) {
            throw DbException.getJdbcSQLException(ErrorCode.OBJECT_CLOSED)
        }
    }

    // --- private -----------------------------
    @Throws(SQLException::class)
    private fun update(columnIndex: Int, obj: Any?) {
        checkClosed()
        checkColumnIndex(columnIndex)
        currentRow!![columnIndex - 1] = obj
    }

    @Throws(SQLException::class)
    private fun update(columnLabel: String?, obj: Any?) {
        currentRow!![findColumn(columnLabel) - 1] = obj
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateArray(columnIndex: Int, x: java.sql.Array?) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateArray(columnLabel: String?, x: java.sql.Array?) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateAsciiStream(columnIndex: Int, x: InputStream?) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateAsciiStream(columnLabel: String?, x: InputStream?) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateAsciiStream(columnIndex: Int, x: InputStream?, length: Int) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateAsciiStream(columnLabel: String?, x: InputStream?, length: Int) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateAsciiStream(columnIndex: Int, x: InputStream?, length: Long) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateAsciiStream(columnLabel: String?, x: InputStream?, length: Long) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateBigDecimal(columnIndex: Int, x: BigDecimal?) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateBigDecimal(columnLabel: String?, x: BigDecimal?) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateBinaryStream(columnIndex: Int, x: InputStream?) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateBinaryStream(columnLabel: String?, x: InputStream?) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateBinaryStream(columnIndex: Int, x: InputStream?, length: Int) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateBinaryStream(columnLabel: String?, x: InputStream?, length: Int) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateBinaryStream(columnIndex: Int, x: InputStream?, length: Long) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateBinaryStream(columnLabel: String?, x: InputStream?, length: Long) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateBlob(columnIndex: Int, x: Blob?) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateBlob(columnLabel: String?, x: Blob?) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateBlob(columnIndex: Int, x: InputStream?) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateBlob(columnLabel: String?, x: InputStream?) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateBlob(columnIndex: Int, x: InputStream?, length: Long) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateBlob(columnLabel: String?, x: InputStream?, length: Long) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateBoolean(columnIndex: Int, x: Boolean) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateBoolean(columnLabel: String?, x: Boolean) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateByte(columnIndex: Int, x: Byte) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateByte(columnLabel: String?, x: Byte) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateBytes(columnIndex: Int, x: ByteArray?) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateBytes(columnLabel: String?, x: ByteArray?) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateCharacterStream(columnIndex: Int, x: Reader?) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateCharacterStream(columnLabel: String?, x: Reader?) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateCharacterStream(columnIndex: Int, x: Reader?, length: Int) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateCharacterStream(columnLabel: String?, x: Reader?, length: Int) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateCharacterStream(columnIndex: Int, x: Reader?, length: Long) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateCharacterStream(columnLabel: String?, x: Reader?, length: Long) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateClob(columnIndex: Int, x: Clob?) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateClob(columnLabel: String?, x: Clob?) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateClob(columnIndex: Int, x: Reader?) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateClob(columnLabel: String?, x: Reader?) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateClob(columnIndex: Int, x: Reader?, length: Long) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateClob(columnLabel: String?, x: Reader?, length: Long) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateDate(columnIndex: Int, x: Date?) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateDate(columnLabel: String?, x: Date?) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateDouble(columnIndex: Int, x: Double) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateDouble(columnLabel: String?, x: Double) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateFloat(columnIndex: Int, x: Float) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateFloat(columnLabel: String?, x: Float) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateInt(columnIndex: Int, x: Int) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateInt(columnLabel: String?, x: Int) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateLong(columnIndex: Int, x: Long) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateLong(columnLabel: String?, x: Long) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateNCharacterStream(columnIndex: Int, x: Reader?) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateNCharacterStream(columnLabel: String?, x: Reader?) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateNCharacterStream(columnIndex: Int, x: Reader?, length: Long) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateNCharacterStream(columnLabel: String?, x: Reader?, length: Long) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateNClob(columnIndex: Int, x: NClob?) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateNClob(columnLabel: String?, x: NClob?) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateNClob(columnIndex: Int, x: Reader?) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateNClob(columnLabel: String?, x: Reader?) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateNClob(columnIndex: Int, x: Reader?, length: Long) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateNClob(columnLabel: String?, x: Reader?, length: Long) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateNString(columnIndex: Int, x: String?) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateNString(columnLabel: String?, x: String?) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateNull(columnIndex: Int) {
        update(columnIndex, null)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateNull(columnLabel: String?) {
        update(columnLabel, null)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateObject(columnIndex: Int, x: Any?) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateObject(columnLabel: String?, x: Any?) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateObject(columnIndex: Int, x: Any?, scale: Int) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateObject(columnLabel: String?, x: Any?, scale: Int) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateRef(columnIndex: Int, x: Ref?) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateRef(columnLabel: String?, x: Ref?) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateRowId(columnIndex: Int, x: RowId?) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateRowId(columnLabel: String?, x: RowId?) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateShort(columnIndex: Int, x: Short) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateShort(columnLabel: String?, x: Short) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateSQLXML(columnIndex: Int, x: SQLXML?) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateSQLXML(columnLabel: String?, x: SQLXML?) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateString(columnIndex: Int, x: String?) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateString(columnLabel: String?, x: String?) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateTime(columnIndex: Int, x: Time?) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateTime(columnLabel: String?, x: Time?) {
        update(columnLabel, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateTimestamp(columnIndex: Int, x: Timestamp?) {
        update(columnIndex, x)
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateTimestamp(columnLabel: String?, x: Timestamp?) {
        update(columnLabel, x)
    }

    // ---- result set meta data ---------------------------------------------

    // ---- result set meta data ---------------------------------------------
    /**
     * Returns the column count.
     *
     * @return the column count
     */
    override fun getColumnCount(): Int {
        return columns!!.size
    }

    /**
     * Returns 15.
     *
     * @param columnIndex (1,2,...)
     * @return 15
     */
    override fun getColumnDisplaySize(columnIndex: Int): Int {
        return 15
    }

    /**
     * Returns the SQL type.
     *
     * @param columnIndex (1,2,...)
     * @return the SQL type
     */
    @Throws(SQLException::class)
    override fun getColumnType(columnIndex: Int): Int {
        return getColumn(columnIndex - 1)!!.type
    }

    /**
     * Returns the precision.
     *
     * @param columnIndex (1,2,...)
     * @return the precision
     */
    @Throws(SQLException::class)
    override fun getPrecision(columnIndex: Int): Int {
        return getColumn(columnIndex - 1)!!.precision
    }

    /**
     * Returns the scale.
     *
     * @param columnIndex (1,2,...)
     * @return the scale
     */
    @Throws(SQLException::class)
    override fun getScale(columnIndex: Int): Int {
        return getColumn(columnIndex - 1)!!.scale
    }

    /**
     * Returns ResultSetMetaData.columnNullableUnknown.
     *
     * @param columnIndex (1,2,...)
     * @return columnNullableUnknown
     */
    override fun isNullable(columnIndex: Int): Int {
        return ResultSetMetaData.columnNullableUnknown
    }

    /**
     * Returns false.
     *
     * @param columnIndex (1,2,...)
     * @return false
     */
    override fun isAutoIncrement(columnIndex: Int): Boolean {
        return false
    }

    /**
     * Returns true.
     *
     * @param columnIndex (1,2,...)
     * @return true
     */
    override fun isCaseSensitive(columnIndex: Int): Boolean {
        return true
    }

    /**
     * Returns false.
     *
     * @param columnIndex (1,2,...)
     * @return false
     */
    override fun isCurrency(columnIndex: Int): Boolean {
        return false
    }

    /**
     * Returns false.
     *
     * @param columnIndex (1,2,...)
     * @return false
     */
    override fun isDefinitelyWritable(columnIndex: Int): Boolean {
        return false
    }

    /**
     * Returns true.
     *
     * @param columnIndex (1,2,...)
     * @return true
     */
    override fun isReadOnly(columnIndex: Int): Boolean {
        return true
    }

    /**
     * Returns true.
     *
     * @param columnIndex (1,2,...)
     * @return true
     */
    override fun isSearchable(columnIndex: Int): Boolean {
        return true
    }

    /**
     * Returns true.
     *
     * @param columnIndex (1,2,...)
     * @return true
     */
    override fun isSigned(columnIndex: Int): Boolean {
        return true
    }

    /**
     * Returns false.
     *
     * @param columnIndex (1,2,...)
     * @return false
     */
    override fun isWritable(columnIndex: Int): Boolean {
        return false
    }

    /**
     * Returns empty string.
     *
     * @param columnIndex (1,2,...)
     * @return empty string
     */
    override fun getCatalogName(columnIndex: Int): String? {
        return ""
    }

    /**
     * Returns the Java class name if this column.
     *
     * @param columnIndex (1,2,...)
     * @return the class name
     */
    @Throws(SQLException::class)
    override fun getColumnClassName(columnIndex: Int): String? {
        val type: Int = DataType.getValueTypeFromResultSet(this, columnIndex)
        return DataType.getTypeClassName(type, true)
    }

    /**
     * Returns the column label.
     *
     * @param columnIndex (1,2,...)
     * @return the column label
     */
    @Throws(SQLException::class)
    override fun getColumnLabel(columnIndex: Int): String? {
        return getColumn(columnIndex - 1)!!.name
    }

    /**
     * Returns the column name.
     *
     * @param columnIndex (1,2,...)
     * @return the column name
     */
    @Throws(SQLException::class)
    override fun getColumnName(columnIndex: Int): String? {
        return getColumnLabel(columnIndex)
    }

    /**
     * Returns the data type name of a column.
     *
     * @param columnIndex (1,2,...)
     * @return the type name
     */
    @Throws(SQLException::class)
    override fun getColumnTypeName(columnIndex: Int): String? {
        return getColumn(columnIndex - 1)!!.typeName
    }

    /**
     * Returns empty string.
     *
     * @param columnIndex (1,2,...)
     * @return empty string
     */
    override fun getSchemaName(columnIndex: Int): String? {
        return ""
    }

    /**
     * Returns empty string.
     *
     * @param columnIndex (1,2,...)
     * @return empty string
     */
    override fun getTableName(columnIndex: Int): String? {
        return ""
    }

    // ---- unsupported / result set -----------------------------------

    // ---- unsupported / result set -----------------------------------
    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun afterLast() {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun cancelRowUpdates() {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun deleteRow() {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun insertRow() {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun moveToCurrentRow() {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun moveToInsertRow() {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun refreshRow() {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun updateRow() {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun first(): Boolean {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun isAfterLast(): Boolean {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun isBeforeFirst(): Boolean {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun isFirst(): Boolean {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun isLast(): Boolean {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun last(): Boolean {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun previous(): Boolean {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun rowDeleted(): Boolean {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun rowInserted(): Boolean {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun rowUpdated(): Boolean {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun setFetchDirection(direction: Int) {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun setFetchSize(rows: Int) {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun absolute(row: Int): Boolean {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun relative(offset: Int): Boolean {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun getCursorName(): String? {
        throw getUnsupportedException()
    }

    /**
     * Returns the current result set holdability.
     *
     * @return the holdability
     */
    override fun getHoldability(): Int {
        return ResultSet.HOLD_CURSORS_OVER_COMMIT
    }

    /**
     * Returns whether this result set has been closed.
     *
     * @return true if the result set was closed
     */
    override fun isClosed(): Boolean {
        return rows == null && source == null
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun <T> unwrap(iface: Class<T>?): T {
        throw getUnsupportedException()
    }

    /**
     * INTERNAL
     */
    @Throws(SQLException::class)
    override fun isWrapperFor(iface: Class<*>?): Boolean {
        throw getUnsupportedException()
    }

    /**
     * Set the auto-close behavior. If enabled (the default), the result set is
     * closed after reading the last row.
     *
     * @param autoClose the new value
     */
    open fun setAutoClose(autoClose: Boolean) {
        this.autoClose = autoClose
    }

    /**
     * Get the current auto-close behavior.
     *
     * @return the auto-close value
     */
    open fun getAutoClose(): Boolean {
        return autoClose
    }

    /**
     * A simple array implementation,
     * backed by an object array
     */
    class SimpleArray internal constructor(private val value: Array<Any>) : java.sql.Array {
        /**
         * Get the object array.
         *
         * @return the object array
         */
        override fun getArray(): Any {
            return value
        }

        /**
         * INTERNAL
         */
        @Throws(SQLException::class)
        override fun getArray(map: Map<String?, Class<*>?>?): Any {
            throw getUnsupportedException()
        }

        /**
         * INTERNAL
         */
        @Throws(SQLException::class)
        override fun getArray(index: Long, count: Int): Any {
            throw getUnsupportedException()
        }

        /**
         * INTERNAL
         */
        @Throws(SQLException::class)
        override fun getArray(index: Long, count: Int, map: Map<String?, Class<*>?>?): Any {
            throw getUnsupportedException()
        }

        /**
         * Get the base type of this array.
         *
         * @return Types.NULL
         */
        override fun getBaseType(): Int {
            return Types.NULL
        }

        /**
         * Get the base type name of this array.
         *
         * @return "NULL"
         */
        override fun getBaseTypeName(): String {
            return "NULL"
        }

        /**
         * INTERNAL
         */
        @Throws(SQLException::class)
        override fun getResultSet(): ResultSet {
            throw getUnsupportedException()
        }

        /**
         * INTERNAL
         */
        @Throws(SQLException::class)
        override fun getResultSet(map: Map<String?, Class<*>?>?): ResultSet {
            throw getUnsupportedException()
        }

        /**
         * INTERNAL
         */
        @Throws(SQLException::class)
        override fun getResultSet(index: Long, count: Int): ResultSet {
            throw getUnsupportedException()
        }

        /**
         * INTERNAL
         */
        @Throws(SQLException::class)
        override fun getResultSet(
            index: Long, count: Int,
            map: Map<String?, Class<*>?>?,
        ): ResultSet {
            throw getUnsupportedException()
        }

        /**
         * INTERNAL
         */
        override fun free() {
            // nothing to do
        }
    }

}

