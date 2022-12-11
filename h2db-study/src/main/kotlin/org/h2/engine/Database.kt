package org.h2.engine

import jdk.internal.org.jline.utils.Colors.s
import org.h2.api.DatabaseEventListener
import org.h2.api.ErrorCode
import org.h2.api.JavaObjectSerializer
import org.h2.api.TableEngine
import org.h2.command.ddl.CreateTableData
import org.h2.command.dml.SetTypes
import org.h2.constraint.Constraint
import org.h2.engine.Mode.ModeEnum
import org.h2.index.Index
import org.h2.message.DbException
import org.h2.message.Trace
import org.h2.message.TraceSystem
import org.h2.mode.DefaultNullOrdering
import org.h2.mode.PgCatalogSchema
import org.h2.mvstore.db.Store
import org.h2.result.RowFactory
import org.h2.result.SearchRow
import org.h2.schema.InformationSchema
import org.h2.schema.Schema
import org.h2.schema.SchemaObject
import org.h2.security.auth.Authenticator
import org.h2.store.DataHandler
import org.h2.store.FileLock
import org.h2.store.FileLockMethod
import org.h2.store.LobStorageInterface
import org.h2.store.fs.FileUtils
import org.h2.store.fs.encrypt.FileEncrypt
import org.h2.table.Column
import org.h2.table.Table
import org.h2.table.TableLinkConnection
import org.h2.table.TableType
import org.h2.tools.Server
import org.h2.util.JdbcUtils
import org.h2.util.NetUtils
import org.h2.util.SmallLRUCache
import org.h2.util.SourceCompiler
import org.h2.util.StringUtils
import org.h2.util.TempFileDeleter
import org.h2.util.Utils
import org.h2.value.CaseInsensitiveConcurrentMap
import org.h2.value.CompareMode
import org.h2.value.TypeInfo
import org.h2.value.ValueInteger
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.sql.SQLException
import java.util.BitSet
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * There is one database object per open database.
 *
 * The format of the meta data table is:
 *  id int, 0, objectType int, sql varchar
 *
 * @since 2004-04-15 22:49
 */
class Database(
    private var persistent: Boolean = false,
    private var databaseName: String? = null,
    private var databaseShortName: String? = null,
    private var databaseURL: String? = null,
    private var cipher: String? = null,
    private var filePasswordHash: ByteArray? = null,
) : DataHandler, CastDataProvider {
    companion object {
        private const val initialPowerOffCount = 0

        private var ASSERT: Boolean? = null

        private var META_LOCK_DEBUGGING: ThreadLocal<SessionLocal>? = null
        private var META_LOCK_DEBUGGING_DB: ThreadLocal<Database>? = null
        private var META_LOCK_DEBUGGING_STACK: ThreadLocal<Throwable>? = null
        private var EMPTY_SESSION_ARRAY = arrayOfNulls<SessionLocal>(0)

        /**
         * The default name of the system user. This name is only used as long as
         * there is no administrator user registered.
         */
        private const val SYSTEM_USER_NAME = "DBA"

        init {
            var a = false
            // Intentional side-effect
            assert(true.also { a = it })
            ASSERT = a
            if (a) {
                META_LOCK_DEBUGGING = ThreadLocal()
                META_LOCK_DEBUGGING_DB = ThreadLocal()
                META_LOCK_DEBUGGING_STACK = ThreadLocal()
            } else {
                META_LOCK_DEBUGGING = null
                META_LOCK_DEBUGGING_DB = null
                META_LOCK_DEBUGGING_STACK = null
            }
        }

        private fun isUpperSysIdentifier(upperName: String): Boolean {
            var l = upperName.length
            if (l == 0) return false

            var c = upperName[0]
            if (c < 'A' || c > 'Z') return false

            l--
            for (i in 1 until l) {
                c = upperName[i]
                if ((c < 'A' || c > 'Z') && c != '_') return false
            }

            if (l > 0) {
                c = upperName[l]
                if (c < 'A' || c > 'Z') return false
            }
            return true
        }

        /**
         * This method doesn't actually unlock the metadata table, all it does it
         * reset the debugging flags.
         *
         * @param session the session
         */
        fun unlockMetaDebug(session: SessionLocal) {
            if (ASSERT != true) return
            if (META_LOCK_DEBUGGING!!.get() == session) {
                META_LOCK_DEBUGGING!!.set(null)
                META_LOCK_DEBUGGING_DB!!.set(null)
                META_LOCK_DEBUGGING_STACK!!.set(null)
            }
        }
    }

    private var usersAndRoles: ConcurrentHashMap<String, RightOwner> = ConcurrentHashMap<String, RightOwner>()
    private var settings: ConcurrentHashMap<String, Setting> = ConcurrentHashMap<String, Setting>()
    val schemas: ConcurrentHashMap<String, Schema> = ConcurrentHashMap<String, Schema>()

    private var rights: ConcurrentHashMap<String, Right> = ConcurrentHashMap<String, Right>()
    private var comments: ConcurrentHashMap<String, Comment> = ConcurrentHashMap<String, Comment>()

    private var tableEngines: java.util.HashMap<String, TableEngine> = HashMap<String, TableEngine>()

    private var userSessions: Set<SessionLocal> = Collections.synchronizedSet(HashSet<SessionLocal>())
    private var exclusiveSession: AtomicReference<SessionLocal> = AtomicReference<SessionLocal>()
    private var objectIds: BitSet = BitSet()

    private var lobSyncObject: Any = Object()
    override fun getLobSyncObject(): Any = lobSyncObject

    private var mainSchema: Schema? = null
    private var infoSchema: Schema? = null
    private var pgCatalogSchema: Schema? = null
    private var nextSessionId = 0
    private var nextTempTableId = 0
    private var systemUser: User? = null
    private var systemSession: SessionLocal? = null
    private var lobSession: SessionLocal? = null
    private var meta: Table? = null
    private var metaIdIndex: Index? = null
    private var lock: FileLock? = null

    @Volatile
    private var starting = false
    private var traceSystem: TraceSystem? = null
    private var trace: Trace? = null
    private var fileLockMethod: FileLockMethod? = null
    private var publicRole: Role? = null
    private var modificationDataId = AtomicLong()
    private var modificationMetaId = AtomicLong()

    /**
     * Used to trigger the client side to reload some settings.
     */
    private var remoteSettingsId = AtomicLong()
    private var compareMode: CompareMode? = CompareMode.getInstance(null, 0)
    override fun getCompareMode(): CompareMode? = compareMode

    private var cluster = Constants.CLUSTERING_DISABLED
    private var readOnly = false
    private var eventListener: DatabaseEventListener? = null
    private var maxMemoryRows = SysProperties.MAX_MEMORY_ROWS
    private var lockMode = 0
    private var maxLengthInplaceLob = 0
    override fun getMaxLengthInplaceLob(): Int = maxLengthInplaceLob

    private var allowLiterals: Int = Constants.ALLOW_LITERALS_ALL

    private val powerOffCount: Int = initialPowerOffCount

    @Volatile
    private var closeDelay: Int = 0
    private val delayedCloser: DelayedDatabaseCloser? = null

    @Volatile
    private var closing: Boolean = false
    private val ignoreCase: Boolean = false
    private val deleteFilesOnDisconnect: Boolean = false
    private val optimizeReuseResults: Boolean = true
    private var cacheType: String? = null
    private val referentialIntegrity: Boolean = true
    private var mode = Mode.getRegular()
    override fun getMode(): Mode = mode

    private var defaultNullOrdering: DefaultNullOrdering = DefaultNullOrdering.LOW
    private val maxOperationMemory: Int = Constants.DEFAULT_MAX_OPERATION_MEMORY
    private val lobFileListCache: SmallLRUCache<String, Array<String>>? = null
    override fun getLobFileListCache(): SmallLRUCache<String, Array<String>>? = lobFileListCache

    private var autoServerMode: Boolean = false
    private var autoServerPort: Int = 0
    private var server: Server? = null
    private var linkConnections: HashMap<TableLinkConnection, TableLinkConnection>? = null
    private var tempFileDeleter: TempFileDeleter = TempFileDeleter.getInstance()
    override fun getTempFileDeleter(): TempFileDeleter = tempFileDeleter


    private var compactMode: Int = 0
    private var compiler: SourceCompiler? = null
    private var lobStorage: LobStorageInterface? = null
    override fun getLobStorage(): LobStorageInterface = lobStorage!!

    private var pageSize: Int = 0
    private var defaultTableType: Int = Table.TYPE_CACHED
    private var dbSettings: DbSettings? = null
    private var store: Store? = null
    private var allowBuiltinAliasOverride: Boolean = false
    private var backgroundException: AtomicReference<DbException> = AtomicReference<DbException>()
    private var javaObjectSerializer: JavaObjectSerializer? = null
    override fun getJavaObjectSerializer(): JavaObjectSerializer? = javaObjectSerializer

    private var javaObjectSerializerName: String? = null

    @Volatile
    private var javaObjectSerializerInitialized: Boolean = false

    @Volatile
    private var queryStatistics: Boolean = false
    private var queryStatisticsMaxEntries = Constants.QUERY_STATISTICS_MAX_ENTRIES
    private var queryStatisticsData: QueryStatisticsData? = null
    private var rowFactory: RowFactory = RowFactory.getRowFactory()
    private var ignoreCatalogs: Boolean = false

    private var authenticator: Authenticator? = null

    constructor(ci: ConnectionInfo,
                cipher: String?) : this(cipher = cipher,
        filePasswordHash = ci.filePasswordHash) {

        if (ASSERT!!) {
            META_LOCK_DEBUGGING!!.set(null)
            META_LOCK_DEBUGGING_DB!!.set(null)
            META_LOCK_DEBUGGING_STACK!!.set(null)
        }

        dbSettings = ci.getDbSettings()
        persistent = ci.persistent
        databaseName = ci.getName()
        databaseShortName = parseDatabaseShortName()
        maxLengthInplaceLob = Constants.DEFAULT_MAX_LENGTH_INPLACE_LOB

        autoServerMode = ci.getProperty("AUTO_SERVER", false)
        autoServerPort = ci.getProperty("AUTO_SERVER_PORT", 0)

        pageSize = ci.getProperty("PAGE_SIZE", Constants.DEFAULT_PAGE_SIZE)

        if (cipher != null && pageSize % FileEncrypt.BLOCK_SIZE != 0) {
            throw DbException.getUnsupportedException("CIPHER && PAGE_SIZE=$pageSize")
        }
        val accessModeData = StringUtils.toLowerEnglish(ci.getProperty("ACCESS_MODE_DATA", "rw")!!)
        readOnly = "r" == accessModeData

        val lockMethodName = ci.getProperty("FILE_LOCK", null)
        fileLockMethod = if (lockMethodName != null)
            FileLock.getFileLockMethod(lockMethodName)
        else if (autoServerMode)
            FileLockMethod.FILE
        else
            FileLockMethod.FS

        databaseURL = ci.url

        ci.removeProperty("DATABASE_EVENT_LISTENER", null)
            ?.let { setEventListenerClass(StringUtils.trim(s = it, leading = true, trailing = true, sp = "'")) }
        ci.removeProperty("MODE", null)
            ?.let { mode = Mode.getInstance(it) ?: throw DbException.get(ErrorCode.UNKNOWN_MODE_1, it) }
        ci.removeProperty("DEFAULT_NULL_ORDERING", null)
            ?.let {
                defaultNullOrdering = try {
                    DefaultNullOrdering.valueOf(StringUtils.toUpperEnglish(it))
                } catch (e: RuntimeException) {
                    throw DbException.getInvalidValueException("DEFAULT_NULL_ORDERING", s)
                }
            }
        ci.getProperty("JAVA_OBJECT_SERIALIZER", null)
            ?.let { javaObjectSerializerName = StringUtils.trim(s = it, leading = true, trailing = true, sp = "'") }

        allowBuiltinAliasOverride = ci.getProperty("BUILTIN_ALIAS_OVERRIDE", false)

        if (autoServerMode && !dbSettings!!.dbCloseOnExit) {
            throw DbException.getUnsupportedException("AUTO_SERVER=TRUE && DB_CLOSE_ON_EXIT=FALSE")
        }

        cacheType = StringUtils.toUpperEnglish(ci.removeProperty("CACHE_TYPE", Constants.CACHE_TYPE_DEFAULT)!!)
        ignoreCatalogs = ci.getProperty("IGNORE_CATALOGS", dbSettings!!.ignoreCatalogs)
        lockMode = ci.getProperty("LOCK_MODE", Constants.DEFAULT_LOCK_MODE)

        val traceLevelFile: Int = ci.getIntProperty(SetTypes.TRACE_LEVEL_FILE, TraceSystem.DEFAULT_TRACE_LEVEL_FILE)
        val traceLevelSystemOut: Int = ci.getIntProperty(SetTypes.TRACE_LEVEL_SYSTEM_OUT, TraceSystem.DEFAULT_TRACE_LEVEL_SYSTEM_OUT)

        traceSystem = TraceSystem((if (this.persistent) {
            if (readOnly) {
                if (traceLevelFile >= TraceSystem.DEBUG) {
                    "${Utils.getProperty("java.io.tmpdir", ".")}/h2_${System.currentTimeMillis()}${Constants.SUFFIX_TRACE_FILE}"
                } else null
            } else databaseName + Constants.SUFFIX_TRACE_FILE
        } else null)).apply {
            setLevelFile(traceLevelFile)
            setLevelSystemOut(traceLevelSystemOut)
        }
        trace = traceSystem!!.getTrace(Trace.DATABASE)
        trace!!.info("opening $databaseName, (build ${Constants.BUILD_ID})")

        try {
            if (autoServerMode
                && (readOnly!!
                        || !persistent!!
                        || fileLockMethod == FileLockMethod.NO
                        || fileLockMethod == FileLockMethod.FS)) {
                throw DbException.getUnsupportedException("AUTO_SERVER=TRUE && (readOnly || inMemory || FILE_LOCK=NO || FILE_LOCK=FS)")
            }

            if (persistent) {
                val lockFileName = databaseName + Constants.SUFFIX_LOCK_FILE
                if (readOnly) {
                    if (FileUtils.exists(lockFileName)) {
                        throw DbException.get(ErrorCode.DATABASE_ALREADY_OPEN_1, "Lock file exists: $lockFileName")
                    }
                } else if (fileLockMethod != FileLockMethod.NO && fileLockMethod != FileLockMethod.FS) {
                    lock = FileLock(traceSystem, lockFileName, Constants.LOCK_SLEEP)
                    lock!!.lock(fileLockMethod!!)
                    if (autoServerMode) startServer(lock!!.uniqueId!!)
                }
                deleteOldTempFiles()
            }

            starting = true
            store = (if (dbSettings!!.mvStore) Store(this, ci.fileEncryptionKey)
            else throw UnsupportedOperationException())
            starting = false

            systemUser = User(database = this, id = 0, userName = SYSTEM_USER_NAME, systemUser = true, admin = true)

            mainSchema = Schema(this,
                Constants.MAIN_SCHEMA_ID,
                sysIdentifier(Constants.SCHEMA_MAIN),
                systemUser,
                true)

            infoSchema = InformationSchema(this, systemUser)

            schemas[mainSchema!!.name] = mainSchema!!
            schemas[infoSchema!!.name] = infoSchema!!

            pgCatalogSchema = (if (mode.enum == ModeEnum.PostgreSQL)
                PgCatalogSchema(this, systemUser)
            else null)?.also { schemas[it.name] = it }

            publicRole = Role(database = this, id = 0, roleName = sysIdentifier(Constants.PUBLIC_ROLE_NAME), system = true)
            usersAndRoles[publicRole!!.objectName!!] = publicRole!!

            systemSession = createSession(systemUser!!)
            lobSession = createSession(systemUser!!)

            store!!.transactionStore.init(lobSession)
            val settingKeys: MutableSet<String> = dbSettings!!.settings.keys
            settingKeys.removeIf { name: String -> name.startsWith("PAGE_STORE_") }

            val data: CreateTableData = createSysTableData()
            starting = true
            meta = mainSchema!!.createTable(data)


        } catch (e: Throwable) {

        }
    }

    private fun startServer(key: String) {
        try {
            server = Server.createTcpServer(
                "-tcpPort", autoServerPort.toString(),
                "-tcpAllowOthers",
                "-tcpDaemon",
                "-key", key, databaseName)
            server!!.start()
        } catch (e: SQLException) {
            throw DbException.convert(e)
        }
        val localAddress = NetUtils.getLocalAddress()
        lock!!.setProperty("server", "$localAddress:${server!!.port}")
        lock!!.setProperty("hostName", NetUtils.getHostName(localAddress))
        lock!!.save()
    }

    private fun deleteOldTempFiles() {
        FileUtils.newDirectoryStream(FileUtils.getParent(databaseName!!))
            .filter { name -> name.endsWith(Constants.SUFFIX_TEMP_FILE) && name.startsWith(databaseName!!) }
            .forEach { name -> // can't always delete the files, they may still be open
                FileUtils.tryDelete(name)
            }
    }

    /**
     * Returns identifier in upper or lower case depending on database settings.
     *
     * @param upperName identifier in the upper case
     * @return identifier in upper or lower case
     */
    fun sysIdentifier(upperName: String): String {
        assert(Database.isUpperSysIdentifier(upperName))
        return if (dbSettings!!.databaseToLower) StringUtils.toLowerEnglish(upperName) else upperName
    }

    private fun createSession(user: User): SessionLocal = SessionLocal(this, user, ++nextSessionId)

    private fun parseDatabaseShortName(): String? {
        var n = databaseName!!
        val l = n.length
        var i = l
        loop@ while (--i >= 0) {
            when (n[i]) {
                '/', ':', '\\' -> break@loop
            }
        }
        n = if (++i == l) "UNNAMED" else n.substring(i)
        return StringUtils.truncateString(
            if (dbSettings!!.databaseToUpper) StringUtils.toUpperEnglish(n)
            else if (dbSettings!!.databaseToLower) StringUtils.toLowerEnglish(n)
            else n,
            Constants.MAX_IDENTIFIER_LENGTH)
    }

    fun setEventListenerClass(className: String?) {
        if (className.isNullOrEmpty()) {
            eventListener = null
            return
        }
        try {
            eventListener = JdbcUtils.loadUserClass<Any>(className).getDeclaredConstructor().newInstance() as DatabaseEventListener
            eventListener!!.init(cipher?.let { "$databaseURL;CIPHER=$it" } ?: databaseURL!!)
        } catch (e: Throwable) {
            throw DbException.get(
                ErrorCode.ERROR_SETTING_DATABASE_EVENT_LISTENER_2, e,
                className, e.toString())
        }
    }

    fun getAllUsersAndRoles(): Collection<RightOwner> = usersAndRoles.values
    fun getAllSchemasNoMeta(): Collection<Schema> = schemas.values
    fun getAllSettings(): Collection<Setting> = settings.values
    fun getAllRights(): ArrayList<Right> = ArrayList(rights.values)

    /**
     * Remove the object from the database.
     *
     * @param session the session
     * @param obj the object to remove
     */
    @Synchronized
    fun removeDatabaseObject(session: SessionLocal, obj: DbObject) {
        checkWritingAllowed()
        val objName: String = obj.objectName!!

        val map: MutableMap<String, DbObject> = getMap(obj.getType()).cast()

        if (SysProperties.CHECK && !map.containsKey(objName)) throw DbException.getInternalError("not found: $objName")

        val comment: Comment? = findComment(obj)
        lockMeta(session)
        if (comment != null) {
            removeDatabaseObject(session, comment)
        }
        val id: Int = obj.id
        obj.removeChildrenAndResources(session)
        map.remove(objName)
        removeMeta(session, id)
    }

    private fun getMap(type: Int): Map<String, DbObject> = when (type) {
        DbObject.USER, DbObject.ROLE -> usersAndRoles
        DbObject.SETTING -> settings
        DbObject.RIGHT -> rights
        DbObject.SCHEMA -> schemas
        DbObject.COMMENT -> comments
        else -> throw DbException.getInternalError("type=$type")
    }

    /**
     * Get the comment for the given database object if one exists, or null if
     * not.
     *
     * @param `object` the database object
     * @return the comment or null
     */
    fun findComment(dbObj: DbObject): Comment? = if (dbObj.getType() == DbObject.COMMENT) null else comments[Comment.getKey(dbObj)]

    /**
     * Lock the metadata table for updates.
     *
     * @param session the session
     * @return whether it was already locked before by this session
     */
    fun lockMeta(session: SessionLocal): Boolean {
        // this method can not be synchronized on the database object,
        // as unlocking is also synchronized on the database object -
        // so if locking starts just before unlocking, locking could
        // never be successful
        if (meta == null) return true
        if (ASSERT!!) {
            lockMetaAssertion(session)
        }
        return meta!!.lock(session, Table.EXCLUSIVE_LOCK)
    }

    private fun lockMetaAssertion(session: SessionLocal) {
        // If we are locking two different databases in the same stack, just ignore it.
        // This only happens in TestLinkedTable where we connect to another h2 DB in the
        // same process.
        if (META_LOCK_DEBUGGING_DB!!.get() == null || META_LOCK_DEBUGGING_DB!!.get() == this) return

        val prev = META_LOCK_DEBUGGING!!.get()
        if (prev == null) {
            META_LOCK_DEBUGGING!!.set(session)
            META_LOCK_DEBUGGING_DB!!.set(this)
            META_LOCK_DEBUGGING_STACK!!.set(Throwable("Last meta lock granted in this stack trace, this is debug information for following IllegalStateException"))
        } else if (prev != session) {
            META_LOCK_DEBUGGING_STACK!!.get().printStackTrace()
            throw IllegalStateException("meta currently locked by $prev, sessionid=${prev.id} and trying to be locked by different session, $session, sessionid=${session.id} on same thread")
        }
    }

    /**
     * Remove the given object from the metadata.
     *
     * @param session the session
     * @param id the id of the object to remove
     */
    fun removeMeta(session: SessionLocal, id: Int) {
        if (id <= 0 || starting) return

        val r: SearchRow = meta!!.rowFactory.createRow()
        r.setValue(0, ValueInteger[id])
        val wasLocked: Boolean = lockMeta(session)
        try {
            val cursor = metaIdIndex!!.find(session, r, r)
            if (cursor.next()) {
                val found = cursor.get()
                meta!!.removeRow(session, found)
                if (SysProperties.CHECK) {
                    checkMetaFree(session, id)
                }
            }
        } finally {
            if (!wasLocked) {
                // must not keep the lock if it was not locked
                // otherwise updating sequences may cause a deadlock
                unlockMeta(session)
            }
        }
        // release of the object id has to be postponed until the end of the transaction,
        // otherwise it might be re-used prematurely, and it would make
        // rollback impossible or lead to MVMaps name collision,
        // so until then ids are accumulated within session
        session.scheduleDatabaseObjectIdForRelease(id)
    }

    private fun checkMetaFree(session: SessionLocal, id: Int) {
        val r: SearchRow = meta!!.rowFactory.createRow()
        r.setValue(0, ValueInteger[id])
        val cursor = metaIdIndex!!.find(session, r, r)
        if (cursor.next()) throw DbException.getInternalError()
    }

    /**
     * Unlock the metadata table.
     *
     * @param session the session
     */
    fun unlockMeta(session: SessionLocal) {
        if (meta == null) return
        Database.unlockMetaDebug(session)
        meta!!.unlock(session)
        session.unlock(meta)
    }

    /**
     * Get the trace object for the given module id.
     *
     * @param moduleId the module id
     * @return the trace object
     */
    fun getTrace(moduleId: Int): Trace = traceSystem!!.getTrace(moduleId)

    private fun createSysTableData(): CreateTableData = CreateTableData().also { data ->
        val cols = data.columns

        cols.add(Column(name = "ID", type = TypeInfo.TYPE_INTEGER).apply { nullable = false })
        cols.add(Column(name = "HEAD", type = TypeInfo.TYPE_INTEGER))
        cols.add(Column(name = "TYPE", type = TypeInfo.TYPE_INTEGER))
        cols.add(Column(name = "SQL", type = TypeInfo.TYPE_VARCHAR))

        data.tableName = "SYS"
        data.id = 0
        data.temporary = false
        data.persistData = persistent
        data.persistIndexes = persistent
        data.isHidden = true
        data.session = systemSession
    }

    /**
     * Create a new hash map. Depending on the configuration, the key is
     * case-sensitive or case-insensitive.
     *
     * @param <V> the value type
     * @return the hash map
    </V> */
    fun <V> newConcurrentStringMap(): ConcurrentHashMap<String, V> = if (dbSettings!!.caseInsensitiveIdentifiers)
        CaseInsensitiveConcurrentMap<V>()
    else
        ConcurrentHashMap()

    /**
     * Remove an object from the system table.
     *
     * @param session the session
     * @param obj the object to be removed
     */
    open fun removeSchemaObject(session: SessionLocal,
                                obj: SchemaObject) {
        val type: Int = obj.getType()
        if (type == DbObject.TABLE_OR_VIEW) {
            val table = obj as Table
            if (table.temporary && !table.isGlobalTemporary()) {
                session.removeLocalTempTable(table)
                return
            }
        } else if (type == DbObject.INDEX) {
            val index = obj as Index
            val table = index.table
            if (table.isTemporary && !table.isGlobalTemporary) {
                session.removeLocalTempTableIndex(index)
                return
            }
        } else if (type == DbObject.CONSTRAINT) {
            val constraint = obj as Constraint
            if (constraint.constraintType != Constraint.Type.DOMAIN) {
                val table = constraint.table
                if (table.isTemporary && !table.isGlobalTemporary) {
                    session.removeLocalTempTableConstraint(constraint)
                    return
                }
            }
        }
        checkWritingAllowed()
        lockMeta(session)
        synchronized(this) {
            val comment = findComment(obj)
            comment?.let { removeDatabaseObject(session, it) }
            obj.schema.remove(obj)
            val id: Int = obj.id
            if (!starting) {
                val t: Table = getDependentTable(obj, null)
                if (t != null) {
                    obj.schema.add(obj)
                    throw DbException.get(ErrorCode.CANNOT_DROP_2, obj.getTraceSQL(), t.getTraceSQL())
                }
                obj.removeChildrenAndResources(session)
            }
            removeMeta(session, id)
        }
    }

    /**
     * Get the first table that depends on this object.
     *
     * @param obj the object to find
     * @param except the table to exclude (or null)
     * @return the first dependent table, or null
     */
    fun getDependentTable(obj: SchemaObject, except: Table): Table? {
        when (obj.getType()) {
            DbObject.COMMENT, DbObject.CONSTRAINT, DbObject.INDEX, DbObject.RIGHT, DbObject.TRIGGER, DbObject.USER -> return null
            else -> {}
        }
        val set = java.util.HashSet<DbObject>()
        for (schema in schemas.values) {
            for (t in schema.getAllTablesAndViews(null)) {
                if (except === t || TableType.VIEW == t.getTableType()) {
                    continue
                }
                set.clear()
                t.addDependencies(set)
                if (set.contains(obj)) {
                    return t
                }
            }
        }
        return null
    }

}