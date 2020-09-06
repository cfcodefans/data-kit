package org.h2.value

import org.h2.api.ErrorCode
import org.h2.message.DbException
import org.h2.util.JdbcUtils

/**
 *  This class contains meta data information about data types,
 *  and can convert between Java objects and Values.
 */
open class DataType {

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
                val dt: DataType = JdbcUtils.customDataTypesHandler.getDataTypeById(type)
                if (dt != null) {
                    return dt
                }
            }
            return TYPES_BY_VALUE_TYPE[Value.NULL]!!
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