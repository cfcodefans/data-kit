package org.h2.value

/**
 * A versioned value (possibly null).
 * It contains current value and latest committed value if current one is uncommitted.
 * Also for uncommitted values it contains operationId - a combination of
 * transactionId and logId.
 */
open class VersionedValue<T> {

    protected constructor()

    open fun isCommitted(): Boolean = true
    open fun getOperationId(): Long = 0L
    open fun getCurrentValue(): T = this as T
    open fun getCommittedValue(): T = this as T
}