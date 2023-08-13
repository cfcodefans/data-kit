package org.h2.util

import kotlin.IntArray
import kotlin.math.max

open class IntArray(private var data: IntArray?) {
    private var size: Int = data?.size ?: 0
    private var hash: Int = 0

    constructor(capacity: Int) : this(data = IntArray(capacity))

    constructor() : this(capacity = 10)

    override fun hashCode(): Int {
        if (hash != 0) return hash
        var h: Int = size + 1
        for (i in 0 until size) {
            h = h * 31 + data!![i]
        }
        hash = h
        return h
    }

    /**
     * Get the size of the list.
     *
     * @return the size
     */
    fun size(): Int = size

    /**
     * Append a value.
     *
     * @param value the value to append
     */
    fun add(value: Int): org.h2.util.IntArray = apply {
        if (size >= data!!.size) {
            ensureCapacity(size + size)
        }
        data!![size++] = value
    }

    /**
     * Ensure the underlying array is large enough for the given number of
     * entries.
     *
     * @param minCapacity the minimum capacity
     */
    fun ensureCapacity(minCapacity: Int) {
        var minCapacity = minCapacity
        minCapacity = max(4.0, minCapacity.toDouble()).toInt()
        if (minCapacity >= data!!.size) {
            data = data!!.copyOf(minCapacity)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is org.h2.util.IntArray) return false
        if (hashCode() != other.hashCode() || size != other.size) return false
        for (i in 0 until size) {
            if (data!![i] != other.data!![i]) return false
        }
        return true
    }

    /**
     * Convert this list to an array. The target array must be big enough.
     *
     * @param array the target array
     */
    fun toArray(array: IntArray) {
        System.arraycopy(data, 0, array, 0, size)
    }

    override fun toString(): String {
        return (this.data ?: IntArray(0))
            .joinToString(separator = ",",
                    prefix = "{",
                    postfix = "}")
    }

    /**
     * Remove a number of elements.
     *
     * @param fromIndex the index of the first item to remove
     * @param toIndex upper bound (exclusive)
     */
    fun removeRange(fromIndex: Int, toIndex: Int) {
        if (fromIndex > toIndex || toIndex > size) {
            throw ArrayIndexOutOfBoundsException("from=$fromIndex to=$toIndex size=$size")
        }
        System.arraycopy(data, toIndex, data, fromIndex, size - toIndex)
        size -= toIndex - fromIndex
    }


    /**
     * Get the value at the given index.
     *
     * @param index the index
     * @return the value
     */
    operator fun get(index: Int): Int {
        if (index >= size) {
            throw ArrayIndexOutOfBoundsException("i=$index size=$size")
        }
        return data!![index]
    }
}