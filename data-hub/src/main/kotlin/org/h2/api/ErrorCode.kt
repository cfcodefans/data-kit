package org.h2.api

/**
 * This class defines the error codes used for SQL exceptions.
 * Error messages are formatted as follows:
 * <pre>
 *     { error message (possibly translated; may include quoted data) }
 *     { error message in English if different }
 *     { SQL statement if applicable }
 *     { [ error code - build number ] }
 *  </pre>
 *  Example:
 *  <pre>
 *      Syntax error in SQL statement "SELECT * FORM[*] TEST ";
 *      SQL statement: select * form test [42000-125]
 *  </pre>
 *  The [*] marks the position of the syntax error
 *  (FORM instead of FROM in the case).
 *  The error code is 42000, and the build number is 125,
 *  meaning version 1.2.125.
 */
object ErrorCode {
    // 02: no data
    /**
     * The error with code <code>2000</code> is thrown when
     * the result set is positioned before the first or after the last row, or
     * not on a valid row for the give operation.
     * Example of wrong usage:
     * <pre>
     *     ResultSet rs = state.executeQuery("select * from DUAL");
     *     rs.getString(1);
     *     </pre>
     *  Correct:
     *  <pre>
     *      ResultSet rs = stat.executeQuery("select * from dual");
     *      rs.next();
     *      rs.getString(1);
     *      </pre>
     */
    const val NO_DATA_AVAILABLE: Int = 2000

    // 07: dynamic SQL error
    /**
     * The error with code <code>7001</code> is thrown when
     * trying to call a function with the wrong number of parameters.
     * Example:
     * <pre>
     *     CALL ABS(1, 2)
     *     </pre>
     */
    const val INVALID_PARAMETER_COUNT_2: Int = 7001

    // 08: connection exception
    /**
     * The error with code <code>8000</code> is thrown when
     * there was a problem trying to create a database lock.
     * See the message and cause for details
     */
    const val ERROR_OPENING_DATABASE_1: Int = 8000

    // 21: cardinality violation
    /**
     * The error with code <code>21002</code> is thrown when the number of
     * columns does not match. Possible reasons are: for a INSERT or MERGE
     * statement, the column count does not match the table or the column list
     * specified. For a SELECT UNION statement, both queries return a different
     * number of columns. For a constraint, the number of referenced and
     * referencing columns does not match. Example:
     * <pre>
     *     CREATE TABLE TEST(ID INT, NAME VARCHAR);
     *     INSERT INTO TEST VALUES('Hello');
     *     </pre>
     */
    const val COLUMN_COUNT_DOES_NOT_MATCH: Int = 21002

    // 22: data exception
    /**
     * The error with code <code>22001</code> is thrown when
     * trying to insert a value that is too long for the column.
     * Example:
     * <pre>
     *     CREATE TABLE TEST(ID INT, NAME VARCHAR(2));
     *     INSERT INTO TEST VALUE(1, 'Hello');
     *     </pre>
     */
    const val VALUE_TOO_LONG_2: Int = 22001

    /**
     * The error with code <code>42101</code> is thrown when
     * trying to create a table or view if an object with the name already
     * exists. Example:
     * <pre>
     *     CREATE TABLE TEST(ID INT);
     *     CREATE TABLE TEST(ID INT PRIMARY KEY);
     *     </pre>
     */
    const val TABLE_OR_VIEW_ALREADY_EXISTS_1: Int = 42101

    /**
     * The error with the code <code>42102</code> is thrown when
     * trying to query, modify or drop a table or view that does not exists
     * in this schema and database. A common cause is that the wrong
     * database was opened.
     * Example:
     * <pre>SELECT * FROM ABC;</pre>
     */
    const val TABLE_OR_VIEW_NOT_FOUND_1: Int = 42102

    /**
     * The error with code <code>42111</code> is thrown when
     * trying to create an index if an index with the same name already exists.
     * Example:
     * <pre>
     *     CREATE TABLE TEST(ID INT, NAME VARCHAR);
     *     CREATE INDEX IDX_ID ON TEST(ID);
     *     CREATE TABLE ADDRESS(ID INT);
     *     CREATE INDEX IDX_ID ON ADDRESS(ID);
     */
    const val INDEX_ALREADY_EXISTS_1: Int = 42111

    /**
     * The error with code <code>42112</code> is thrown when
     * trying to drop or reference an index that does not exist.
     * Example:
     * <pre>
     *     DROP INDEX ABC;
     *     </pre>
     */
    const val INDEX_NOT_FOUND_1: Int = 42112

    /**
     * The error with code <code>42121</code> is thrown when trying to create
     * a table or insert into a table and use the same column name twice.
     * Example:
     * <pre>
     *     CREATE TABLE TEST(ID INT, ID INT);
     *     </pre>
     */
    const val DUPLICATE_COLUMN_NAME_1: Int = 42121

    /**
     * The error with code <code>42122</code> is thrown when
     * referencing an non-existing column.
     * Example:
     * <pre>
     *     CREATE TABLE TEST(ID INT);
     *     SELECT NAME FROM TEST;
     *     </pre>
     */
    const val COLUMN_NOT_FOUND_1: Int = 42122

    /**
     * The error with code <code>42131</code> is thrown when
     * identical expression should be used, but different
     * expressions were found.
     * Example:
     * <pre>
     *     SELECT MODE(A ORDER BY B) FROM TEST;
     * </pre>
     */
    const val IDENTICAL_EXPRESSIONS_SHOULD_BE_USED: Int = 42131

    /**
     * The error with code <code>50000</code> is thrown when
     * something unexpected occurs, for example an internal stack
     * overflow. For details about the problem, see the cause of the
     * exception in the stack trace.
     */
    const val GENERAL_ERROR_1: Int = 50000

    /**
     * The error with code <code>50004</code> is thrown when
     * creating a table with an unsupported data type, or
     * when the data type is unknown because parameters are used.
     * Example:
     * <pre>CREATE TABLE TEST(ID VERYSMALLINT);
     * </pre>
     */
    const val UNKNOWN_DATA_TYPE_1: Int = 50004

    /**
     * The error with code <code>50100</code> is thrown when calling an
     * unsupported JDBC method or database feature. See the stack trace for
     * details.
     */
    const val FEATURE_NOT_SUPPORTED_1: Int = 50100

    /**
     * The error with code <code>50200</code> is thrown when
     * another connection locked an object longer than the lock timeout
     * set for this connection, or when a deadlock occurred.
     * Example:
     * <pre>
     *     CREATE TABLE TEST(ID INT);
     *     -- connection 1:
     *     SET AUTOCOMMIT FALSE;
     *     INSERT INTO TEST VALUES(1);
     *     -- connection 2:
     *     SET AUTOCOMMIT FALSE;
     *     INSERT INTO TEST VALUES(1);
     *     </pre>
     */
    const val LOCK_TIMEOUT_1: Int = 50200

    /**
     * The error with code <code>90000</code> is thrown when
     * a function that does not return a result set was used in the FORM clause.
     * Example:
     * <pre>SELECT * FROM SIN(1);</pre>
     */
    const val FUNCTION_MUST_RETURN_RESULT_SET_1: Int = 90000

    /**
     * The error with code <code>90001</code> is thrown when
     * Statement.executeUpdate() was called for a SELECT statement.
     * This is not allowed according to the JDBC specs.
     */
    const val METHOD_NOT_ALLOWED_FOR_QUERY: Int = 90001

    /**
     * The error with code <code>90002</code> is thrown when
     * Statement.executeQuery() was called for a statement that does
     * not return a result set (for example, an UPDATE statement).
     * This is not allowed according to the JDBC specs.
     */
    const val METHOD_ONLY_ALLOWED_FOR_QUERY: Int = 90002

    /**
     * The error with code <code>90003</code> is thrown when
     * trying to convert a string to a binary value. Two hex digits
     * per byte are required. Example of wrong usage:
     * <pre>
     *     CALL X'00023';
     *     Hexadecimal string with odd number of characters: 00023
     *     </pre>
     *     Correct:
     *     <pre> CALL X'000023';</pre>
     */
    const val HEX_STRING_ODD_1: Int = 90003

    /**
     * The error with code <code>90004</code> is thrown when
     * trying to convert a text to binary, but the expression contains
     * a non-hexadecimal character.
     * Example:
     * <pre> CALL X'ABCDEFGH';
     * CALL CAST('ABCDEFGH' AS BINARY);
     * </pre>
     * Conversion from text to binary is supported, but the text must
     * represent the hexadecimal encoded bytes.
     */
    const val HEX_STRING_WRONG_1: Int = 90004

    /**
     * The error with code <code>90005</code> is thrown when
     * trying to create a trigger and using the combination of SELECT
     * and FOR EACH ROW, which we do not support.
     */
    const val TRIGGER_SELECT_AND_ROW_BASED_NOT_SUPPORTED: Int = 90005

    /**
     * The error with code <code>90006</code> is thrown when
     * trying to get a value from a sequence that has run out of numbers
     * and does not have cycling enabled.
     */
    const val SEQUENCE_EXHAUSTED: Int = 90006

    /**
     * The error with code <code>90007</code> is thrown when
     * trying to call a JDBC method on an object that has been closed.
     */
    const val OBJECT_CLOSED: Int = 90007

    /**
     * The error with code <code>90008</code> is thrown when trying to use
     * a value that is not valid for teh given operation.
     * Example:
     * <pre>CREATE SEQUENCE TEST INCREMENT 0;</pre>
     */

    /**
     * The error with code <code>90026</code> is thrown when an object could not
     * be serialized.
     */
    const val SERIALIZATION_FAILED_1: Int = 90026

    /**
     * The error with code <code>90027</code> is thrown when an object could not
     * be de-serialized.
     */
    const val DESERIALIZATION_FAILED_1: Int = 90027

    /**
     * The error with code <code>90028</code> is thrown when
     * an input / output error occurred. For more information, see the root
     * cause of the exception.
     */
    const val IO_EXCEPTION_1: Int = 90028

    /**
     * The error with code <code>90029</code> is thrown when
     * calling ResultSet.deleteRow(), insertRow(), or updateRow()
     * when then current row is not updatable.
     * Example:
     * <pre>
     *     ResultSet rs = stat.executeQuery("SELECT * FROM REST");
     *     rs.next();
     *     rs.insertRow();
     *     </pre>
     */
    const val NOT_ON_UPDATABLE_ROW: Int = 90029

    /**
     * The error with code <code>90029</code> is thrown when
     * the database engine has detected a checksum mismatch in the data
     * or index. To solve this problem, restore a backup or use the
     * Recovery tool (org.h2.tools.Recover).
     */
    const val FILE_CORRUPTED_1: Int = 90030

    /**
     * The error with code <code>90095</code> is thrown when
     * calling the method STRINGDECODE with an invalid escape sequence.
     * Only Java style escape sequences and java properties file escape
     * sequences are supported.
     * Example:
     * <pre>CALL STRINGDECODE('\i');</pre>
     */
    const val STRING_FORMAT_ERROR_1: Int = 90095

    /**
     * The error with code <code>90105</code> is thrown when
     * an exception occurred in a user-defined method.
     * Example:
     * <pre>
     *     CREATE ALIAS SYS_PROP FOR "java.lang.System.getProperty";
     *     CALL SYS_PROP(NULL);
     *     </pre>
     */
    const val EXCEPTION_IN_FUNCTION_1: Int = 90105

    /**
     * The error with code <code>90106</code> is thrown when
     * trying to truncate a table that can not be truncated.
     * Tables with referential integrity constraints can not be truncated.
     * Also, system tables and view can not be truncated.
     * Example:
     * <pre>
     *     TRUNCATE TABLE INFORMATION_SCHEMA.SETTINGS;
     *     </pre>
     */
    const val CANNOT_TRUNCATE_1: Int = 90106

    /**
     * The error with code <code>90107</code> is thrown when
     * trying to drop an object because another object would become invalid.
     * Example:
     * <pre>
     * CREATE TABLE COUNT(X INT);
     * CREATE TABLE ITEMS(ID INT DEFAULT SELECT MAX(X)+1 FROM COUNT);
     * DROP TABLE COUNT;
     * </pre>
     */
    const val CANNOT_DROP_2: Int = 90107

    /**
     * The error with code `90108` is thrown when not enough heap
     * memory was available. A possible solutions is to increase the memory size
     * using `java -Xmx128m ...`. Another solution is to reduce
     * the cache size.
     */
    const val OUT_OF_MEMORY: Int = 90108

    /**
     * INTERNAL
     */
    @JvmStatic
    fun getState(errorCode: Int): String = when (errorCode) {
        // To convert SQLState to error code, replace
        // 21S:210, 42S:421, HY:50, C:1, T:2

        //02: no data
        NO_DATA_AVAILABLE -> "02000"
        //07:dynamic SQL error
        INVALID_PARAMETER_COUNT_2 -> "07001"
        // 08: connection exception
        ERROR_OPENING_DATABASE_1 -> "08000"
        //21: cardinality violation
        COLUMN_COUNT_DOES_NOT_MATCH -> "21S02"
        //42: syntax error or access rule violation
        TABLE_OR_VIEW_ALREADY_EXISTS_1 -> "42S01"
        TABLE_OR_VIEW_NOT_FOUND_1 -> "42S02"
        INDEX_ALREADY_EXISTS_1 -> "42S11"
        INDEX_NOT_FOUND_1 -> "42S12"
        DUPLICATE_COLUMN_NAME_1 -> "42S21"
        COLUMN_NOT_FOUND_1 -> "42S22"
        IDENTICAL_EXPRESSIONS_SHOULD_BE_USED -> "42S31"
        //OA: feature not supported
        //HZ: remote database access
        //HY
        GENERAL_ERROR_1 -> "HY000"
        UNKNOWN_DATA_TYPE_1 -> "HY004"
        FEATURE_NOT_SUPPORTED_1 -> "HYC00"
        LOCK_TIMEOUT_1 -> "HYT00"
        else -> errorCode.toString()
    }
}