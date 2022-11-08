package org.h2.engine

import org.h2.message.DbException
import org.h2.message.Trace
import org.h2.util.HasSQL

/**
 * A persistent database setting.
 */
class Setting(db: Database,
              id: Int,
              settingName: String) : DbObject(database = db, id = id, objectName = settingName, traceModuleId = Trace.SETTING) {
    var intValue: Int? = 0
    var stringValue: String? = null

    override fun getType(): Int = DbObject.SETTING

    override fun getSQL(sqlFlags: Int): String = objectName!!

    override fun getCreateSQL(): String =
        getSQL(StringBuilder("SET "), HasSQL.DEFAULT_SQL_FLAGS)
            .append(' ')
            .append(stringValue ?: intValue)
            .toString()

    override fun removeChildrenAndResources(session: SessionLocal?) {
        database!!.removeMeta(session, id)
        invalidate()
    }

    override fun checkRename(): Unit = throw DbException.getUnsupportedException("RENAME")
}