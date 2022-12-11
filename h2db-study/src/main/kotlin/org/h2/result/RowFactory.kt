package org.h2.result

import org.h2.engine.CastDataProvider
import org.h2.mvstore.db.RowDataType
import org.h2.store.DataHandler
import org.h2.table.IndexColumn
import org.h2.value.CompareMode
import org.h2.value.TypeInfo
import org.h2.value.Typed
import org.h2.value.Value

/**
 * Creates rows.
 */
abstract class RowFactory {
    companion object {
        /**
         * Default implementation of row factory.
         */
        class DefaultRowFactory(
            override val dataType: RowDataType = RowDataType(null, CompareMode.getInstance(null, 0), null, null, null, 0, true),
            override val columnCount: Int = 0,
            override val indexes: IntArray? = null,
            override var columnTypes: Array<TypeInfo>? = null) : RowFactory() {

            val map: IntArray? = indexes?.mapIndexed { _, i -> i + 1 }?.toIntArray()

            override fun createRow(): SearchRow = when {
                indexes == null -> DefaultRow(columnCount)
                indexes.size == 1 -> SimpleRowValue(columnCount, indexes[0])
                else -> Sparse(columnCount, indexes.size, map)
            }

            override fun createRow(data: Array<Value?>, memory: Int): Row = DefaultRow(data, memory)

            override fun getStoreKeys(): Boolean = dataType.isStoreKeys
        }

        val EFFECTIVE: RowFactory = DefaultRowFactory()

        fun getRowFactory(): RowFactory = RowFactory.EFFECTIVE

        /**
         * Create a new row factory.
         *
         * @param provider the cast provider
         * @param compareMode the compare mode
         * @param handler the data handler
         * @param sortTypes the sort types
         * @param indexes the list of indexed columns
         * @param columnTypes the list of column data type information
         * @param columnCount the number of columns
         * @param storeKeys whether row keys are stored
         * @return the (possibly new) row factory
         */
        fun createRowFactory(provider: CastDataProvider?,
                             compareMode: CompareMode?,
                             handler: DataHandler?,
                             sortTypes: IntArray?,
                             indexes: IntArray?,
                             columnTypes: Array<TypeInfo>?,
                             columnCount: Int,
                             storeKeys: Boolean): RowFactory {

            val rowDataType = RowDataType(provider, compareMode, handler, sortTypes, indexes, columnCount, storeKeys)
            val rowFactory: RowFactory = DefaultRowFactory(rowDataType, columnCount, indexes, columnTypes)
            rowDataType.rowFactory = rowFactory
            return rowFactory
        }

    }

    /**
     * Create new row.
     *
     * @return the created row
     */
    abstract fun createRow(): SearchRow

    abstract val dataType: RowDataType?

    abstract val indexes: IntArray?

    abstract val columnTypes: Array<TypeInfo>?

    abstract val columnCount: Int

    abstract fun getStoreKeys(): Boolean

    /**
     * Create a new row.
     *
     * @param data the values
     * @param memory the estimated memory usage in bytes
     * @return the created row
     */
    abstract fun createRow(data: Array<Value?>, memory: Int): Row

    /**
     * Create a new row factory.
     *
     * @param provider the cast provider
     * @param compareMode the compare mode
     * @param handler the data handler
     * @param columns the list of columns
     * @param indexColumns the list of index columns
     * @param storeKeys whether row keys are stored
     * @return the (possibly new) row factory
     */
    open fun createRowFactory(provider: CastDataProvider?,
                              compareMode: CompareMode?,
                              handler: DataHandler?,
                              columns: Array<Typed?>?,
                              indexColumns: Array<IndexColumn>?,
                              storeKeys: Boolean): RowFactory? = this
}