package org.h2.api

import java.sql.SQLException
import java.util.*

/**
 * A class that implements this interface can get notified about exceptions
 * and other events. A database event listener can be registered when
 * connecting to a database. Example database URL:
 * jdbc:h2:./test;DATABASE_EVENT_LISTENER='com.acme.DbListener'
 */
interface DatabaseEventListener : EventListener {
    companion object {
        /**
         * This state is used when scanning the database file.
         */
        const val STATE_SCAN_FILE: Int = 0
        /**
         * This state is used when re-creating an index.
         */
        const val STATE_CREATE_INDEX: Int = 1
        /**
         * This state is used when re-applying the transaction log or rolling back
         * uncommitted transactions.
         */
        const val STATE_RECOVER: Int = 2
        /**
         * This state is used during the BACKUP command.
         */
        const val STATE_BACKUP_FILE: Int = 3
        /**
         * This state is used after re-connecting to a database (if auto-reconnect
         * is enabled).
         */
        const val STATE_RECONNECTED: Int = 4
        /**
         * This state is used when a query starts.
         */
        const val STATE_STATEMENT_START: Int = 5
        /**
         * This state is used when a query ends.
         */
        const val STATE_STATEMENT_END: Int = 6
        /**
         * This state is used for periodic notification during long-running queries.
         */
        const val STATE_STATEMENT_PROGRESS: Int = 7
    }

    /**
     * This method is called just after creating the object.
     * This is done when opening the database if the listener is specified
     * in the database URL, but may be later if the listener is set at
     * runtime with the SET SQL statement.
     * @param url - the database URL
     */
    fun init(url: String): Unit

    /**
     * This method is called after the database has been opened. It is save
     * to connect to the database and execute statements at this point.
     */
    fun opened(): Unit

    /**
     * The method is called if an exception occurred.
     * @param e the exception
     * @param sql the SQL statement
     */
    fun exceptionThrown(e: SQLException, sql: String): Unit

    /**
     * This method is called for long running events, such as recovering,
     * scanning a file or building an index.
     * <p/>
     * More state might be added in future versions, therefore implementations
     * should silently ignore states that they don't understand.
     * </p>
     * @param state the state
     * @param name the object name
     * @param x the current position
     * @param max the highest possible value (might be 0)
     */
    fun setProgress(state: Int, name: String, x: Int, max: Int): Unit

    /**
     * This method is called before the database is closed normally. It is save
     * to connect to the database and execute statements at this point, however
     * the connection must be closed before the method returns.
     */
    fun closingDatabase(): Unit
}