package org.h2.command.ddl

import org.h2.engine.SessionLocal
import org.h2.schema.Schema
import org.h2.table.Column

/**
 * The data required to create a table.
 */
open class CreateTableData(
    /**
     * The schema.
     */
    var schema: Schema? = null,
    /**
     * The table name.
     */
    var tableName: String? = null,
    /**
     * The object id.
     */
    var id: Int = 0,
    /**
     * The column list.
     */
    var columns: ArrayList<Column> = ArrayList<Column>(),
    /**
     * Whether this is a temporary table.
     */
    var temporary: Boolean = false,
    /**
     * Whether the table is global temporary.
     */
    var globalTemporary: Boolean = false,
    /**
     * Whether the indexes should be persisted.
     */
    var persistIndexes: Boolean = false,
    /**
     * Whether the data should be persisted.
     */
    var persistData: Boolean = false,
    /**
     * The session.
     */
    var session: SessionLocal? = null,
    /**
     * The table engine to use for creating the table.
     */
    var tableEngine: String? = null,
    /**
     * The table engine params to use for creating the table.
     */
    var tableEngineParams: ArrayList<String>? = null,
    /**
     * The table is hidden.
     */
    var isHidden: Boolean = false) {

}