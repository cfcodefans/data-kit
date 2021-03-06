package org.h2.value

import org.h2.api.ErrorCode
import org.h2.api.Interval
import org.h2.api.IntervalQualifier
import org.h2.api.TimestampWithTimeZone
import org.h2.engine.SysProperties
import org.h2.message.DbException
import org.h2.util.JSR310
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
    val type: Int = 0,
    /**
     * The data type name.
     */
    val name: String? = null,
    /**
     * The SQL type.
     */
    val sqlType: Int = 0,
    /**
     * How closely the data type maps to the corresponding JDBC SQL type (low is best).
     */
    val sqlTypePos: Int = 0,
    /**
     * The maxium supported precision.
     */
    val maxPrecision: Long = 0L,
    /**
     * The lowest possible scale.
     */
    val minScale: Int = 0,
    /**
     * The highest possible scale.
     */
    val maxScale: Int = 0,
    /**
     * If this is a numeric type.
     */
    val decimal: Boolean = false,
    /**
     * The prefix required for the SQL literal representation.
     */
    val prefix: String? = null,
    /**
     * The suffix required for the SQL literal representation.
     */
    val suffix: String? = null,
    /**
     * The list of parameters used in the column definition.
     */
    val params: String? = null,
    /**
     * If this is an autoincrement type.
     */
    val autoIncrement: Boolean = false,
    /**
     * If this data type is case sensitive.
     */
    val caseSensitive: Boolean = false,
    /**
     * If the precision parameter is supported.
     */
    val supportsPrecision: Boolean = false,
    /**
     * If the scale parameter is supported.
     */
    val supportsScale: Boolean = false,
    /**
     * The default precision.
     */
    val defaultPrecision: Long = 0L,
    /**
     * The default scale.
     */
    val defaultScale: Int = 0,
    /**
     * If this data type should not be listed in the database meta data.
     */
    val hidden: Boolean = false,
) {

    companion object {
        /**
         * Get the data type object for the given value type.
         * @param type the value type
         * @return the data type object
         */
        fun getDataType(type: Int): DataType {
            if (type == Value.UNKNOWN) throw DbException.get(ErrorCode.UNKNOWN_DATA_TYPE_1, "?")
            if (type >= Value.NULL && type < Value.TYPE_COUNT) {
                val dt = TYPES_BY_VALUE_TYPE[type]
                if (dt != null) {
                    return dt
                }
            }
            if (JdbcUtils.customDataTypesHandler != null) {
                val dt: DataType = JdbcUtils.customDataTypesHandler!!.getDataTypeById(type)
                if (dt != null) {
                    return dt
                }
            }
            return TYPES_BY_VALUE_TYPE[Value.NULL]!!
        }

        fun addInterval(type: Int) {
            val qualifier: IntervalQualifier = IntervalQualifier.valueOf(type - Value.INTERVAL_YEAR)
            val dataType: DataType = DataType(prefix = "INTERVAL ",
                suffix = ' ' + qualifier.name,
                supportsPrecision = true,
                defaultPrecision = ValueInterval.DEFAULT_PRECISION.toLong(),
                maxPrecision = ValueInterval.MAXIMUM_PRECISION.toLong())

            if (qualifier.hasSeconds()) {
                dataType.supportsScale = true
                dataType.defaultScale = ValueInterval.DEFAULT_SCALE
            }
        }

        fun add(type: Int, sqlType: Int, dataType: DataType, names: Array<String>) {
            for (i in names.indices) {
                val dt: DataType = DataType(
                    type = dataType.type,
                    sqlType = sqlType,
                    name = names[i],
                    autoIncrement = dataType.autoIncrement,
                    decimal = dataType.decimal,
                    maxPrecision = dataType.maxPrecision,
                    maxScale = dataType.maxScale,
                    minScale = dataType.minScale,
                    params = dataType.params,
                    prefix = dataType.prefix,
                    suffix = dataType.suffix,
                    supportsPrecision = dataType.supportsPrecision,
                    supportsScale = dataType.supportsScale,
                    defaultPrecision = dataType.defaultPrecision,
                    defaultScale = dataType.defaultScale,
                    caseSensitive = dataType.caseSensitive,
                    hidden = i > 0,
                    sqlTypePos = TYPES.filter { it.sqlType == sqlType }.size)
                TYPES_BY_NAME[dt.name!!] = dt
                if (TYPES_BY_VALUE_TYPE[type] == null) TYPES_BY_VALUE_TYPE[type] = dt
                TYPES.add(dt)
            }
        }

        /**
         * Create a width numeric data type without parameters.
         * @param precision precision
         * @param scale scale
         * @param autoInc whether the data type is an auto-increment type
         * @return data type
         */
        fun createNumeric(precision: Long, scale: Int, autoInc: Boolean): DataType =
            DataType(defaultPrecision = precision,
                maxPrecision = precision,
                defaultScale = scale,
                maxScale = scale,
                minScale = scale,
                decimal = true,
                autoIncrement = autoInc)

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
                ValueDecimal.DEFAULT_PRECISION.toLong(),
                ValueDecimal.DEFAULT_SCALE),
            names = arrayOf("DECIMAL", "DEC"))

        fun addNumeric() = add(type = Value.DECIMAL,
            sqlType = Types.NUMERIC,
            dataType = createNumeric(Integer.MAX_VALUE.toLong(),
                ValueDecimal.DEFAULT_PRECISION.toLong(),
                ValueDecimal.DEFAULT_SCALE),
            names = arrayOf("NUMERIC", "NUMBER"))

        /**
         * Convert a SQL type to a value type
         * @param sqlType the SQL type
         * @return the value type
         */
        fun convertSQLTypeToValueType(sqlType: Int): Int {
            return when (sqlType) {
                Types.CHAR, Types.NCHAR -> Value.STRING_FIXED
                Types.VARCHAR, Types.LONGNVARCHAR, Types.NVARCHAR, Types.LONGVARCHAR -> Value.STRING
                Types.NUMERIC, Types.DECIMAL -> Value.DECIMAL
                Types.BIT, Types.BOOLEAN -> Value.BOOLEAN
                Types.INTEGER -> Value.INT
                Types.SMALLINT -> Value.SHORT
                Types.TINYINT -> Value.BYTE
                Types.BIGINT -> Value.LONG
                Types.REAL -> Value.FLOAT
                Types.DOUBLE, Types.FLOAT -> Value.DOUBLE
                Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY -> Value.BYTES
                Types.OTHER, Types.JAVA_OBJECT -> Value.JAVA_OBJECT
                Types.DATE -> Value.DATE
                Types.TIME -> Value.TIME
                Types.TIMESTAMP -> Value.TIMESTAMP
                Types.TIME_WITH_TIMEZONE -> Value.TIME_TZ
                Types.TIMESTAMP_WITH_TIMEZONE -> Value.TIMESTAMP_TZ
                Types.BLOB -> Value.BLOB
                Types.CLOB, Types.NCLOB -> Value.BLOB
                Types.NULL -> Value.NULL
                Types.ARRAY -> Value.ARRAY
                DataType.TYPE_RESULT_SET -> Value.RESULT_SET
                else -> throw DbException.get(
                    ErrorCode.UNKNOWN_DATA_TYPE_1, Integer.toString(sqlType))
            }
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
                    if (JSR310.PRESENT) { // "java.time.OffsetTime";
                        JSR310.OFFSET_TIME!!.getName()
                    } else String::class.java.name // "java.lang.String";
                }
                Value.DATE -> Date::class.java.name // "java.sql.Date";
                Value.TIMESTAMP -> Timestamp::class.java.name  // "java.sql.Timestamp";
                Value.TIMESTAMP_TZ -> {
                    if (SysProperties.RETURN_OFFSET_DATE_TIME && JSR310.PRESENT) { // "java.time.OffsetDateTime";
                        JSR310.OFFSET_DATE_TIME!!.getName()
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
    }
}