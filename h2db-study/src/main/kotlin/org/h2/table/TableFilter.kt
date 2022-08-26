package org.h2.table

import org.h2.command.query.Select
import org.h2.engine.Right
import org.h2.engine.SessionLocal
import org.h2.index.Index

/**
 * A visitor for table filters.
 * This method is called for each nested or joined table filter.
 * @param f the filter
 */
typealias TableFilterVisitor = (TableFilter) -> Unit

/**
 * A table filter represents a table that is used in a query. There is one such
 * object whenever a table (or view) is used in a query. For example the
 * following query has 2 table filters: SELECT * FROM TEST T1, TEST T2.
 *
 *     * Create a new table filter object.
 *
 * @param session the session
 * @param table the table from where to read data
 * @param alias the alias name
 * @param rightsChecked true if rights are already checked
 * @param select the select statement
 * @param orderInFrom original order number (index) of this table filter in
 * @param indexHints the index hints to be used by the query planner
 */
open class TableFilter(private var sesson: SessionLocal,
                       private val table: Table,
                       private var alias: String,
                       rightsChecked: Boolean,
                       override val select: Select?,
                       private val orderInFrom: Int = 0,
                       val indexHints: IndexHints?) : ColumnResolver {

    private val hashCode: Int = sesson.nextObjectId()

    init {
        if (rightsChecked.not()) sesson.user.checkTableRight(table, Right.SELECT)
    }

    /**
     * Whether this is a direct or indirect (nested) outer join
     */
    protected var joinOuterIndirect = false

    private var index: Index? = null

    var masks: IntArray? = null
    private var scanCount: Int = 0
    var evaluatable: Boolean = false

    /**
     * Indicates that this filter is used in the plan.
     */
    var used: Boolean = false

    companion object {
        private const val BEFORE_FIRST = 0
        private const val FOUND = 1
        private const val AFTER_LAST = 2
        private const val NULL_ROW = 3

        /**
         * A visitor that sets joinOuterIndirect to true.
         */
        private val JOI_VISITOR: TableFilterVisitor = { f: TableFilter -> f.joinOuterIndirect = true }
    }
}