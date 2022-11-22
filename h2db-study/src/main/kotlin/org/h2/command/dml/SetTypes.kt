package org.h2.command.dml


/**
 * The list of setting for a SET statement.
 */
object SetTypes {
    /**
     * The type of a SET IGNORECASE statement.
     */
    const val IGNORECASE = 0

    /**
     * The type of a SET MAX_LOG_SIZE statement.
     */
    const val MAX_LOG_SIZE = IGNORECASE + 1

    /**
     * The type of a SET MODE statement.
     */
    const val MODE = MAX_LOG_SIZE + 1

    /**
     * The type of a SET READONLY statement.
     */
    const val READONLY = MODE + 1

    /**
     * The type of a SET LOCK_TIMEOUT statement.
     */
    const val LOCK_TIMEOUT = READONLY + 1

    /**
     * The type of a SET DEFAULT_LOCK_TIMEOUT statement.
     */
    const val DEFAULT_LOCK_TIMEOUT = LOCK_TIMEOUT + 1

    /**
     * The type of a SET DEFAULT_TABLE_TYPE statement.
     */
    const val DEFAULT_TABLE_TYPE = DEFAULT_LOCK_TIMEOUT + 1

    /**
     * The type of a SET CACHE_SIZE statement.
     */
    const val CACHE_SIZE = DEFAULT_TABLE_TYPE + 1

    /**
     * The type of a SET TRACE_LEVEL_SYSTEM_OUT statement.
     */
    const val TRACE_LEVEL_SYSTEM_OUT = CACHE_SIZE + 1

    /**
     * The type of a SET TRACE_LEVEL_FILE statement.
     */
    const val TRACE_LEVEL_FILE = TRACE_LEVEL_SYSTEM_OUT + 1

    /**
     * The type of a SET TRACE_MAX_FILE_SIZE statement.
     */
    const val TRACE_MAX_FILE_SIZE = TRACE_LEVEL_FILE + 1

    /**
     * The type of a SET COLLATION  statement.
     */
    const val COLLATION = TRACE_MAX_FILE_SIZE + 1

    /**
     * The type of a SET CLUSTER statement.
     */
    const val CLUSTER = COLLATION + 1

    /**
     * The type of a SET WRITE_DELAY statement.
     */
    const val WRITE_DELAY = CLUSTER + 1

    /**
     * The type of a SET DATABASE_EVENT_LISTENER statement.
     */
    const val DATABASE_EVENT_LISTENER = WRITE_DELAY + 1

    /**
     * The type of a SET MAX_MEMORY_ROWS statement.
     */
    const val MAX_MEMORY_ROWS = DATABASE_EVENT_LISTENER + 1

    /**
     * The type of a SET LOCK_MODE statement.
     */
    const val LOCK_MODE = MAX_MEMORY_ROWS + 1

    /**
     * The type of a SET DB_CLOSE_DELAY statement.
     */
    const val DB_CLOSE_DELAY = LOCK_MODE + 1

    /**
     * The type of a SET THROTTLE statement.
     */
    const val THROTTLE = DB_CLOSE_DELAY + 1

    /**
     * The type of a SET MAX_MEMORY_UNDO statement.
     */
    const val MAX_MEMORY_UNDO = THROTTLE + 1

    /**
     * The type of a SET MAX_LENGTH_INPLACE_LOB statement.
     */
    const val MAX_LENGTH_INPLACE_LOB = MAX_MEMORY_UNDO + 1

    /**
     * The type of a SET ALLOW_LITERALS statement.
     */
    const val ALLOW_LITERALS = MAX_LENGTH_INPLACE_LOB + 1

    /**
     * The type of a SET SCHEMA statement.
     */
    const val SCHEMA = ALLOW_LITERALS + 1

    /**
     * The type of a SET OPTIMIZE_REUSE_RESULTS statement.
     */
    const val OPTIMIZE_REUSE_RESULTS = SCHEMA + 1

    /**
     * The type of a SET SCHEMA_SEARCH_PATH statement.
     */
    const val SCHEMA_SEARCH_PATH = OPTIMIZE_REUSE_RESULTS + 1

    /**
     * The type of a SET REFERENTIAL_INTEGRITY statement.
     */
    const val REFERENTIAL_INTEGRITY = SCHEMA_SEARCH_PATH + 1

    /**
     * The type of a SET MAX_OPERATION_MEMORY statement.
     */
    const val MAX_OPERATION_MEMORY = REFERENTIAL_INTEGRITY + 1

    /**
     * The type of a SET EXCLUSIVE statement.
     */
    const val EXCLUSIVE = MAX_OPERATION_MEMORY + 1

    /**
     * The type of a SET CREATE_BUILD statement.
     */
    const val CREATE_BUILD = EXCLUSIVE + 1

    /**
     * The type of a SET \@VARIABLE statement.
     */
    const val VARIABLE = CREATE_BUILD + 1

    /**
     * The type of a SET QUERY_TIMEOUT statement.
     */
    const val QUERY_TIMEOUT = VARIABLE + 1

    /**
     * The type of a SET REDO_LOG_BINARY statement.
     */
    const val REDO_LOG_BINARY = QUERY_TIMEOUT + 1

    /**
     * The type of a SET JAVA_OBJECT_SERIALIZER statement.
     */
    const val JAVA_OBJECT_SERIALIZER = REDO_LOG_BINARY + 1

    /**
     * The type of a SET RETENTION_TIME statement.
     */
    const val RETENTION_TIME = JAVA_OBJECT_SERIALIZER + 1

    /**
     * The type of a SET QUERY_STATISTICS statement.
     */
    const val QUERY_STATISTICS = RETENTION_TIME + 1

    /**
     * The type of a SET QUERY_STATISTICS_MAX_ENTRIES statement.
     */
    const val QUERY_STATISTICS_MAX_ENTRIES = QUERY_STATISTICS + 1

    /**
     * The type of SET LAZY_QUERY_EXECUTION statement.
     */
    const val LAZY_QUERY_EXECUTION = QUERY_STATISTICS_MAX_ENTRIES + 1

    /**
     * The type of SET BUILTIN_ALIAS_OVERRIDE statement.
     */
    const val BUILTIN_ALIAS_OVERRIDE = LAZY_QUERY_EXECUTION + 1

    /**
     * The type of a SET AUTHENTICATOR statement.
     */
    const val AUTHENTICATOR = BUILTIN_ALIAS_OVERRIDE + 1

    /**
     * The type of a SET IGNORE_CATALOGS statement.
     */
    const val IGNORE_CATALOGS = AUTHENTICATOR + 1

    /**
     * The type of a SET CATALOG statement.
     */
    const val CATALOG = IGNORE_CATALOGS + 1

    /**
     * The type of a SET NON_KEYWORDS statement.
     */
    const val NON_KEYWORDS = CATALOG + 1

    /**
     * The type of a SET TIME ZONE statement.
     */
    const val TIME_ZONE = NON_KEYWORDS + 1

    /**
     * The type of a SET VARIABLE_BINARY statement.
     */
    const val VARIABLE_BINARY = TIME_ZONE + 1

    /**
     * The type of a SET DEFAULT_NULL_ORDERING statement.
     */
    const val DEFAULT_NULL_ORDERING = VARIABLE_BINARY + 1

    /**
     * The type of a SET TRUNCATE_LARGE_LENGTH statement.
     */
    const val TRUNCATE_LARGE_LENGTH = DEFAULT_NULL_ORDERING + 1

    private const val COUNT = TRUNCATE_LARGE_LENGTH + 1

    val TYPES: ArrayList<String> = arrayListOf(
        "IGNORECASE",
        "MAX_LOG_SIZE",
        "MODE",
        "READONLY",
        "LOCK_TIMEOUT",
        "DEFAULT_LOCK_TIMEOUT",
        "DEFAULT_TABLE_TYPE",
        "CACHE_SIZE",
        "TRACE_LEVEL_SYSTEM_OUT",
        "TRACE_LEVEL_FILE",
        "TRACE_MAX_FILE_SIZE",
        "COLLATION",
        "CLUSTER",
        "WRITE_DELAY",
        "DATABASE_EVENT_LISTENER",
        "MAX_MEMORY_ROWS",
        "LOCK_MODE",
        "DB_CLOSE_DELAY",
        "THROTTLE",
        "MAX_MEMORY_UNDO",
        "MAX_LENGTH_INPLACE_LOB",
        "ALLOW_LITERALS",
        "SCHEMA",
        "OPTIMIZE_REUSE_RESULTS",
        "SCHEMA_SEARCH_PATH",
        "REFERENTIAL_INTEGRITY",
        "MAX_OPERATION_MEMORY",
        "EXCLUSIVE",
        "CREATE_BUILD",
        "@",
        "QUERY_TIMEOUT",
        "REDO_LOG_BINARY",
        "JAVA_OBJECT_SERIALIZER",
        "RETENTION_TIME",
        "QUERY_STATISTICS",
        "QUERY_STATISTICS_MAX_ENTRIES",
        "LAZY_QUERY_EXECUTION",
        "BUILTIN_ALIAS_OVERRIDE",
        "AUTHENTICATOR",
        "IGNORE_CATALOGS",
        "CATALOG",
        "NON_KEYWORDS",
        "TIME ZONE",
        "VARIABLE_BINARY",
        "DEFAULT_NULL_ORDERING",
        "TRUNCATE_LARGE_LENGTH")

    init {
        assert(TYPES.size == COUNT)
    }

    /**
     * Get the set type number.
     *
     * @param name the set type name
     * @return the number
     */
    fun getType(name: String): Int = TYPES.indexOf(name)

    /**
     * Get the set type name.
     *
     * @param type the type number
     * @return the name
     */
    fun getTypeName(type: Int): String = TYPES[type]
}