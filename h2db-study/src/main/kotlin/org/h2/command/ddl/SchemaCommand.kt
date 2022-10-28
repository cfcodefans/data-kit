package org.h2.command.ddl

import org.h2.engine.SessionLocal
import org.h2.schema.Schema

/**
 * This class represents a non-transaction statement that involves a schema.
 */
abstract class SchemaCommand(session: SessionLocal,
                             open val schema: Schema) : DefineCommand(session) {

}