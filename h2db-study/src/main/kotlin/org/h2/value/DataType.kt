package org.h2.value

import org.h2.api.ErrorCode
import org.h2.api.IntervalQualifier
import org.h2.message.DbException
import org.h2.util.JdbcUtils
import java.sql.Types

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
        val hidden: Boolean = false) {

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
                    defaultPrecision = ValueInterval

            )
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
                dataType = createNumeric(Integer.MAX_VALUE.toLong(), ValueDecimal.DEFAULT_PRECISION.toLong(), ValueDecimal.DEFAULT_SCALE),
                names = arrayOf("DECIMAL", "DEC"))

        fun addNumeric() = add(type = Value.DECIMAL,
                sqlType = Types.NUMERIC,
                dataType = createNumeric(Integer.MAX_VALUE.toLong(), ValueDecimal.DEFAULT_PRECISION.toLong(), ValueDecimal.DEFAULT_SCALE),
                names = arrayOf("NUMERIC", "NUMBER"))

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