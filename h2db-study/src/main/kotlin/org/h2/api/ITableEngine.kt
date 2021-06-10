package org.h2.api

import org.h2.command.ddl.CreateTableData
import org.h2.table.Table


/**
 * A class that implements this interface can create custom table
 * implementations.
 *
 * @author Sergi Vladykin
 */
interface ITableEngine {
    /**
     * Create new table.
     *
     * @param data the data to construct the table
     * @return the created table
     */
    fun createTable(data: CreateTableData?): Table?
}