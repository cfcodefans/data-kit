package org.h2.engine

import org.h2.command.Parser
import org.h2.message.DbException
import org.h2.message.Trace
import org.h2.table.Table
import org.h2.util.HasSQL
import org.h2.util.ParserUtil

/**
 * A database object such as a table, an index, or a user.
 * Initialize some attributes of this object.
 *
 * @param db the database
 * @param objectId the object id
 * @param name the name
 * @param traceModuleId the trace module id
 */
abstract class DbObject(var database: Database?,
                        var id: Int,
                        var objectName: String?,
                        traceModuleId: Int) : HasSQL {

    companion object {
        /**
         * The object is of the type table or view.
         */
        const val TABLE_OR_VIEW = 0

        /**
         * This object is an index.
         */
        const val INDEX = 1

        /**
         * This object is a user.
         */
        const val USER = 2

        /**
         * This object is a sequence.
         */
        const val SEQUENCE = 3

        /**
         * This object is a trigger.
         */
        const val TRIGGER = 4

        /**
         * This object is a constraint (check constraint, unique constraint, or
         * referential constraint).
         */
        const val CONSTRAINT = 5

        /**
         * This object is a setting.
         */
        const val SETTING = 6

        /**
         * This object is a role.
         */
        const val ROLE = 7

        /**
         * This object is a right.
         */
        const val RIGHT = 8

        /**
         * This object is an alias for a Java function.
         */
        const val FUNCTION_ALIAS = 9

        /**
         * This object is a schema.
         */
        const val SCHEMA = 10

        /**
         * This object is a constant.
         */
        const val CONSTANT = 11

        /**
         * This object is a domain.
         */
        const val DOMAIN = 12

        /**
         * This object is a comment.
         */
        const val COMMENT = 13

        /**
         * This object is a user-defined aggregate function.
         */
        const val AGGREGATE = 14

        /**
         * This object is a synonym.
         */
        const val SYNONYM = 15
    }

    protected var trace: Trace? = database?.getTrace(traceModuleId)
    var modificationId: Long = database!!.modificationMetaId

    /**
     * The comment (if set).
     */
    var comment: String? = null
    var temporary = false

    /**
     * Tell the object that is was modified.
     */
    fun setModified() {
        modificationId = database?.nextModificationMetaId ?: -1
    }

    override fun getSQL(sqlFlags: Int): String = Parser.quoteIdentifier(objectName, sqlFlags)!!

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder = ParserUtil.quoteIdentifier(builder, objectName, sqlFlags)

    /**
     * Get the list of dependent children (for tables, this includes indexes and
     * so on).
     * @return the list of children, or `null`
     */
    open fun getChildren(): ArrayList<DbObject>? = null

    /**
     * Set the main attributes to null to make sure the object is no longer
     * used.
     */
    protected open fun invalidate() {
        if (id == -1) throw DbException.getInternalError()
        setModified()
        id = -1
        database = null
        trace = null
        objectName = null
    }

    fun isValid(): Boolean = id != -1

    /**
     * Build a SQL statement to re-create the object, or to create a copy of the
     * object with a different name or referencing a different table
     *
     * @param table the new table
     * @param quotedName the quoted name
     * @return the SQL statement
     */
    open fun getCreateSQLForCopy(table: Table?, quotedName: String?): String {
        throw DbException.getInternalError(toString())
    }

    /**
     * Construct the CREATE ... SQL statement for this object for meta table.
     *
     * @return the SQL statement
     */
    open fun getCreateSQLForMeta(): String? = getCreateSQL()

    /**
     * Construct the CREATE ... SQL statement for this object.
     *
     * @return the SQL statement
     */
    abstract fun getCreateSQL(): String?

    /**
     * Construct a DROP ... SQL statement for this object.
     *
     * @return the SQL statement
     */
    open fun getDropSQL(): String? = null

    /**
     * Get the object type.
     *
     * @return the object type
     */
    abstract fun getType(): Int

    /**
     * Delete all dependent children objects and resources of this object.
     *
     * @param session the session
     */
    abstract fun removeChildrenAndResources(session: SessionLocal?)

    /**
     * Check if renaming is allowed. Does nothing when allowed.
     */
    open fun checkRename() {
        // Allowed by default
    }

    /**
     * Rename the object.
     *
     * @param newName the new name
     */
    open fun rename(newName: String) {
        checkRename()
        objectName = newName
        setModified()
    }

    override fun toString(): String = "$objectName:$id:${super.toString()}"
}