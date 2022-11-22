package org.h2.engine

import org.h2.message.Trace
import org.h2.util.HasSQL

/**
 * Represents a role. Roles can be granted to users, and to other roles.
 */
class Role(database: Database,
           id: Int,
           roleName: String,
           val system: Boolean) : RightOwner(database = database, id = id, name = roleName, traceModuleId = Trace.USER) {

    /**
     * Get the CREATE SQL statement for this object.
     *
     * @param ifNotExists true if IF NOT EXISTS should be used
     * @return the SQL statement
     */
    fun getCreateSQL(ifNotExists: Boolean): String? {
        if (system) return null

        val builder = StringBuilder("CREATE ROLE ")
        if (ifNotExists) builder.append("IF NOT EXISTS ")
        return getSQL(builder, HasSQL.DEFAULT_SQL_FLAGS).toString()
    }

    override fun getType(): Int = DbObject.ROLE

    override fun getCreateSQL(): String? = getCreateSQL(false)

    override fun getChildren(): ArrayList<DbObject> = database!!.schemas.values.filter { schema -> schema.owner == this }.let { ArrayList(it) }

    override fun removeChildrenAndResources(session: SessionLocal) {
        for (rightOwner in database!!.getAllUsersAndRoles()) {
            rightOwner.getRightForRole(this)?.let { right -> database!!.removeDatabaseObject(session, right) }
        }
        for (right in database!!.getAllRights()) {
            if (right.getGrantee() === this) database!!.removeDatabaseObject(session, right)
        }
        database!!.removeMeta(session, id)
        invalidate()
    }
}