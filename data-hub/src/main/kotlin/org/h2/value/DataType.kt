package org.h2.value

/**
 *  This class contains meta data information about data types,
 *  and can convert between Java objects and Values.
 */
open class DataType {

    companion object {
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