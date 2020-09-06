package org.h2.value

/**
 * A versioned value (possibly null).
 * It contains current value and latest committed value if current one is uncommitted.
 * Also for uncommitted values it contains operationId - a combination of
 * transactionId and logId
 */
open class VersionedValue {
    /**
     * Used when we don't care about a VersionedValue instance.
     */
    companion object {
        val DUMMY: VersionedValue = VersionedValue()
    }

    protected constructor()

    fun isCommitted(): Boolean = true
    fun getOperationId(): Long = 0L
    fun getCurrentValue(): Any = this
    fun getCommittedValue(): Any = this
}