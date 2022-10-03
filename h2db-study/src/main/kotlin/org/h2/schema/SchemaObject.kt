package org.h2.schema

import org.h2.engine.DbObject

/**
 * Any database object that is stored in a schema.
 *      * Initialize some attributes of this object.
 *
 * @param newSchema the schema
 * @param id the object id
 * @param name the name
 * @param traceModuleId the trace module id
 */
abstract class SchemaObject(val schema: Schema,
                            id: Int,
                            name: String,
                            traceModuleId: Int
) : DbObject(database = schema.database,
    id = id,
    objectName = name,
    traceModuleId = traceModuleId) {


    override fun getSQL(sqlFlags: Int): String = getSQL(StringBuilder(), sqlFlags).toString()

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder {
        schema.getSQL(builder, sqlFlags).append('.')
        return super.getSQL(builder, sqlFlags)
    }

    /**
     * Check whether this is a hidden object that doesn't appear in the meta
     * data and in the script, and is not dropped on DROP ALL OBJECTS.
     *
     * @return true if it is hidden
     */
    open fun isHidden(): Boolean = false
}