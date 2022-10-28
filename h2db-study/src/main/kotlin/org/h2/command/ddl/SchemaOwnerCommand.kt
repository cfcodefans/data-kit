package org.h2.command.ddl

import org.h2.engine.SessionLocal
import org.h2.schema.Schema

/**
 * This class represents a non-transaction statement that involves a schema and
 * requires schema owner rights.
 */
abstract class SchemaOwnerCommand(session: SessionLocal,
                                  schema: Schema) : SchemaCommand(session, schema) {

    override fun update(): Long {
        session.user.checkSchemaOwner(schema)
        return update(schema)
    }

    abstract fun update(schema: Schema?): Long
}