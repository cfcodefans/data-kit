package org.h2.engine

import org.h2.api.ErrorCode
import org.h2.message.DbException
import org.h2.schema.Schema
import org.h2.table.Table
import org.h2.util.StringUtils

/**
 * A right owner (sometimes called principal).
 */
abstract class RightOwner(database: Database,
                          id: Int,
                          name: String,
                          traceModuleId: Int) : DbObject(database = database,
    id = id,
    objectName = StringUtils.toUpperEnglish(name),
    traceModuleId = traceModuleId) {

    /**
     * The map of granted roles.
     */
    private var grantedRoles: HashMap<Role, Right>? = null

    /**
     * The map of granted rights.
     */
    private var grantedRights: HashMap<DbObject?, Right>? = null

    override fun rename(newName: String) = super.rename(StringUtils.toUpperEnglish(newName))

    /**
     * Check if a role has been granted for this right owner.
     *
     * @param grantedRole the role
     * @return true if the role has been granted
     */
    open fun isRoleGranted(grantedRole: Role): Boolean = (grantedRole == this)
            || (grantedRoles?.keys
        ?.any { role -> role == grantedRole || role.isRoleGranted(grantedRole) }
        ?: false)

    /**
     * Checks if a right is already granted to this object or to objects that
     * were granted to this object. The rights of schemas will be valid for
     * every each table in the related schema. The ALTER ANY SCHEMA right gives
     * all rights to all tables.
     *
     * @param table
     * the table to check
     * @param rightMask
     * the right mask to check
     * @return true if the right was already granted
     */
    fun isTableRightGrantedRecursive(table: Table, rightMask: Int): Boolean {
        val schema: Schema = table.schema
        if (schema.owner === this) return true

        if (grantedRights?.let {
                (it[null]?.let { right: Right -> (right.getRightMask() and Right.ALTER_ANY_SCHEMA) == Right.ALTER_ANY_SCHEMA } == true)
                        || (it[schema]?.let { right -> right.getRightMask() and rightMask == rightMask } == true)
                        || (it[table]?.let { right -> right.getRightMask() and rightMask == rightMask } == true)
            } == true) return true

        if (grantedRoles?.keys?.any { role -> role.isTableRightGrantedRecursive(table, rightMask) } == true)
            return true

        return false
    }

    /**
     * Checks if a schema owner right is already granted to this object or to
     * objects that were granted to this object. The ALTER ANY SCHEMA right
     * gives rights to all schemas.
     *
     * @param schema
     * the schema to check, or `null` to check for ALTER ANY
     * SCHEMA right only
     * @return true if the right was already granted
     */
    fun isSchemaRightGrantedRecursive(schema: Schema?): Boolean {
        if (schema != null && schema.owner === this)
            return true

        if (grantedRights?.get(null)?.let { right -> right.getRightMask() and Right.ALTER_ANY_SCHEMA == Right.ALTER_ANY_SCHEMA } == true)
            return true

        if (grantedRoles?.keys?.any { role -> role.isSchemaRightGrantedRecursive(schema) } == true)
            return true

        return false
    }

    /**
     * Grant a right for the given table. Only one right object per table is
     * supported.
     *
     * @param object the object (table or schema)
     * @param right the right
     */
    open fun grantRight(`object`: DbObject?, right: Right?) {
        if (grantedRights == null) grantedRights = HashMap()

        grantedRights!![`object`] = right!!
    }

    /**
     * Revoke the right for the given object (table or schema).
     *
     * @param object the object
     */
    open fun revokeRight(`object`: DbObject?) {
        if (grantedRights == null) return

        grantedRights!!.remove(`object`)
        if (grantedRights!!.size == 0) grantedRights = null
    }

    /**
     * Grant a role to this object.
     *
     * @param role the role
     * @param right the right to grant
     */
    open fun grantRole(role: Role?, right: Right?) {
        if (grantedRoles == null) grantedRoles = HashMap()

        grantedRoles!![role!!] = right!!
    }

    /**
     * Remove the right for the given role.
     *
     * @param role the role to revoke
     */
    open fun revokeRole(role: Role?) {
        if (grantedRoles == null) return

        val right = grantedRoles!![role] ?: return

        grantedRoles!!.remove(role)
        if (grantedRoles!!.size == 0) grantedRoles = null
    }

    /**
     * Remove all the temporary rights granted on roles
     */
    open fun revokeTemporaryRightsOnRoles() {
        if (grantedRoles == null) return

        grantedRoles!!.entries
            .filter { en -> en.value.temporary || en.value.isValid().not() }
            .map { it.key }
            .toList()
            .forEach { revokeRole(it) }
    }

    /**
     * Get the 'grant schema' right of this object.
     *
     * @param object the granted object (table or schema)
     * @return the right or null if the right has not been granted
     */
    open fun getRightForObject(`object`: DbObject?): Right? = grantedRights?.get(`object`)

    /**
     * Get the 'grant role' right of this object.
     *
     * @param role the granted role
     * @return the right or null if the right has not been granted
     */
    open fun getRightForRole(role: Role?): Right? = grantedRoles?.get(role)

    /**
     * Check that this right owner does not own any schema. An exception is
     * thrown if it owns one or more schemas.
     *
     * @throws DbException if this right owner owns a schema
     */
    fun checkOwnsNoSchemas() {
        for (s in database!!.allSchemas) {
            if (this === s.owner) throw DbException.get(ErrorCode.CANNOT_DROP_2, objectName!!, s.name)
        }
    }
}