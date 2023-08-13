package org.h2.index

import org.h2.engine.SessionLocal
import org.h2.result.SearchRow

/**
 * A spatial index. Spatial indexes are used to speed up searching
 * spatial/geometric data.
 */
interface SpatialIndex {
    /**
     * Find a row or a list of rows and create a cursor to iterate over the
     * result.
     *
     * @param session the session
     * @param first the lower bound
     * @param last the upper bound
     * @param intersection the geometry which values should intersect with, or
     * null for anything
     * @return the cursor to iterate over the results
     */
    fun findByGeometry(session: SessionLocal, first: SearchRow?, last: SearchRow?, intersection: SearchRow?): Cursor?
}
