package org.h2.util

/**
 * The stack of byte values. This class is not synchronized and should not be
 * used by multiple threads concurrently.
 */
class ByteStack {
    companion object {
        private const val MAX_ARRAY_SIZE = Int.MAX_VALUE - 8
    }

    private var size = 0

    private var array: ByteArray = Utils.EMPTY_BYTES

    private fun grow(length: Int) = apply {
        var len = if (length == 0) 0x10
        else if (length >= MAX_ARRAY_SIZE) throw OutOfMemoryError()
        else (length shl 1).let { if (it < 0) MAX_ARRAY_SIZE else it }

        array = array.copyOf(length)
    }

    /**
     * Returns the number of items in this stack.
     *
     * @return the number of items in this stack
     */
    fun size(): Int = size

    /**
     * Returns `true` if this stack is empty.
     *
     * @return `true` if this stack is empty
     */
    fun isEmpty(): Boolean = size == 0

    /**
     * Looks at the item at the top of this stack without removing it.
     *
     * @param defaultValue value to return if stack is empty
     * @return the item at the top of this stack, or default value
     */
    fun peek(defaultValue: Int): Int {
        val index = size - 1
        return if (index < 0) defaultValue else array[index].toInt()
    }

    /**
     * Removes the item at the top of this stack and returns that item.
     *
     * @param defaultValue value to return if stack is empty
     * @return the item at the top of this stack, or default value
     */
    fun poll(defaultValue: Int): Int {
        val index = size - 1
        if (index < 0) {
            return defaultValue
        }
        size = index
        return array[index].toInt()
    }

    /**
     * Removes the item at the top of this stack and returns that item.
     *
     * @return the item at the top of this stack
     * @throws NoSuchElementException if stack is empty
     */
    fun pop(): Byte {
        val index = size - 1
        if (index < 0) {
            throw NoSuchElementException()
        }
        size = index
        return array[index]
    }

    /**
     * Pushes an item onto the top of this stack.
     *
     * @param item the item to push
     */
    fun push(item: Byte) = apply {
        val index = size
        val oldLength = array.size
        if (index >= oldLength) {
            grow(oldLength)
        }
        array[index] = item
        size = index + 1
    }
}