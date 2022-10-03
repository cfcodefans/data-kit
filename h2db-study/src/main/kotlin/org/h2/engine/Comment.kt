package org.h2.engine

import org.h2.message.DbException
import org.h2.message.Trace
import org.h2.table.Table
import org.h2.util.HasSQL
import org.h2.util.StringUtils

/**
 * Represents a database object comment.
 */
class Comment(database: Database, id: Int, obj: DbObject) : DbObject(database, id, getKey(obj), Trace.DATABASE) {

    val objectType: Int = obj.getType()
    val quotedObjectName: String = obj.getSQL(HasSQL.DEFAULT_SQL_FLAGS)
    var commentText: String? = null


    override fun getCreateSQLForCopy(table: Table?, quotedName: String?): String? = throw DbException.getInternalError(toString())

    override fun getCreateSQL(): String? = StringBuilder("COMMENT ON ")
        .append(getTypeName(objectType))
        .append(' ')
        .append(quotedObjectName).append(" IS ")
        .also { if (commentText == null) it.append("NULL") else StringUtils.quoteStringSQL(it, commentText) }
        .toString()

    override fun getType(): Int = COMMENT

    override fun removeChildrenAndResources(session: SessionLocal?) = database!!.removeMeta(session, id)

    override fun checkRename(): Unit = throw DbException.getInternalError()


    companion object {
        /**
         * Get the comment key name for the given database object. This key name is
         * used internally to associate the comment to the object.
         *
         * @param obj the object
         * @return the key name
         */
        fun getKey(obj: DbObject): String = StringBuilder(getTypeName(obj.getType()))
            .append(' ')
            .also { obj.getSQL(it, HasSQL.DEFAULT_SQL_FLAGS) }
            .toString()

        private fun getTypeName(type: Int): String = when (type) {
            CONSTANT -> "CONSTANT"
            CONSTRAINT -> "CONSTRAINT"
            FUNCTION_ALIAS -> "ALIAS"
            INDEX -> "INDEX"
            ROLE -> "ROLE"
            SCHEMA -> "SCHEMA"
            SEQUENCE -> "SEQUENCE"
            TABLE_OR_VIEW -> "TABLE"
            TRIGGER -> "TRIGGER"
            USER -> "USER"
            DOMAIN -> "DOMAIN"
            else ->             // not supported by parser, but required when trying to find a
                // comment
                "type$type"
        }
    }

}