package org.h2.value

import org.h2.api.ErrorCode
import org.h2.api.H2Type
import org.h2.api.Interval
import org.h2.api.IntervalQualifier
import org.h2.api.TimestampWithTimeZone
import org.h2.engine.Constants
import org.h2.engine.SysProperties
import org.h2.message.DbException
import org.h2.util.JSR310Utils
import org.h2.util.JdbcUtils
import java.math.BigDecimal
import java.sql.*

/**
 *  This class contains meta data information about data types,
 *  and can convert between Java objects and Values.
 */
open class DataType(
        /**
         * The value type of this data type.
         */
        var type: Int = 0,
        /**
         * The SQL type.
         */
        var sqlType: Int = 0,
        /**
         * The minimum supported precision.
         */
        var minPrecision: Long = 0,
        /**
         * The maxium supported precision.
         */
        var maxPrecision: Long = 0L,
        /**
         * The lowest possible scale.
         */
        var minScale: Int = 0,
        /**
         * The highest possible scale.
         */
        var maxScale: Int = 0,
        /**
         * The prefix required for the SQL literal representation.
         */
        var prefix: String? = null,
        /**
         * The suffix required for the SQL literal representation.
         */
        var suffix: String? = null,
        /**
         * The list of parameters used in the column definition.
         */
        var params: String? = null,
        /**
         * If this data type is case sensitive.
         */
        var caseSensitive: Boolean = false,
        /**
         * If the precision parameter is supported.
         */
        var supportsPrecision: Boolean = false,
        /**
         * If the scale parameter is supported.
         */
        var supportsScale: Boolean = false,
        /**
         * The default precision.
         */
        var defaultPrecision: Long = 0L,
        /**
         * The default scale.
         */
        var defaultScale: Int = 0,
        /**
         * If precision and scale have non-standard default values.
         */
        var specialPrecisionScale: Boolean = false) {

    companion object {
        init {
            DataType(defaultPrecision = ValueNull.PRECISION.toLong(),
                    maxPrecision = ValueNull.PRECISION.toLong(),
                    minPrecision = ValueNull.PRECISION.toLong()).let { dataType ->
                add(Value.NULL, Types.NULL, dataType, "NULL")
                add(Value.CHAR, Types.CHAR, createString(true, true),
                        "CHARACTER", "CHAR", "NCHAR", "NATIONAL CHARACTER", "NATIONAL CHAR")
                add(Value.VARCHAR, Types.VARCHAR, createString(true, false),
                        "CHARACTER VARYING", "VARCHAR", "CHAR VARYING",
                        "NCHAR VARYING", "NATIONAL CHARACTER VARYING", "NATIONAL CHAR VARYING",
                        "VARCHAR2", "NVARCHAR", "NVARCHAR2",
                        "VARCHAR_CASESENSITIVE", "TID",
                        "LONGVARCHAR", "LONGNVARCHAR")
                add(Value.CLOB, Types.CLOB, createLob(true),
                        "CHARACTER LARGE OBJECT", "CLOB", "CHAR LARGE OBJECT", "TINYTEXT", "TEXT", "MEDIUMTEXT",
                        "LONGTEXT", "NTEXT", "NCLOB", "NCHAR LARGE OBJECT", "NATIONAL CHARACTER LARGE OBJECT")
                add(Value.VARCHAR_IGNORECASE, Types.VARCHAR, createString(false, false), "VARCHAR_IGNORECASE")
                add(Value.BINARY, Types.BINARY, createBinary(true), "BINARY")
                add(Value.VARBINARY, Types.VARBINARY, createBinary(false),
                        "BINARY VARYING", "VARBINARY", "RAW", "BYTEA", "LONG RAW", "LONGVARBINARY")
                add(Value.BLOB, Types.BLOB, DataType.createLob(false),
                        "BINARY LARGE OBJECT", "BLOB", "TINYBLOB", "MEDIUMBLOB", "LONGBLOB", "IMAGE")
                add(Value.BOOLEAN, Types.BOOLEAN, createNumeric(ValueBoolean.PRECISION.toLong(), 0), "BOOLEAN", "BIT", "BOOL")
                add(Value.TINYINT, Types.TINYINT, createNumeric(ValueTinyint.PRECISION.toLong(), 0), "TINYINT")
                add(Value.SMALLINT, Types.SMALLINT, createNumeric(ValueSmallint.PRECISION.toLong(), 0), "SMALLINT", "INT2")
                add(Value.INTEGER, Types.INTEGER, createNumeric(ValueInteger.PRECISION.toLong(), 0),
                        "INTEGER", "INT", "MEDIUMINT", "INT4", "SIGNED", "SERIAL"
                )
                add(Value.BIGINT, Types.BIGINT, createNumeric(ValueBigint.PRECISION.toLong(), 0),
                        "BIGINT", "INT8", "LONG", "IDENTITY", "BIGSERIAL")
            }

            DataType(minPrecision = 1,
                    defaultPrecision = Constants.MAX_NUMERIC_PRECISION.toLong(),
                    maxPrecision = Constants.MAX_NUMERIC_PRECISION.toLong(),
                    defaultScale = ValueNumeric.DEFAULT_SCALE,
                    maxScale = ValueNumeric.MAXIMUM_SCALE,
                    minScale = 0,
                    params = "PRECISION,SCALE",
                    supportsPrecision = true,
                    supportsScale = true).let { dataType ->
                add(Value.NUMERIC,
                        Types.NUMERIC,
                        dataType,
                        "NUMERIC", "DECIMAL", "DEC", "NUMBER")
                add(Value.REAL,
                        Types.REAL,
                        createNumeric(ValueReal.PRECISION.toLong(), 0),
                        "REAL", "FLOAT4")
                add(Value.DOUBLE,
                        Types.DOUBLE,
                        createNumeric(ValueDouble.PRECISION.toLong(), 0),
                        "DOUBLE PRECISION", "DOUBLE", "FLOAT8")
                add(Value.DOUBLE, Types.FLOAT, createNumeric(ValueDouble.PRECISION.toLong(), 0), "FLOAT")
            }

            DataType(minPrecision = 1,
                    defaultPrecision = Constants.MAX_NUMERIC_PRECISION.toLong(),
                    maxPrecision = Constants.MAX_NUMERIC_PRECISION.toLong(),
                    params = "PRECISION",
                    supportsPrecision = true).let { dataType ->

                add(Value.DECFLOAT, Types.NUMERIC, dataType, "DECFLOAT")
                add(Value.DATE,
                        Types.DATE,
                        createDate(ValueDate.PRECISION, ValueDate.PRECISION, "DATE", false, 0, 0)!!,
                        "DATE")
                add(Value.TIME,
                        Types.TIME,
                        createDate(ValueTime.MAXIMUM_PRECISION, ValueTime.DEFAULT_PRECISION, "TIME", true, ValueTime.DEFAULT_SCALE, ValueTime.MAXIMUM_SCALE)!!,
                        "TIME", "TIME WITHOUT TIME ZONE")
                add(Value.TIME_TZ, Types.TIME_WITH_TIMEZONE, createDate(ValueTimeTimeZone.MAXIMUM_PRECISION, ValueTimeTimeZone.DEFAULT_PRECISION,
                        "TIME WITH TIME ZONE", true, ValueTime.DEFAULT_SCALE, ValueTime.MAXIMUM_SCALE)!!,
                        "TIME WITH TIME ZONE")
                add(Value.TIMESTAMP, Types.TIMESTAMP,
                        createDate(ValueTimestamp.MAXIMUM_PRECISION, ValueTimestamp.DEFAULT_PRECISION,
                                "TIMESTAMP", true, ValueTimestamp.DEFAULT_SCALE, ValueTimestamp.MAXIMUM_SCALE)!!,
                        "TIMESTAMP", "TIMESTAMP WITHOUT TIME ZONE", "DATETIME", "DATETIME2", "SMALLDATETIME")
                add(Value.TIMESTAMP_TZ, Types.TIMESTAMP_WITH_TIMEZONE,
                        createDate(ValueTimestampTimeZone.MAXIMUM_PRECISION, ValueTimestampTimeZone.DEFAULT_PRECISION,
                                "TIMESTAMP WITH TIME ZONE", true, ValueTimestamp.DEFAULT_SCALE, ValueTimestamp.MAXIMUM_SCALE)!!,
                        "TIMESTAMP WITH TIME ZONE")
                for (i in Value.INTERVAL_YEAR..Value.INTERVAL_MINUTE_TO_SECOND) {
                    addInterval(i)
                }
                add(Value.JAVA_OBJECT, Types.JAVA_OBJECT, createBinary(false), "JAVA_OBJECT", "OBJECT", "OTHER")
            }

            createString(false, false).let { dataType ->
                dataType.supportsPrecision = false
                dataType.params = "ELEMENT [,...]"
                add(Value.ENUM, Types.OTHER, dataType, "ENUM")
                add(Value.GEOMETRY, Types.OTHER, DataType.createGeometry(), "GEOMETRY")
                add(Value.JSON, Types.OTHER, createString(true, false, "JSON '", "'"), "JSON")
            }
            DataType().let { dataType ->
                dataType.prefix = "'".also { dataType.suffix = it }
                dataType.defaultPrecision = ValueUuid.PRECISION.also { dataType.minPrecision = it.toLong() }.also { dataType.maxPrecision = it.toLong() }.toLong()
                add(Value.UUID, Types.BINARY, dataType, "UUID")
            }
            DataType().let { dataType ->
                dataType.prefix = "ARRAY["
                dataType.suffix = "]"
                dataType.params = "CARDINALITY"
                dataType.supportsPrecision = true
                dataType.defaultPrecision = Constants.MAX_ARRAY_CARDINALITY.also { dataType.maxPrecision = it.toLong() }.toLong()
                add(Value.ARRAY, Types.ARRAY, dataType, "ARRAY")
            }

            DataType(prefix = "ROW(", suffix = ")", params = "NAME DATA_TYPE [,...]").let { dataType ->
                add(Value.ROW, Types.OTHER, dataType, "ROW")
            }
        }

        /**
         * Get the data type object for the given value type.
         *
         * @param type the value type
         * @return the data type object
         */
        fun getDataType(type: Int): DataType? {
            if (type == Value.UNKNOWN) throw DbException.get(ErrorCode.UNKNOWN_DATA_TYPE_1, "?")
            return if (type >= Value.NULL && type < Value.TYPE_COUNT) TYPES_BY_VALUE_TYPE[type] else TYPES_BY_VALUE_TYPE[Value.NULL]
        }

        fun addInterval(type: Int) {
            val qualifier: IntervalQualifier = IntervalQualifier.valueOf(type - Value.INTERVAL_YEAR)
            val dataType: DataType = DataType(prefix = "INTERVAL ",
                    suffix = ' ' + qualifier.toString(),
                    supportsPrecision = true,
                    defaultPrecision = ValueInterval.DEFAULT_PRECISION.toLong(),
                    minPrecision = 1,
                    maxPrecision = ValueInterval.MAXIMUM_PRECISION.toLong())

            if (qualifier.hasSeconds()) {
                dataType.supportsScale = true
                dataType.defaultScale = ValueInterval.DEFAULT_SCALE
                dataType.maxScale = ValueInterval.MAXIMUM_SCALE
                dataType.params = "PRECISION,SCALE"
            } else {
                dataType.params = "PRECISION"
            }
            add(type, Types.OTHER, dataType, "INTERVAL ${qualifier.toString()}".intern())
        }

        fun add(type: Int, sqlType: Int, dataType: DataType, vararg names: String) {
            dataType.type = type
            dataType.sqlType = sqlType
            if (TYPES_BY_VALUE_TYPE[type] == null) {
                TYPES_BY_VALUE_TYPE[type] = dataType
            }
            for (name in names) {
                TYPES_BY_NAME[name] = dataType
            }
        }

        /**
         * Create a width numeric data type without parameters.
         * @param precision precision
         * @param scale scale
         * @param autoInc whether the data type is an auto-increment type
         * @return data type
         */
        fun createNumeric(precision: Long, scale: Int): DataType =
                DataType(defaultPrecision = precision,
                        minPrecision = precision,
                        maxPrecision = precision,
                        defaultScale = scale,
                        maxScale = scale,
                        minScale = scale)

        /**
         * Create a date-time data type.
         *
         * @param maxPrecision maximum supported precision
         * @param precision default precision
         * @param prefix the prefix for SQL literal representation
         * @param supportsScale whether the scale parameter is supported
         * @param scale default scale
         * @param maxScale highest possible scale
         * @return data type
         */
        fun createDate(maxPrecision: Int,
                       precision: Int,
                       prefix: String,
                       supportsScale: Boolean,
                       scale: Int,
                       maxScale: Int): DataType? {
            val dataType = DataType(prefix = "$prefix '",
                    suffix = "'",
                    maxPrecision = maxPrecision.toLong(),
                    minPrecision = precision.toLong(),
                    defaultPrecision = dataType.minPrecision)
            if (supportsScale) {
                dataType.params = "SCALE"
                dataType.supportsScale = true
                dataType.maxScale = maxScale
                dataType.defaultScale = scale
            }
            return dataType
        }

        /**
         * Create a numeric data type.
         * @param maxPrecision maximum supported precision.
         * @param defaultPrecision default precision
         * @param defaultScale default scale
         * @return data type
         */
        fun createNumeric(maxPrecision: Long, defaultPrecision: Long, defaultScale: Int): DataType = DataType(
                maxPrecision = maxPrecision,
                defaultPrecision = defaultPrecision,
                defaultScale = defaultScale,
                params = "PRECISION,SCALE",
                supportsPrecision = true,
                supportsScale = true,
                maxScale = maxPrecision.toInt(),
                decimal = true)

        fun addDecimal() = add(type = Value.DECIMAL,
                sqlType = Types.DECIMAL,
                dataType = createNumeric(Integer.MAX_VALUE.toLong(),
                        ValueDecfloat.DEFAULT_PRECISION.toLong(),
                        ValueDecfloat.DEFAULT_SCALE),
                names = arrayOf("DECIMAL", "DEC"))

        fun addNumeric() = add(type = Value.DECIMAL,
                sqlType = Types.NUMERIC,
                dataType = createNumeric(Integer.MAX_VALUE.toLong(),
                        ValueDecfloat.DEFAULT_PRECISION.toLong(),
                        ValueDecfloat.DEFAULT_SCALE),
                names = arrayOf("NUMERIC", "NUMBER"))

        /**
         * Get the SQL type from the result set meta data for the given column. This
         * method uses the SQL type and type name.
         *
         * @param meta the meta data
         * @param columnIndex the column index (1, 2,...)
         * @return the value type
         * @throws SQLException on failure
         */
        @Throws(SQLException::class)
        fun getValueTypeFromResultSet(meta: ResultSetMetaData, columnIndex: Int): Int {
            return convertSQLTypeToValueType(
                    meta.getColumnType(columnIndex),
                    meta.getColumnTypeName(columnIndex))
        }

        private fun createString(caseSensitive: Boolean, fixedLength: Boolean): DataType {
            return createString(caseSensitive, fixedLength, "'", "'")
        }

        private fun createBinary(fixedLength: Boolean): DataType {
            return createString(false, fixedLength, "X'", "'")
        }

        private fun createString(caseSensitive: Boolean, fixedLength: Boolean, prefix: String, suffix: String): DataType {
            return DataType(prefix = prefix,
                    suffix = suffix,
                    params = "LENGTH",
                    caseSensitive = caseSensitive,
                    supportsPrecision = true,
                    minPrecision = 1,
                    maxPrecision = Constants.MAX_STRING_LENGTH.toLong(),
                    defaultPrecision = if (fixedLength) 1 else Constants.MAX_STRING_LENGTH.toLong())
        }

        private fun createLob(clob: Boolean): DataType {
            val t = if (clob) createString(true, false) else createBinary(false)
            t.maxPrecision = Long.MAX_VALUE
            t.defaultPrecision = Long.MAX_VALUE
            return t
        }

        /**
         * Check whether the specified column needs the binary representation.
         *
         * @param meta
         * metadata
         * @param column
         * column index
         * @return `true` if column needs the binary representation,
         * `false` otherwise
         * @throws SQLException
         * on SQL exception
         */
        @Throws(SQLException::class)
        fun isBinaryColumn(meta: ResultSetMetaData, column: Int): Boolean = when (meta.getColumnType(column)) {
            Types.BINARY -> meta.getColumnTypeName(column) != "UUID"
            Types.LONGVARBINARY, Types.VARBINARY, Types.JAVA_OBJECT, Types.BLOB -> true
            else -> false
        }

        /**
         * Convert a SQL type to a value type.
         *
         * @param sqlType the SQL type
         * @return the value type
         */
        fun convertSQLTypeToValueType(sqlType: SQLType?): Int = when (sqlType) {
            is H2Type -> sqlType.getVendorTypeNumber()
            is JDBCType -> convertSQLTypeToValueType(sqlType.getVendorTypeNumber())
            else -> throw DbException.get(ErrorCode.UNKNOWN_DATA_TYPE_1, if (sqlType == null) "<null>" else DataType.unknownSqlTypeToString(StringBuilder(), sqlType).toString())
        }

        /**
         * Convert a SQL type to a value type
         * @param sqlType the SQL type
         * @return the value type
         */
        fun convertSQLTypeToValueType(sqlType: Int): Int = when (sqlType) {
            Types.CHAR, Types.NCHAR -> Value.CHAR
            Types.VARCHAR, Types.LONGVARCHAR, Types.NVARCHAR, Types.LONGNVARCHAR -> Value.VARCHAR
            Types.NUMERIC, Types.DECIMAL -> Value.NUMERIC
            Types.BIT, Types.BOOLEAN -> Value.BOOLEAN
            Types.INTEGER -> Value.INTEGER
            Types.SMALLINT -> Value.SMALLINT
            Types.TINYINT -> Value.TINYINT
            Types.BIGINT -> Value.BIGINT
            Types.REAL -> Value.REAL
            Types.DOUBLE, Types.FLOAT -> Value.DOUBLE
            Types.BINARY -> Value.BINARY
            Types.VARBINARY, Types.LONGVARBINARY -> Value.VARBINARY
            Types.OTHER -> Value.UNKNOWN
            Types.JAVA_OBJECT -> Value.JAVA_OBJECT
            Types.DATE -> Value.DATE
            Types.TIME -> Value.TIME
            Types.TIMESTAMP -> Value.TIMESTAMP
            Types.TIME_WITH_TIMEZONE -> Value.TIME_TZ
            Types.TIMESTAMP_WITH_TIMEZONE -> Value.TIMESTAMP_TZ
            Types.BLOB -> Value.BLOB
            Types.CLOB, Types.NCLOB -> Value.CLOB
            Types.NULL -> Value.NULL
            Types.ARRAY -> Value.ARRAY
            else -> throw DbException.get(
                    ErrorCode.UNKNOWN_DATA_TYPE_1, Integer.toString(sqlType))
        }

        /**
         * Get the name of the Java class for the given value type.
         *
         * @param type the value type
         * @param forResultSet return mapping for result set
         * @return the class name
         */
        fun getTypeClassName(type: Int, forResultSet: Boolean): String? {
            return when (type) {
                Value.BOOLEAN -> Boolean::class.java.name   // "java.lang.Boolean";
                Value.BYTE -> {
                    if (forResultSet && !SysProperties.OLD_RESULT_SET_GET_OBJECT) {
                        Int::class.java.name // "java.lang.Integer";
                    } else Byte::class.java.name  // "java.lang.Byte";
                }
                Value.SHORT -> {
                    if (forResultSet && !SysProperties.OLD_RESULT_SET_GET_OBJECT) { // "java.lang.Integer";
                        Int::class.java.name
                    } else Short::class.java.name // "java.lang.Short";
                }
                Value.INT -> Int::class.java.name // "java.lang.Integer";
                Value.LONG -> Long::class.java.name           // "java.lang.Long";
                Value.DECIMAL -> BigDecimal::class.java.name          // "java.math.BigDecimal";
                Value.TIME -> Time::class.java.name       // "java.sql.Time";

                Value.TIME_TZ -> {
                    if (JSR310Utils.PRESENT) { // "java.time.OffsetTime";
                        JSR310Utils.OFFSET_TIME!!.getName()
                    } else String::class.java.name // "java.lang.String";
                }
                Value.DATE -> Date::class.java.name // "java.sql.Date";
                Value.TIMESTAMP -> Timestamp::class.java.name  // "java.sql.Timestamp";
                Value.TIMESTAMP_TZ -> {
                    if (SysProperties.RETURN_OFFSET_DATE_TIME && JSR310Utils.PRESENT) { // "java.time.OffsetDateTime";
                        JSR310Utils.OFFSET_DATE_TIME!!.getName()
                    } else TimestampWithTimeZone::class.java.name // "org.h2.api.TimestampWithTimeZone";
                }
                Value.BYTES, Value.UUID, Value.JSON -> ByteArray::class.java.name  // "[B", not "byte[]";
                Value.STRING, Value.STRING_IGNORECASE, Value.STRING_FIXED, Value.ENUM -> String::class.java.name   // "java.lang.String";
                Value.BLOB -> Blob::class.java.name // "java.sql.Blob";
                Value.CLOB -> Clob::class.java.name // "java.sql.Clob";
                Value.DOUBLE -> Double::class.java.name  // "java.lang.Double";
                Value.FLOAT -> Float::class.java.name // "java.lang.Float";
                Value.NULL -> null
                Value.JAVA_OBJECT -> Any::class.java.name      // "java.lang.Object";
                Value.UNKNOWN -> Any::class.java.name // anything
                Value.ARRAY -> java.sql.Array::class.java.name
                Value.RESULT_SET -> ResultSet::class.java.name
                Value.GEOMETRY -> if (GEOMETRY_CLASS != null) GEOMETRY_CLASS_NAME else String::class.java.name
                Value.INTERVAL_YEAR, Value.INTERVAL_MONTH, Value.INTERVAL_DAY, Value.INTERVAL_HOUR, Value.INTERVAL_MINUTE, Value.INTERVAL_SECOND, Value.INTERVAL_YEAR_TO_MONTH, Value.INTERVAL_DAY_TO_HOUR, Value.INTERVAL_DAY_TO_MINUTE, Value.INTERVAL_DAY_TO_SECOND, Value.INTERVAL_HOUR_TO_MINUTE, Value.INTERVAL_HOUR_TO_SECOND, Value.INTERVAL_MINUTE_TO_SECOND ->             // "org.h2.api.Interval"
                    Interval::class.java.name
                else -> return JdbcUtils.customDataTypesHandler?.getDataTypeClassName(type)
                        ?: throw DbException.throwInternalError("type=$type")
            }
        }

        /**
         * Get the SQL type from the result set meta data for the given column. This
         * method uses the SQL type and type name.
         *
         * @param meta the meta data
         * @param columnIndex the column index (1, 2,...)
         * @return the value type
         */
        @Throws(SQLException::class)
        fun getValueTypeFromResultSet(meta: ResultSetMetaData, columnIndex: Int): Int {
            return convertSQLTypeToValueType(meta.getColumnType(columnIndex), meta.getColumnTypeName(columnIndex))
        }

        /**
         * Convert a SQL type to a value type using SQL type name, in order to
         * manage SQL type extension mechanism.
         *
         * @param sqlType the SQL type
         * @param sqlTypeName the SQL type name
         * @return the value type
         */
        fun convertSQLTypeToValueType(sqlType: Int, sqlTypeName: String): Int {
            when (sqlType) {
                Types.BINARY -> if (sqlTypeName.equals("UUID", ignoreCase = true)) return Value.UUID
                Types.OTHER, Types.JAVA_OBJECT -> when {
                    sqlTypeName.equals("geometry", ignoreCase = true) -> return Value.GEOMETRY
                    sqlTypeName.equals("json", ignoreCase = true) -> return Value.JSON
                }
            }
            return convertSQLTypeToValueType(sqlType)
        }

        /**
         * This constant is used to represent the type of a ResultSet. There is
         * no equivalent java.sql.Types value, but Oracle uses it to represent
         * a ResultSet (OracleTypes.CURSOR = -10)
         */
        const val TYPE_RESULT_SET: Int = -10

        /**
         * The Geometry class. This object is null if the jts jar file
         * is not in the classpath
         */
        var GEOMETRY_CLASS: Class<*>? = null
        const val GEOMETRY_CLASS_NAME: String = "org.locationtech.jts.geom.Geometry"

        /**
         * The list of types. An ArrayList so that Tomcat doesn't set it to null
         * when clearing references.
         */
        val TYPES: ArrayList<DataType> = ArrayList(96)
        val TYPES_BY_NAME: HashMap<String, DataType> = HashMap(128)

        /**
         * Mapping from value type numbers to DataType.
         */
        val TYPES_BY_VALUE_TYPE: Array<DataType?> = arrayOfNulls<DataType?>(Value.TYPE_COUNT)

        /**
         * Check if the given value type is a year-month interval type.
         *
         * @param type the value type
         * @return true if the value type is a year-month interval type
         */
        fun isYearMonthIntervalType(type: Int): Boolean = type == Value.INTERVAL_YEAR
                || type == Value.INTERVAL_MONTH
                || type == Value.INTERVAL_YEAR_TO_MONTH

        /**
         * Check if the given value type is a large object (BLOB or CLOB).
         *
         * @param type the value type
         * @return true if the value type is a lob type
         */
        fun isLargeObject(type: Int): Boolean {
            return type == Value.BLOB || type == Value.CLOB
        }

    }
}