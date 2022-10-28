package org.h2.command.ddl

import org.h2.command.Prepared
import org.h2.engine.SessionLocal
import org.h2.result.ResultInterface

/**
 * This class represents a non-transaction statement, for example a CREATE or
 * DROP.
 */
abstract class DefineCommand(
    /**
     * Create a new command for the given session.
     * @param session the session
     */
    session: SessionLocal) : Prepared(session) {
    /**
     * The transactional behavior. The default is disabled, meaning the command
     * commits an open transaction.
     */
    open var transactional = false

    override fun isReadOnly(): Boolean = false

    override fun queryMeta(): ResultInterface? = null

}