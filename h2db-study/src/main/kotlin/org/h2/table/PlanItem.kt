package org.h2.table

import org.h2.index.Index


/**
 * The plan item describes the index to be used, and the estimated cost when
 * using it.
 */
open class PlanItem(
    /**
     * The cost.
     */
    var cost: Double = 0.0,
    var masks: IntArray? = null,
    var index: Index? = null,
    var joinPlan: PlanItem? = null,
    var nestedJoinPlan: PlanItem? = null) {
}