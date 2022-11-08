package org.h2.engine

import org.h2.api.ErrorCode
import org.h2.message.DbException
import org.h2.message.Trace
import org.h2.schema.Schema
import org.h2.security.SHA256
import org.h2.security.auth.AuthenticationInfo
import org.h2.table.DualTable
import org.h2.table.MetaTable
import org.h2.table.RangeTable
import org.h2.table.Table
import org.h2.util.HasSQL
import org.h2.util.MathUtils
import org.h2.util.StringUtils
import org.h2.util.Utils
import java.util.Arrays

/**
 * Represents a user object.
 */
class User(database: Database,
           id: Int,
           userName: String,
           val systemUser: Boolean) : RightOwner(database = database, id = id, name = userName, traceModuleId = Trace.USER) {

    companion object {
        /**
         * Build the database user starting from authentication informations.
         *
         * @param authenticationInfo authentication info
         * @param database target database
         * @param persistent true if the user will be persisted in the database
         * @return user bean
         */
        fun buildUser(authenticationInfo: AuthenticationInfo, database: Database, persistent: Boolean): User {
            val user = User(database = database,
                id = if (persistent) database.allocateObjectId() else -1,
                userName = authenticationInfo.getFullyQualifiedName(),
                systemUser = false)
            // In case of external authentication fill the password hash with random data
            user.setUserPasswordHash(if (authenticationInfo.realm == null)
                authenticationInfo.connectionInfo.userPasswordHash
            else
                MathUtils.secureRandomBytes(64))

            user.temporary = (!persistent)
            return user
        }
    }

    var salt: ByteArray? = null
    var passwordHash: ByteArray? = null
    var admin: Boolean = false

    /**
     * Set the salt and hash of the password for this user.
     *
     * @param salt the salt
     * @param hash the password hash
     */
    fun setSaltAndHash(salt: ByteArray?, hash: ByteArray?) {
        this.salt = salt
        passwordHash = hash
    }

    /**
     * Set the username password hash. A random salt is generated as well.
     * The parameter is filled with zeros after use.
     *
     * @param userPasswordHash the username password hash
     */
    fun setUserPasswordHash(userPasswordHash: ByteArray?) {
        if (userPasswordHash == null) return

        if (userPasswordHash.isEmpty()) {
            passwordHash = userPasswordHash
            salt = passwordHash
        } else {
            salt = ByteArray(Constants.SALT_LEN)
            MathUtils.randomBytes(salt!!)
            passwordHash = SHA256.getHashWithSalt(userPasswordHash, salt)
        }
    }

    /**
     * Get the CREATE SQL statement for this object.
     *
     * @param password true if the password (actually the salt and hash) should
     * be returned
     * @return the SQL statement
     */
    fun getCreateSQL(password: Boolean): String? {
        val buff = StringBuilder("CREATE USER IF NOT EXISTS ")
        getSQL(buff, HasSQL.DEFAULT_SQL_FLAGS)

        if (comment != null) {
            buff.append(" COMMENT ")
            StringUtils.quoteStringSQL(buff, comment)
        }

        if (password) {
            buff.append(" SALT '")
            StringUtils.convertBytesToHex(buff, salt!!)!!.append("' HASH '")
            StringUtils.convertBytesToHex(buff, passwordHash!!)!!.append('\'')
        } else {
            buff.append(" PASSWORD ''")
        }

        if (admin) buff.append(" ADMIN")
        return buff.toString()
    }

    override fun getCreateSQL(): String? = getCreateSQL(true)

    /**
     * Check the password of this user.
     *
     * @param userPasswordHash the password data (the user password hash)
     * @return true if the user password hash is correct
     */
    fun validateUserPasswordHash(userPasswordHash: ByteArray): Boolean {
        if (userPasswordHash.isEmpty() && passwordHash!!.isEmpty()) return true

        val hash = SHA256.getHashWithSalt(
            if (userPasswordHash.isEmpty())
                SHA256.getKeyPasswordHash(objectName, CharArray(0))
            else
                userPasswordHash,
            salt)
        return Utils.compareSecure(hash, passwordHash)
    }

    /**
     * Checks if this user has admin rights. An exception is thrown if user
     * doesn't have them.
     *
     * @throws DbException if this user is not an admin
     */
    fun checkAdmin() {
        if (!admin) throw DbException.get(ErrorCode.ADMIN_RIGHTS_REQUIRED)
    }

    /**
     * Checks if this user has schema admin rights for every schema. An
     * exception is thrown if user doesn't have them.
     *
     * @throws DbException if this user is not a schema admin
     */
    fun checkSchemaAdmin() {
        if (!hasSchemaRight(null)) throw DbException.get(ErrorCode.ADMIN_RIGHTS_REQUIRED)
    }

    /**
     * Checks if this user has schema owner rights for the specified schema. An
     * exception is thrown if user doesn't have them.
     *
     * @param schema the schema
     * @throws DbException if this user is not a schema owner
     */
    fun checkSchemaOwner(schema: Schema) {
        if (!hasSchemaRight(schema)) throw DbException.get(ErrorCode.NOT_ENOUGH_RIGHTS_FOR_1, schema.traceSQL)
    }

    /**
     * See if this user has owner rights for the specified schema
     *
     * @param schema the schema
     * @return true if the user has the rights
     */
    private fun hasSchemaRight(schema: Schema?): Boolean = if (admin)
        true
    else
        if (database!!.publicRole.isSchemaRightGrantedRecursive(schema))
            true
        else
            isSchemaRightGrantedRecursive(schema)

    /**
     * Checks that this user has the given rights for the specified table.
     *
     * @param table the table
     * @param rightMask the rights required
     * @throws DbException if this user does not have the required rights
     */
    fun checkTableRight(table: Table, rightMask: Int) {
        if (!hasTableRight(table, rightMask)) {
            throw DbException.get(ErrorCode.NOT_ENOUGH_RIGHTS_FOR_1, table.getTraceSQL())
        }
    }

    /**
     * See if this user has the given rights for this database object.
     *
     * @param table the database object, or null for schema-only check
     * @param rightMask the rights required
     * @return true if the user has the rights
     */
    fun hasTableRight(table: Table, rightMask: Int): Boolean {
        if (rightMask != Right.SELECT && !systemUser) {
            table.checkWritingAllowed()
        }

        if (admin) return true

        if (database!!.publicRole.isTableRightGrantedRecursive(table, rightMask)) return true

        // everybody has access to the metadata information
        if (table is MetaTable
            || table is DualTable
            || table is RangeTable) return true

        // derived or function table
        val tableType = table.getTableType() ?: return true

        // the owner has all rights on local temporary tables
        return if (table.temporary && !table.isGlobalTemporary()) true
        else
            isTableRightGrantedRecursive(table, rightMask)
    }


    override fun getType(): Int = USER


    override fun getChildren(): ArrayList<DbObject> {
        return ArrayList<DbObject>(database!!.allRights.filter { r -> r.grantee === this }
                + database!!.allSchemas.filter { s -> s.owner === this })
    }

    override fun removeChildrenAndResources(session: SessionLocal?) {
        for (right in database!!.allRights) {
            if (right.grantee === this) {
                database!!.removeDatabaseObject(session, right)
            }
        }
        database!!.removeMeta(session, id)
        salt = null
        Arrays.fill(passwordHash, 0.toByte())
        passwordHash = null
        invalidate()
    }
}