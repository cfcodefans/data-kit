package org.h2.api

import java.sql.Connection
import java.sql.SQLException

/**
 * A user_defined aggregate function needs to implement this interface.
 * THe class must be public and must have a public non-argument constructor.
 * <p>
 * Please note this interface only has limited support for data types.
 * If you need data types that don't have a corresponding SQL type
 * (for example GEOMETRY), then use the {link Aggregate) interface.
 * </p>
 */
interface AggregateFunction {

    /**
     * This method is called when the aggregate function is used.
     * A new ojbect is created for each invocation.
     * @param conn a connection to the database
     */
    @Throws(SQLException::class)
    fun init(conn: Connection): Unit

    /**
     * This method must return the SQL type of the method, given the SQL type of
     * the input data. The method should check here if the number of parameters
     * passed is correct, and if not it should throw an exception.
     * @param inputTypes the SQL type of the parameters, {@link java.sql.Types}
     * @return the SQL type of the result
     */
    @Throws(SQLException::class)
    fun getType(inputTypes: IntArray): Int

    /**
     * This method is called once for each row.
     * If the aggregate function is called with multiple parameters,
     * those are passed as array.
     * @param value the value(s) for this row
     */
    @Throws(SQLException::class)
    fun add(value: Any): Unit

    /**
     * This method returns the computed aggregate value. This method must
     * preserve previously added values and must be able to reevaluate result if
     * more values were added since its previous invocation.
     * @return the aggregate value
     */
    @Throws(SQLException::class)
    fun getResult(): Any
}