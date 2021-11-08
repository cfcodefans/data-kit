package org.h2.api

import java.sql.Connection
import java.sql.SQLException


/**
 * A class that implements this interface can be used as a trigger.
 */
interface ITrigger {
    /**
     * This method is called by the database engine once when initializing the
     * trigger. It is called when the trigger is created, as well as when the
     * database is opened. The type of operation is a bit field with the
     * appropriate flags set. As an example, if the trigger is of type INSERT
     * and UPDATE, then the parameter type is set to (INSERT | UPDATE).
     *
     * @param conn a connection to the database (a system connection)
     * @param schemaName the name of the schema
     * @param triggerName the name of the trigger used in the CREATE TRIGGER
     * statement
     * @param tableName the name of the table
     * @param before whether the fire method is called before or after the
     * operation is performed
     * @param type the operation type: INSERT, UPDATE, DELETE, SELECT, or a
     * combination (this parameter is a bit field)
     * @throws SQLException on SQL exception
     */
    @Throws(SQLException::class)
    fun init(conn: Connection?, schemaName: String?, triggerName: String?,
             tableName: String?, before: Boolean, type: Int) {
        // Does nothing by default
    }

    /**
     * This method is called for each triggered action. The method is called
     * immediately when the operation occurred (before it is committed). A
     * transaction rollback will also rollback the operations that were done
     * within the trigger, if the operations occurred within the same database.
     * If the trigger changes state outside the database, a rollback trigger
     * should be used.
     *
     *
     * The row arrays contain all columns of the table, in the same order
     * as defined in the table.
     *
     *
     *
     * The trigger itself may change the data in the newRow array.
     *
     *
     * @param conn a connection to the database
     * @param oldRow the old row, or null if no old row is available (for
     * INSERT)
     * @param newRow the new row, or null if no new row is available (for
     * DELETE)
     * @throws SQLException if the operation must be undone
     */
    @Throws(SQLException::class)
    fun fire(conn: Connection?, oldRow: Array<Any?>?, newRow: Array<Any?>?)

    /**
     * This method is called when the database is closed.
     * If the method throws an exception, it will be logged, but
     * closing the database will continue.
     *
     * @throws SQLException on SQL exception
     */
    @Throws(SQLException::class)
    fun close() {
        // Does nothing by default
    }

    /**
     * This method is called when the trigger is dropped.
     *
     * @throws SQLException on SQL exception
     */
    @Throws(SQLException::class)
    fun remove() {
        // Does nothing by default
    }

    companion object {
        /**
         * The trigger is called for INSERT statements.
         */
        const val INSERT = 1

        /**
         * The trigger is called for UPDATE statements.
         */
        const val UPDATE = 2

        /**
         * The trigger is called for DELETE statements.
         */
        const val DELETE = 4

        /**
         * The trigger is called for SELECT statements.
         */
        const val SELECT = 8
    }
}