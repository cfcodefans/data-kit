package org.h2.engine

import org.h2.message.DbException
import org.h2.message.Trace
import org.h2.schema.Schema
import org.h2.table.Table
import org.h2.util.HasSQL

/**
 * An access right. Rights are regular database objects, but have generated
 * names.
 */
class Right(db: Database, id: Int,
            /**
             * To whom the right is granted.
             */
            private var grantee: RightOwner?,
            /**
             * The granted role, or null if a right was granted.
             */
            private var grantedRole: Role?,
            /**
             * The granted right.
             */
            private var grantedRight: Int = 0,
            /**
             * The object. If the right is global, this is null.
             */
            private var grantedObject: DbObject? = null) : DbObject(database = db, id = id, objectName = "RIGHT_$id", traceModuleId = Trace.USER) {
    companion object {
        /**
         * The right bit mask that means: selecting from a table is allowed.
         */
        const val SELECT = 1

        /**
         * The right bit mask that means: deleting rows from a table is allowed.
         */
        const val DELETE = 2

        /**
         * The right bit mask that means: inserting rows into a table is allowed.
         */
        const val INSERT = 4

        /**
         * The right bit mask that means: updating data is allowed.
         */
        const val UPDATE = 8

        /**
         * The right bit mask that means: create/alter/drop schema is allowed.
         */
        const val ALTER_ANY_SCHEMA = 16

        /**
         * The right bit mask that means: user is a schema owner. This mask isn't
         * used in GRANT / REVOKE statements.
         */
        const val SCHEMA_OWNER = 32

        /**
         * The right bit mask that means: select, insert, update, delete, and update
         * for this object is allowed.
         */
        const val ALL: Int = SELECT or DELETE or INSERT or UPDATE

        private fun appendRight(buff: StringBuilder, right: Int, mask: Int, name: String, comma: Boolean): Boolean {
            if (right and mask != 0) {
                if (comma) buff.append(", ")
                buff.append(name)
                return true
            }
            return comma
        }
    }

    fun getGrantee(): DbObject? = grantee

    fun getRights(): String {
        val buff = StringBuilder()
        if (grantedRight == ALL) {
            buff.append("ALL")
        } else {
            var comma = false
            comma = appendRight(buff, grantedRight, SELECT, "SELECT", comma)
            comma = appendRight(buff, grantedRight, DELETE, "DELETE", comma)
            comma = appendRight(buff, grantedRight, INSERT, "INSERT", comma)
            comma = appendRight(buff, grantedRight, UPDATE, "UPDATE", comma)
            appendRight(buff, grantedRight, ALTER_ANY_SCHEMA, "ALTER ANY SCHEMA", comma)
        }
        return buff.toString()
    }

    override fun getCreateSQLForCopy(table: Table?, quotedName: String?): String = getCreateSQLForCopy(table)

    private fun getCreateSQLForCopy(`object`: DbObject?): String {
        val builder = StringBuilder("GRANT ")
        if (grantedRole != null) {
            grantedRole!!.getSQL(builder, HasSQL.DEFAULT_SQL_FLAGS)
        } else {
            builder.append(getRights())
            if (`object` != null) {
                if (`object` is Schema) {
                    builder.append(" ON SCHEMA ")
                    `object`.getSQL(builder, HasSQL.DEFAULT_SQL_FLAGS)
                } else if (`object` is Table) {
                    builder.append(" ON ")
                    `object`.getSQL(builder, HasSQL.DEFAULT_SQL_FLAGS)
                }
            }
        }
        builder.append(" TO ")
        grantee!!.getSQL(builder, HasSQL.DEFAULT_SQL_FLAGS)
        return builder.toString()
    }


    override fun getCreateSQL(): String? = getCreateSQLForCopy(grantedObject)

    override fun getType(): Int = DbObject.RIGHT

    override fun removeChildrenAndResources(session: SessionLocal) {
        if (grantedRole != null) {
            grantee!!.revokeRole(grantedRole)
        } else {
            grantee!!.revokeRight(grantedObject)
        }
        database!!.removeMeta(session, id)
        grantedRole = null
        grantedObject = null
        grantee = null
        invalidate()
    }

    override fun checkRename(): Unit = throw DbException.getInternalError()

    fun setRightMask(rightMask: Int) {
        grantedRight = rightMask
    }

    fun getRightMask(): Int {
        return grantedRight
    }
}