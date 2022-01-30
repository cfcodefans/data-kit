package org.h2.value

import org.h2.api.ErrorCode
import org.h2.api.H2Type
import org.h2.api.IntervalQualifier
import org.h2.engine.Constants
import org.h2.engine.Mode
import org.h2.message.DbException
import org.h2.util.StringUtils
import java.sql.JDBCType
import java.sql.ResultSetMetaData
import java.sql.SQLException
import java.sql.SQLType
import java.sql.Types

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
        /**
         * The map of types.
         */
        private val TYPES_BY_NAME = HashMap<String, DataType>(128)

        /**
         * Mapping from Value type numbers to DataType.
         */
        private val TYPES_BY_VALUE_TYPE = arrayOfNulls<DataType>(Value.TYPE_COUNT)

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
                        "INTEGER", "INT", "MEDIUMINT", "INT4", "SIGNED", "SERIAL")
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

            add(Value.ROW, Types.OTHER, DataType(prefix = "ROW(", suffix = ")", params = "NAME DATA_TYPE [,...]"), "ROW")
        }

        fun addInterval(type: Int) {
            val qualifier: IntervalQualifier = IntervalQualifier.valueOf(type - Value.INTERVAL_YEAR)
            val dataType: DataType = DataType(prefix = "INTERVAL ",
                    suffix = " $qualifier",
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
            if (TYPES_BY_VALUE_TYPE[type] == null) TYPES_BY_VALUE_TYPE[type] = dataType
            for (name in names) TYPES_BY_NAME[name] = dataType
        }

        /**
         * Create a width numeric data type without parameters.
         * @param precision precision
         * @param scale scale
         * @return data type
         */
        private fun createNumeric(precision: Long, scale: Int): DataType = DataType(defaultPrecision = precision,
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
                       maxScale: Int): DataType {
            val dataType = DataType(prefix = "$prefix '",
                    suffix = "'",
                    maxPrecision = maxPrecision.toLong(),
                    minPrecision = precision.toLong(),
                    defaultPrecision = precision.toLong())
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
                maxScale = maxPrecision.toInt())

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
                Types.OTHER -> {
                    val type = TYPES_BY_NAME[StringUtils.toUpperEnglish(sqlTypeName)]
                    if (type != null) return type.type
                }
            }
            return convertSQLTypeToValueType(sqlType)
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

        private fun createGeometry(): DataType = DataType(prefix = "'",
                suffix = "'",
                params = "TYPE,SRID",
                maxPrecision = Long.MAX_VALUE,
                defaultPrecision = Long.MAX_VALUE)

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

        /**
         * Convert a value type to a SQL type.
         *
         * @param type the type
         * @return the SQL type
         */
        fun convertTypeToSQLType(type: TypeInfo): Int {
            val valueType: Int = type.valueType
            when (valueType) {
                Value.NUMERIC -> return if (type.extTypeInfo != null) Types.DECIMAL else Types.NUMERIC
                Value.REAL, Value.DOUBLE -> if (type.getDeclaredPrecision() >= 0) return Types.FLOAT
            }
            return getDataType(valueType)!!.sqlType
        }

        /**
         * Check whether the specified column needs the binary representation.
         *
         * @param meta metadata
         * @param column column index
         * @return `true` if column needs the binary representation, `false` otherwise
         * @throws SQLException on SQL exception
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
            else -> throw DbException.get(ErrorCode.UNKNOWN_DATA_TYPE_1, sqlType.toString())
        }

        /**
         * Convert a SQL type to a debug string.
         *
         * @param sqlType the SQL type
         * @return the textual representation
         */
        fun sqlTypeToString(sqlType: SQLType?): String = when (sqlType) {
            null -> "null"
            is JDBCType -> "JDBCType.${sqlType.getName()}"
            is H2Type -> sqlType.toString()
            else -> unknownSqlTypeToString(StringBuilder("/* "), sqlType).append(" */ null").toString()
        }


        private fun unknownSqlTypeToString(builder: StringBuilder, sqlType: SQLType): StringBuilder =
                builder.append(StringUtils.quoteJavaString(sqlType.vendor))
                        .append('/')
                        .append(StringUtils.quoteJavaString(sqlType.name))
                        .append(" [")
                        .append(sqlType.vendorTypeNumber)
                        .append(']')

        /**
         * Get a data type object from a type name.
         *
         * @param s the type name
         * @param mode database mode
         * @return the data type object
         */
        fun getTypeByName(s: String?, mode: Mode): DataType? = mode.typeByNameMap[s] ?: TYPES_BY_NAME[s]

        /**
         * Returns whether columns with the specified data type may have an index.
         *
         * @param type the data type
         * @return whether an index is allowed
         */
        fun isIndexable(type: TypeInfo): Boolean = when (type.valueType) {
            Value.UNKNOWN, Value.NULL, Value.BLOB, Value.CLOB -> false
            Value.ARRAY -> isIndexable(type.extTypeInfo as TypeInfo)
            Value.ROW -> !(type.extTypeInfo as ExtTypeInfoRow).fields.any { !isIndexable(it.value) }
            else -> true
        }

        /**
         * Returns whether values of the specified data types have
         * session-independent compare results.
         *
         * @param type1 the first data type
         * @param type2 the second data type
         * @return are values have session-independent compare results
         */
        fun areStableComparable(type1: TypeInfo, type2: TypeInfo): Boolean {
            val t1: Int = type1.valueType
            val t2: Int = type2.valueType
            return when (t1) {
                Value.UNKNOWN, Value.NULL, Value.BLOB, Value.CLOB, Value.ROW -> false
                Value.DATE, Value.TIMESTAMP -> t2 == Value.DATE || t2 == Value.TIMESTAMP // DATE is equal to TIMESTAMP at midnight
                Value.TIME, Value.TIME_TZ, Value.TIMESTAMP_TZ -> t1 == t2 // Conversions depend on current timestamp and time zone
                Value.ARRAY -> if (t2 != Value.ARRAY) false else areStableComparable(type1.extTypeInfo as TypeInfo, type2.extTypeInfo as TypeInfo)
                else -> when (t2) {
                    Value.UNKNOWN, Value.NULL, Value.BLOB, Value.CLOB, Value.ROW -> false
                    else -> true
                }
            }
        }

        /**
         * Check if the given value type is a date-time type (TIME, DATE, TIMESTAMP,
         * TIMESTAMP_TZ).
         *
         * @param type the value type
         * @return true if the value type is a date-time type
         */
        fun isDateTimeType(type: Int): Boolean = type >= Value.DATE && type <= Value.TIMESTAMP_TZ

        /**
         * Check if the given value type is an interval type.
         *
         * @param type the value type
         * @return true if the value type is an interval type
         */
        fun isIntervalType(type: Int): Boolean = type >= Value.INTERVAL_YEAR && type <= Value.INTERVAL_MINUTE_TO_SECOND

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
        fun isLargeObject(type: Int): Boolean = type == Value.BLOB || type == Value.CLOB


        /**
         * Check if the given value type is a numeric type.
         *
         * @param type the value type
         * @return true if the value type is a numeric type
         */
        fun isNumericType(type: Int): Boolean = type >= Value.TINYINT && type <= Value.DECFLOAT

        /**
         * Check if the given value type is a binary string type.
         *
         * @param type the value type
         * @return true if the value type is a binary string type
         */
        fun isBinaryStringType(type: Int): Boolean = type >= Value.BINARY && type <= Value.BLOB

        /**
         * Check if the given value type is a character string type.
         *
         * @param type the value type
         * @return true if the value type is a character string type
         */
        fun isCharacterStringType(type: Int): Boolean = type >= Value.CHAR && type <= Value.VARCHAR_IGNORECASE

        /**
         * Check if the given value type is a String (VARCHAR,...).
         *
         * @param type the value type
         * @return true if the value type is a String type
         */
        fun isStringType(type: Int): Boolean = type == Value.VARCHAR || type == Value.CHAR || type == Value.VARCHAR_IGNORECASE

        /**
         * Check if the given value type is a binary string type or a compatible
         * special data type such as Java object, UUID, geometry object, or JSON.
         *
         * @param type the value type
         * @return true if the value type is a binary string type or a compatible special data type
         */
        fun isBinaryStringOrSpecialBinaryType(type: Int): Boolean = when (type) {
            Value.VARBINARY, Value.BINARY, Value.BLOB, Value.JAVA_OBJECT, Value.UUID, Value.GEOMETRY, Value.JSON -> true
            else -> false
        }

        /**
         * Check if the given type has total ordering.
         *
         * @param type the value type
         * @return true if the value type has total ordering
         */
        fun hasTotalOrdering(type: Int): Boolean = when (type) {
            Value.BOOLEAN, Value.TINYINT, Value.SMALLINT, Value.INTEGER, Value.BIGINT, Value.DOUBLE, Value.REAL, Value.TIME, Value.DATE, Value.TIMESTAMP, Value.VARBINARY, Value.JAVA_OBJECT, Value.UUID, Value.GEOMETRY, Value.ENUM, Value.INTERVAL_YEAR, Value.INTERVAL_MONTH, Value.INTERVAL_DAY, Value.INTERVAL_HOUR, Value.INTERVAL_MINUTE, Value.INTERVAL_SECOND, Value.INTERVAL_YEAR_TO_MONTH, Value.INTERVAL_DAY_TO_HOUR, Value.INTERVAL_DAY_TO_MINUTE, Value.INTERVAL_DAY_TO_SECOND, Value.INTERVAL_HOUR_TO_MINUTE, Value.INTERVAL_HOUR_TO_SECOND, Value.INTERVAL_MINUTE_TO_SECOND, Value.BINARY -> true
            else -> false
        }

        /**
         * Performs saturated addition of precision values.
         *
         * @param p1 the first summand
         * @param p2 the second summand
         * @return the sum of summands, or [Long.MAX_VALUE] if either argument
         * is negative or sum is out of range
         */
        fun addPrecision(p1: Long, p2: Long): Long {
            val sum = p1 + p2
            return if (p1 or p2 or sum < 0) Long.MAX_VALUE else sum
        }

        /**
         * Get the default value in the form of a Java object for the given Java class.
         *
         * @param clazz the Java class
         * @return the default object
         */
        fun getDefaultForPrimitiveType(clazz: Class<*>): Any? = when (clazz) {
            java.lang.Boolean.TYPE -> java.lang.Boolean.FALSE
            java.lang.Byte.TYPE -> 0.toByte()
            Character.TYPE -> 0.toChar()
            java.lang.Short.TYPE -> 0.toShort()
            Integer.TYPE -> 0
            java.lang.Long.TYPE -> 0L
            java.lang.Float.TYPE -> 0f
            java.lang.Double.TYPE -> 0.0
            else -> throw DbException.getInternalError("primitive=$clazz")
        }
    }
}