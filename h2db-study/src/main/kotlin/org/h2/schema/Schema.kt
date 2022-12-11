package org.h2.schema

import org.h2.api.ErrorCode
import org.h2.constraint.Constraint
import org.h2.engine.Database
import org.h2.engine.DbObject
import org.h2.engine.RightOwner
import org.h2.engine.SessionLocal
import org.h2.index.Index
import org.h2.message.DbException
import org.h2.message.Trace
import org.h2.table.Table
import org.h2.table.TableSynonym
import org.h2.util.HasSQL
import org.h2.util.Utils
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.util.concurrent.ConcurrentHashMap

/**
 * A schema as created by the SQL statement
 * CREATE SCHEMA
 */

/**
 * Create a new schema object.
 *
 * @param database the database
 * @param id the object id
 * @param schemaName the schema name
 * @param owner the owner of the schema
 * @param system if this is a system schema (such a schema can not be dropped)
 */
open class Schema(database: Database,
                  id: Int,
                  schemaName: String,
                  var owner: RightOwner?,
                  val system: Boolean) : DbObject(database = database,
    id = id,
    objectName = schemaName,
    traceModuleId = Trace.SCHEMA) {

    val tablesAndViews: ConcurrentHashMap<String, Table> = database.newConcurrentStringMap()
    private val domains: ConcurrentHashMap<String, Domain> = database.newConcurrentStringMap()
    private val synonyms: ConcurrentHashMap<String, TableSynonym> = database.newConcurrentStringMap()
    private val indexes: ConcurrentHashMap<String, Index> = database.newConcurrentStringMap()
    private val sequences: ConcurrentHashMap<String, Sequence> = database.newConcurrentStringMap()
    private val triggers: ConcurrentHashMap<String, TriggerObject> = database.newConcurrentStringMap()
    private val constraints: ConcurrentHashMap<String, Constraint> = database.newConcurrentStringMap()
    private val constants: ConcurrentHashMap<String, Constant> = database.newConcurrentStringMap()
    private val functionsAndAggregates: ConcurrentHashMap<String, UserDefinedFunction> = database.newConcurrentStringMap()

    /**
     * The set of returned unique names that are not yet stored. It is used to
     * avoid returning the same unique name twice when multiple threads
     * concurrently create objects.
     */
    private val temporaryUniqueNames: HashSet<String> = HashSet<String>()
    private var tableEngineParams: ArrayList<String>? = null

    /**
     * Check if this schema can be dropped. System schemas can not be dropped.
     *
     * @return true if it can be dropped
     */
    open fun canDrop(): Boolean = !system

    override fun getCreateSQL(): String? {
        if (system) return null
        val builder = StringBuilder("CREATE SCHEMA IF NOT EXISTS ")
        getSQL(builder, HasSQL.DEFAULT_SQL_FLAGS).append(" AUTHORIZATION ")
        owner.getSQL(builder, HasSQL.DEFAULT_SQL_FLAGS)
        return builder.toString()
    }

    override fun getType(): Int = DbObject.SCHEMA

    /**
     * Return whether is this schema is empty (does not contain any objects).
     *
     * @return `true` if this schema is empty, `false` otherwise
     */
    open fun isEmpty(): Boolean = (tablesAndViews.isEmpty()
            && domains.isEmpty()
            && synonyms.isEmpty()
            && indexes.isEmpty()
            && sequences.isEmpty()
            && triggers.isEmpty()
            && constraints.isEmpty()
            && constants.isEmpty()
            && functionsAndAggregates.isEmpty())

    override fun getChildren(): java.util.ArrayList<DbObject>? {
        val children = Utils.newSmallArrayList<DbObject>()
        for (right in database!!.getAllRights()) {
            if (right.grantedObject === this) children.add(right)
        }
        return children
    }

    private fun removeChildrenFromMap(session: SessionLocal, map: ConcurrentHashMap<String, out SchemaObject>) {
        if (map.isEmpty()) return
        for (obj in map.values) {
            /*
             * Referential constraints are dropped when unique or PK
             * constraint is dropped, but iterator may return already
             * removed objects in some cases.
             */
            if (obj.isValid()) {
                // Database.removeSchemaObject() removes the object from
                // the map too, but it is safe for ConcurrentHashMap.
                database!!.removeSchemaObject(session, obj)
            }
        }
    }

    override fun removeChildrenAndResources(session: SessionLocal) {
        removeChildrenFromMap(session, triggers)
        removeChildrenFromMap(session, constraints)
        // There can be dependencies between tables e.g. using computed columns,
        // so we might need to loop over them multiple times.
        var modified = true
        while (!tablesAndViews.isEmpty()) {
            var newModified = false
            for (obj in tablesAndViews.values) {
                if (obj.objectName == null) continue
                // Database.removeSchemaObject() removes the object from
                // the map too, but it is safe for ConcurrentHashMap.
                val dependentTable = database!!.getDependentTable(obj, obj)
                if (dependentTable == null) {
                    database!!.removeSchemaObject(session, obj)
                    newModified = true
                } else if (dependentTable.schema !== this) {
                    throw DbException.get(ErrorCode.CANNOT_DROP_2, obj.getTraceSQL(), dependentTable.getTraceSQL())
                } else if (!modified) {
                    dependentTable.removeColumnExpressionsDependencies(session)
                    dependentTable.setModified()
                    database.updateMeta(session, dependentTable)
                }
            }
            modified = newModified
        }
        removeChildrenFromMap(session, domains)
        removeChildrenFromMap(session, indexes)
        removeChildrenFromMap(session, sequences)
        removeChildrenFromMap(session, constants)
        removeChildrenFromMap(session, functionsAndAggregates)

        for (right in database!!.getAllRights()) {
            if (right.grantedObject === this) {
                database!!.removeDatabaseObject(session, right)
            }
        }
        database!!.removeMeta(session!!, id)
        owner = null
        invalidate()
    }

    /**
     * Get all tables and views.
     *
     * @param session the session, `null` to exclude meta tables
     * @return a (possible empty) list of all objects
     */
    open fun getAllTablesAndViews(session: SessionLocal): Collection<Table?> = tablesAndViews.values

    private fun getMap(type: Int): ConcurrentHashMap<String, SchemaObject> = when (type) {
        TABLE_OR_VIEW -> tablesAndViews
        DOMAIN -> domains
        SYNONYM -> synonyms
        SEQUENCE -> sequences
        INDEX -> indexes
        TRIGGER -> triggers
        CONSTRAINT -> constraints
        CONSTANT -> constants
        FUNCTION_ALIAS, AGGREGATE -> functionsAndAggregates
        else -> throw DbException.getInternalError("type=$type")
    }.cast()

    /**
     * Add an object to this schema.
     * This method must not be called within CreateSchemaObject;
     * use Database.addSchemaObject() instead
     *
     * @param obj the object to add
     */
    open fun add(obj: SchemaObject) {
        if (obj.schema !== this) throw DbException.getInternalError("wrong schema")

        val name: String = obj.objectName!!
        val map: ConcurrentHashMap<String, SchemaObject> = getMap(obj.getType())
        if (map.putIfAbsent(name, obj) != null) throw DbException.getInternalError("object already exists: $name")
        freeUniqueName(name)
    }

    /**
     * Release a unique object name.
     *
     * @param name the object name
     */
    open fun freeUniqueName(name: String?) {
        if (name == null) return
        synchronized(temporaryUniqueNames) { temporaryUniqueNames.remove(name) }
    }

    private fun getUniqueName(obj: DbObject, map: Map<String, SchemaObject?>, prefix: String): String {
        val nameBuilder = StringBuilder(prefix)
        val hash = Integer.toHexString(obj.objectName.hashCode())
        synchronized(temporaryUniqueNames) {
            run {
                for (c in hash) {
                    val name = nameBuilder.append(if (c >= 'a') (c.code - 0x20).toChar() else c).toString()
                    if (!map.containsKey(name) && temporaryUniqueNames.add(name)) return name
                }
            }
            val nameLength = nameBuilder.append('_').length
            var i = 0;while (true) {
            val name = nameBuilder.append(i).toString()
            if (!map.containsKey(name) && temporaryUniqueNames.add(name)) return name
            nameBuilder.setLength(nameLength)
            i++
        }
        }
    }
}