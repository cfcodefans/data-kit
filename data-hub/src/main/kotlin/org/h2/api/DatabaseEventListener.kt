package org.h2.api

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
}