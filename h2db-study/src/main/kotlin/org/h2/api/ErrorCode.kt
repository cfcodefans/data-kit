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
     * The error with code `22003` is thrown when a value is out of
     * range when converting to another data type. Example:
     * <pre>
     * CALL CAST(1000000 AS TINYINT);
     * SELECT CAST(124.34 AS DECIMAL(2, 2));
    </pre> *
     */
    const val NUMERIC_VALUE_OUT_OF_RANGE_1 = 22003

    /**
     * The error with code `22004` is thrown when a value is out of
     * range when converting to another column's data type.
     */
    const val NUMERIC_VALUE_OUT_OF_RANGE_2 = 22004

    /**
     * The error with code `22007` is thrown when
     * a text can not be converted to a date, time, or timestamp constant.
     * Examples:
     * <pre>
     * CALL DATE '2007-January-01';
     * CALL TIME '14:61:00';
     * CALL TIMESTAMP '2001-02-30 12:00:00';
    </pre> *
     */
    const val INVALID_DATETIME_CONSTANT_2 = 22007

    /**
     * The error with code `22012` is thrown when trying to divide
     * a value by zero. Example:
     * <pre>
     * CALL 1/0;
    </pre> *
     */
    const val DIVISION_BY_ZERO_1 = 22012

    /**
     * The error with code `22013` is thrown when preceding or
     * following size in a window function is null or negative. Example:
     * <pre>
     * FIRST_VALUE(N) OVER(ORDER BY N ROWS -1 PRECEDING)
    </pre> *
     */
    const val INVALID_PRECEDING_OR_FOLLOWING_1 = 22013

    /**
     * The error with code `22025` is thrown when using an invalid
     * escape character sequence for LIKE or REGEXP. The default escape
     * character is '\'. The escape character is required when searching for
     * the characters '%', '_' and the escape character itself. That means if
     * you want to search for the text '10%', you need to use LIKE '10\%'. If
     * you want to search for 'C:\temp' you need to use 'C:\\temp'. The escape
     * character can be changed using the ESCAPE clause as in LIKE '10+%' ESCAPE
     * '+'. Example of wrong usage:
     * <pre>
     * CALL 'C:\temp' LIKE 'C:\temp';
     * CALL '1+1' LIKE '1+1' ESCAPE '+';
    </pre> *
     * Correct:
     * <pre>
     * CALL 'C:\temp' LIKE 'C:\\temp';
     * CALL '1+1' LIKE '1++1' ESCAPE '+';
    </pre> *
     */
    const val LIKE_ESCAPE_ERROR_1 = 22025

    /**
     * The error with code `22018` is thrown when
     * trying to convert a value to a data type where the conversion is
     * undefined, or when an error occurred trying to convert. Example:
     * <pre>
     * CALL CAST(DATE '2001-01-01' AS BOOLEAN);
     * CALL CAST('CHF 99.95' AS INT);
    </pre> *
     */
    const val DATA_CONVERSION_ERROR_1 = 22018

    /**
     * The error with code `22030` is thrown when
     * an attempt is made to INSERT or UPDATE an ENUM-typed cell,
     * but the value is not one of the values enumerated by the
     * type.
     *
     * Example:
     * <pre>
     * CREATE TABLE TEST(CASE ENUM('sensitive','insensitive'));
     * INSERT INTO TEST VALUES('snake');
    </pre> *
     */
    const val ENUM_VALUE_NOT_PERMITTED = 22030

    /**
     * The error with code `22032` is thrown when an
     * attempt is made to add or modify an ENUM-typed column so
     * that one or more of its enumerators would be empty.
     *
     * Example:
     * <pre>
     * CREATE TABLE TEST(CASE ENUM(' '));
    </pre> *
     */
    const val ENUM_EMPTY = 22032

    /**
     * The error with code `22033` is thrown when an
     * attempt is made to add or modify an ENUM-typed column so
     * that it would have duplicate values.
     *
     * Example:
     * <pre>
     * CREATE TABLE TEST(CASE ENUM('sensitive', 'sensitive'));
    </pre> *
     */
    const val ENUM_DUPLICATE = 22033

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
     * The error with code `42602` is thrown when
     * invalid name of identifier is used.
     * Example:
     * <pre>
     * statement.enquoteIdentifier("\"", true);
    </pre> *
     */
    const val INVALID_NAME_1 = 42602

    /**
     * The error with code `42622` is thrown when
     * name of identifier is too long.
     * Example:
     * <pre>
     * char[] c = new char[1000];
     * Arrays.fill(c, 'A');
     * statement.executeQuery("SELECT 1 " + new String(c));
    </pre> *
     */
    const val NAME_TOO_LONG_2 = 42622

    // 54: program limit exceeded
    /**
     * The error with code `54011` is thrown when
     * too many columns were specified in a table, select statement,
     * or row value.
     * Example:
     * <pre>
     * CREATE TABLE TEST(C1 INTEGER, C2 INTEGER, ..., C20000 INTEGER);
    </pre> *
     */
    const val TOO_MANY_COLUMNS_1 = 54011

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
     * The error with code <code>90008</code> is thrown when
     * trying to use a value that is not valid for the give operation.
     * Example:
     * <pre>
     *     CREATE SEQUENCE TEST INCREMENT 0;
     * </pre>
     */
    const val INVALID_VALUE_2: Int = 90008

    /**
     * The error with code <code>90019</code> is thrown when
     * trying to drop the current user, if there are no other admin users.
     * Example:
     * <pre>DROP USER SA;</pre>
     */
    const val CANNOT_DROP_CURRENT_USER: Int = 90019

    /**
     * The error with code <code>90020</code> is thrown when trying to open a
     * database in embedded mode if this database is already in use in another
     * process (or in a different class loader). Multiple connections to the
     * same database are supported in the following cases:
     * <ul>
     *     <li>In embedded mode (URL of the form jdbc:h2:~/test) if all
     * connections are opened within the same process and class loader.
     * </li>
     * <li>In server and cluster mode (URL of the form
     * jdbc:h2:tcp://localhost/test) using remote connections.
     * </li></ul>
     * The mixed mode is also supported. This mode requires to start a server
     * in the same process where the database is open in embedded mode.
     */
    const val DATABASE_ALREADY_OPEN_1: Int = 90020

    /**
     * The error with code <code>90021</code> is thrown when
     * trying to change a specific database property that conflicts with other
     * database properties.
     */
    const val UNSUPPORTED_SETTING_COMBINATION: Int = 90021

    /**
     * The error with code <code>90022</code> is thrown when
     * trying to call a unknown function.
     * Example:
     * <pre>
     *     CALL SPECIAL_SIN(10);
     *     </pre>
     */
    const val FUNCTION_NOT_FOUND_1: Int = 90022

    /**
     * The error with code <code>90023</code> is thrown when
     * trying to set a primary key on a nullable column.
     * Example:
     * <pre>
     *     CREATE TABLE TEST(ID INT, NAME VARCHAR);
     *     ALTER TABLE TEST ADD CONSTRAINT PK PRIMARY KEY(ID);
     *     </pre>
     */
    const val COLUMN_MUST_NOT_BE_NULLABLE_1: Int = 90023

    /**
     * The error with code <code>90024</code> is thrown when
     * a file could not be renamed.
     */
    const val FILE_RENAME_FAILED_2: Int = 90024

    /**
     * The error with code <code>90025</code> is thrown when a file
     * could not be deleted, because it is still in use (only in Windows),
     * or because an error occurred when deleting.
     */
    const val FILE_DELETE_FAILED_1: Int = 90025

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
     * The error with code <code>90031</code> is thrown when
     * an input / output error occurred. For more information, see the root
     * cause of the exception.
     */
    const val IO_EXCEPTION_2: Int = 90031

    /**
     * The error with code <code>90032</code> is thrown when
     * trying to drop or alter a user that does not exist.
     * Example:
     * <pre>DROP USER TEST_USER;</pre>
     */
    const val USER_NOT_FOUND_1: Int = 90032

    /**
     * The error with code <code>90033</code> is thrown when
     * trying to create a user or role if a user with this name already exists.
     * Example:
     * <pre>
     *     CREATE USER TEST_USER;
     *     CREATE USER TEST_USER;
     * </pre>
     */
    const val USER_ALREADY_EXISTS_1: Int = 90033

    /**
     * The error with code <code>90034</code> is thrown when
     * writing to the trace file failed, for example because there
     * is an I/O exception. This message is printed to System.out,
     * but only once.
     */
    const val TRACE_FILE_ERROR_2: Int = 90034

    /**
     * The error with code <code>90035</code> is thrown when
     * trying to create a sequence if a sequence with this name already
     * exists.
     * Example:
     * <pre>
     *     CREATE SEQUENCE TEST_SEQ;
     *     CREATE SEQUENCE TEST_SEQ;
     *     </pre>
     */
    const val SEQUENCE_ALREADY_EXISTS_1: Int = 90035

    /**
     * The error with code <code>90036</code> is thrown when
     * trying to access a sequence that does not exist.
     * Example:
     * <pre>SELECT NEXT VALUE FOR SEQUENCE XYZ;</pre>
     */
    const val SEQUENCE_NOT_FOUND_1: Int = 90036

    /**
     * The error with code <code>90037</code> is thrown when
     * trying to drop or alter a view that does not exist.
     * Example:
     * <pre>DROP VIEW XYZ;</pre>
     */
    const val VIEW_NOT_FOUND_1: Int = 90037

    /**
     * The error with code <code>90037</code> is thrown when
     * trying to create a view if a view with this name already
     * exists.
     * Example:
     * <pre>
     *     CREATE_VIEW DUMMY AS SELECT * FROM DUAL;
     *     CREATE_VIEW DUMMY AS SELECT * FROM DUAL;
     *     </pre>
     */
    const val VIEW_ALREADY_EXISTS_1: Int = 90038

    /**
     * The error with code <code>90039</code> is thrown when
     * trying to access a CLOB or BLOB object that time out.
     * See the database setting LOB_TIMEOUT.
     */
    const val LOB_CLOSED_NO_TIMEOUT_1: Int = 90039

    /**
     * The error with code <code>90040</code> is thrown when
     * a user that is not administrator tries to execute a statement
     * that requires admin privileges.
     */
    const val ADMIN_RIGHTS_REQUIRED: Int = 90040

    /**
     * The error with code <code>90041</code> is thrown when
     * trying to create a trigger and there is already a trigger with that name.
     * <pre>
     *     CREATE TABLE TEST (ID INT);
     *     CREATE TRIGGER TRIGGER_A AFTER INSERT ON TEST
     *          CALL "org.h2.samples.TriggerSample$MyTrigger";
     *     CREATE TRIGGER TRIGGER_A AFTER INSERT ON TEST
     *          CALL "org.h2.samples.TriggerSample$MyTrigger";
     * </pre>
     */
    const val TRIGGER_ALREADY_EXISTS_1: Int = 90041

    /**
     * The erro with code <code>90042</code> is thrown when
     * trying to drop a trigger that does not exists.
     * Example:
     * <pre>
     *     DROP TRIGGER TRIGGER_XYZ;
     *     </pre>
     */
    const val TRIGGER_NOT_FOUND_1: Int = 90042

    /**
     * The error with code <code>90043</code> is thrown when
     * there is an error initializing the trigger, for example because the
     * class does not implement the Trigger interface.
     * See the root cause for details.
     * Example:
     * <pre>
     *     CREATE TABLE TEST (ID INT);
     *     CREATE TRIGGER TRIGGER_A AFTER INSERT ON TEST
     *          CALL "java.lang.String";
     *          </pre>
     */
    const val ERROR_CREATING_TRIGGER_OBJECT_3: Int = 90043

    /**
     * The error with code <code>90044</code> is thrown when
     * an exception or error occurred while calling the triggers fire method.
     * See the root cause for details.
     */
    const val ERROR_EXECUTING_TRIGGER_3: Int = 90044

    /**
     * The error with code <code>90061</code> is sthrown when
     * trying to start a server if a server is already running at the same port.
     * It could also be a firewall problem. To find out if another server is
     * already running, run the following command on Windows:
     * <pre> netstat -ano</pre>
     * The column PID is the process id as listed in the Task Manager.
     * For Linux, use:
     * <pre>netstat -npl</pre>
     */
    const val EXCEPTION_OPENING_PORT_2: Int = 90061

    /**
     * The error with code <code>90062</code> is thrown when
     * a directory or file could not be created. This can occur when
     * trying to create a directory if a file with the same name already
     * exists, or vice versa
     */
    const val FILE_CREATION_FAILED_1: Int = 90062

    /**
     * The error with code `90086` is thrown when
     * a class can not be loaded because it is not in the classpath
     * or because a related class is not in the classpath.
     * Example:
     * <pre>
     * CREATE ALIAS TEST FOR "java.lang.invalid.Math.sqrt";
    </pre> *
     */
    const val CLASS_NOT_FOUND_1: Int = 90086

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
     * The error with code <code>90140</code> is thrown when trying to update or
     * delete a row in a result set if the statement was not created with
     * updatable concurrency. Result sets are only updatable if the statement
     * was created with updatable concurrency, and if the result set contains
     * all columns of the primary key or of a unique index of a table.
     */
    const val RESULT_SET_READONLY: Int = 901040

    /**
     * The error with code `90124` is thrown when
     * trying to access a file that doesn't exist. This can occur when trying to
     * read a lob if the lob file has been deleted by another application.
     */
    const val FILE_NOT_FOUND_1: Int = 90124

    /**
     * The error with code `90125` is thrown when
     * PreparedStatement.setBigDecimal is called with object that extends the
     * class BigDecimal, and the system property h2.allowBigDecimalExtensions is
     * not set. Using extensions of BigDecimal is dangerous because the database
     * relies on the behavior of BigDecimal. Example of wrong usage:
     * <pre>
     * BigDecimal bd = new MyDecimal("$10.3");
     * prep.setBigDecimal(1, bd);
     * Invalid class, expected java.math.BigDecimal but got MyDecimal
     * </pre>
     * Correct:
     * <pre>
     * BigDecimal bd = new BigDecimal(&quot;10.3&quot;);
     * prep.setBigDecimal(1, bd);
     * </pre>
     */
    const val INVALID_CLASS_2: Int = 90125

    /**
     * The error with code `90128` is thrown when
     * trying to call a method of the ResultSet that is only supported
     * for scrollable result sets, and the result set is not scrollable.
     * Example:
     * <pre>
     * rs.first();
    </pre> *
     */
    const val RESULT_SET_NOT_SCROLLABLE: Int = 90128

    /**
     * The error with code `90134` is thrown when
     * trying to load a Java class that is not part of the allowed classes. By
     * default, all classes are allowed, but this can be changed using the
     * system property h2.allowedClasses.
     */
    const val ACCESS_DENIED_TO_CLASS_1: Int = 90134

    /**
     * The error with code `90154` is thrown when trying to assign a
     * value to a generated column.
     *
     * <pre>
     * CREATE TABLE TEST(A INT, B INT GENERATED ALWAYS AS (A + 1));
     * INSERT INTO TEST(A, B) VALUES (1, 1);
    </pre> *
     */
    const val GENERATED_COLUMN_CANNOT_BE_ASSIGNED_1 = 90154

    /**
     * The error with code `90155` is thrown when trying to create a
     * referential constraint that can update a referenced generated column.
     *
     * <pre>
     * CREATE TABLE PARENT(ID INT PRIMARY KEY, K INT GENERATED ALWAYS AS (ID) UNIQUE);
     * CREATE TABLE CHILD(ID INT PRIMARY KEY, P INT);
     * ALTER TABLE CHILD ADD FOREIGN KEY(P) REFERENCES PARENT(K) ON DELETE SET NULL;
    </pre> *
     */
    const val GENERATED_COLUMN_CANNOT_BE_UPDATABLE_BY_CONSTRAINT_2 = 90155


    /**
     * The error with code `90156` is thrown when trying to create a
     * view or a table from a select and some expression doesn't have a column
     * name or alias when it is required by a compatibility mode.
     *
     * <pre>
     * SET MODE DB2;
     * CREATE TABLE T1(A INT, B INT);
     * CREATE TABLE T2 AS (SELECT A + B FROM T1) WITH DATA;
    </pre> *
     */
    const val COLUMN_ALIAS_IS_NOT_SPECIFIED_1 = 90156

    /**
     * The error with code `90157` is thrown when the integer
     * index that is used in the GROUP BY is not in the SELECT list
     */
    const val GROUP_BY_NOT_IN_THE_RESULT = 90157

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