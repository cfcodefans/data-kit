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
     * The error with code `22034` is thrown when an
     * attempt is made to read non-existing element of an array.
     *
     * Example:
     * <pre>
     * VALUES ARRAY[1, 2][3]
    </pre> *
     */
    const val ARRAY_ELEMENT_ERROR_2 = 22034

    // 23: constraint violation

    // 23: constraint violation
    /**
     * The error with code `23502` is thrown when
     * trying to insert NULL into a column that does not allow NULL.
     * Example:
     * <pre>
     * CREATE TABLE TEST(ID INT, NAME VARCHAR NOT NULL);
     * INSERT INTO TEST(ID) VALUES(1);
    </pre> *
     */
    const val NULL_NOT_ALLOWED = 23502

    /**
     * The error with code `23503` is thrown when trying to delete
     * or update a row when this would violate a referential constraint, because
     * there is a child row that would become an orphan. Example:
     * <pre>
     * CREATE TABLE TEST(ID INT PRIMARY KEY, PARENT INT);
     * INSERT INTO TEST VALUES(1, 1), (2, 1);
     * ALTER TABLE TEST ADD CONSTRAINT TEST_ID_PARENT
     * FOREIGN KEY(PARENT) REFERENCES TEST(ID) ON DELETE RESTRICT;
     * DELETE FROM TEST WHERE ID = 1;
    </pre> *
     */
    const val REFERENTIAL_INTEGRITY_VIOLATED_CHILD_EXISTS_1 = 23503

    /**
     * The error with code `23505` is thrown when trying to insert
     * a row that would violate a unique index or primary key. Example:
     * <pre>
     * CREATE TABLE TEST(ID INT PRIMARY KEY);
     * INSERT INTO TEST VALUES(1);
     * INSERT INTO TEST VALUES(1);
    </pre> *
     */
    const val DUPLICATE_KEY_1 = 23505

    /**
     * The error with code `23506` is thrown when trying to insert
     * or update a row that would violate a referential constraint, because the
     * referenced row does not exist. Example:
     * <pre>
     * CREATE TABLE PARENT(ID INT PRIMARY KEY);
     * CREATE TABLE CHILD(P_ID INT REFERENCES PARENT(ID));
     * INSERT INTO CHILD VALUES(1);
    </pre> *
     */
    const val REFERENTIAL_INTEGRITY_VIOLATED_PARENT_MISSING_1 = 23506

    /**
     * The error with code `23507` is thrown when
     * updating or deleting from a table with a foreign key constraint
     * that should set the default value, but there is no default value defined.
     * Example:
     * <pre>
     * CREATE TABLE TEST(ID INT PRIMARY KEY, PARENT INT);
     * INSERT INTO TEST VALUES(1, 1), (2, 1);
     * ALTER TABLE TEST ADD CONSTRAINT TEST_ID_PARENT
     * FOREIGN KEY(PARENT) REFERENCES TEST(ID) ON DELETE SET DEFAULT;
     * DELETE FROM TEST WHERE ID = 1;
    </pre> *
     */
    const val NO_DEFAULT_SET_1 = 23507

    /**
     * The error with code `23513` is thrown when
     * a check constraint is violated. Example:
     * <pre>
     * CREATE TABLE TEST(ID INT CHECK (ID&gt;0));
     * INSERT INTO TEST VALUES(0);
    </pre> *
     */
    const val CHECK_CONSTRAINT_VIOLATED_1 = 23513

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

    // 3B: savepoint exception
    /**
     * The error with code `40001` is thrown when
     * the database engine has detected a deadlock. The transaction of this
     * session has been rolled back to solve the problem. A deadlock occurs when
     * a session tries to lock a table another session has locked, while the
     * other session wants to lock a table the first session has locked. As an
     * example, session 1 has locked table A, while session 2 has locked table
     * B. If session 1 now tries to lock table B and session 2 tries to lock
     * table A, a deadlock has occurred. Deadlocks that involve more than two
     * sessions are also possible. To solve deadlock problems, an application
     * should lock tables always in the same order, such as always lock table A
     * before locking table B. For details, see [Wikipedia Deadlock](https://en.wikipedia.org/wiki/Deadlock).
     */
    const val DEADLOCK_1 = 40001

    // 42: syntax error or access rule violation
    /**
     * The error with code `42000` is thrown when
     * trying to execute an invalid SQL statement.
     * Example:
     * <pre>
     * CREATE ALIAS REMAINDER FOR "IEEEremainder";
    </pre> *
     */
    const val SYNTAX_ERROR_1 = 42000

    /**
     * The error with code `42001` is thrown when
     * trying to execute an invalid SQL statement.
     * Example:
     * <pre>
     * CREATE TABLE TEST(ID INT);
     * INSERT INTO TEST(1);
    </pre> *
     */
    const val SYNTAX_ERROR_2 = 42001

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
     * trying to query, modify or drop a table or view that does not exist
     * in this schema and database. A common cause is that the wrong
     * database was opened.
     * Example:
     * <pre>SELECT * FROM ABC;</pre>
     */
    const val TABLE_OR_VIEW_NOT_FOUND_1: Int = 42102

    /**
     * The error with code `42103` is thrown when
     * trying to query, modify or drop a table or view that does not exist
     * in this schema and database but similar names were found. A common cause
     * is that the names are written in different case.
     * Example:
     * <pre>
     * SELECT * FROM ABC;
    </pre> *
     */
    const val TABLE_OR_VIEW_NOT_FOUND_WITH_CANDIDATES_2 = 42103

    /**
     * The error with code `42104` is thrown when
     * trying to query, modify or drop a table or view that does not exist
     * in this schema and database but it is empty anyway. A common cause is
     * that the wrong database was opened.
     * Example:
     * <pre>
     * SELECT * FROM ABC;
    </pre> *
     */
    const val TABLE_OR_VIEW_NOT_FOUND_DATABASE_EMPTY_1 = 42104

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
     * The error with code `57014` is thrown when
     * a statement was canceled using Statement.cancel() or
     * when the query timeout has been reached.
     * Examples:
     * <pre>
     * stat.setQueryTimeout(1);
     * stat.cancel();
    </pre> *
     */
    const val STATEMENT_WAS_CANCELED = 57014

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
     * The error with code `90009` is thrown when
     * trying to create a sequence with an invalid combination
     * of attributes (min value, max value, start value, etc).
     */
    const val SEQUENCE_ATTRIBUTES_INVALID_7 = 90009

    /**
     * The error with code `90010` is thrown when
     * trying to format a timestamp or number using TO_CHAR
     * with an invalid format.
     */
    const val INVALID_TO_CHAR_FORMAT = 90010

    /**
     * The error with code `90011` is thrown when
     * trying to open a connection to a database using an implicit relative
     * path, such as "jdbc:h2:test" (in which case the database file would be
     * stored in the current working directory of the application). This is not
     * allowed because it can lead to confusion where the database file is, and
     * can result in multiple databases because different working directories
     * are used. Instead, use "jdbc:h2:~/name" (relative to the current user
     * home directory), use an absolute path, set the base directory (baseDir),
     * use "jdbc:h2:./name" (explicit relative path), or set the system property
     * "h2.implicitRelativePath" to "true" (to prevent this check). For Windows,
     * an absolute path also needs to include the drive ("C:/..."). Please see
     * the documentation on the supported URL format. Example:
     * <pre>
     * jdbc:h2:test
    </pre> *
     */
    const val URL_RELATIVE_TO_CWD = 90011

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
     * trying to drop a trigger that does not exist.
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
     * The error with code `90045` is thrown when trying to create a
     * constraint if an object with this name already exists. Example:
     * <pre>
     * CREATE TABLE TEST(ID INT NOT NULL);
     * ALTER TABLE TEST ADD CONSTRAINT PK PRIMARY KEY(ID);
     * ALTER TABLE TEST ADD CONSTRAINT PK PRIMARY KEY(ID);
    </pre> *
     */
    const val CONSTRAINT_ALREADY_EXISTS_1 = 90045

    /**
     * The error with code `90046` is thrown when
     * trying to open a connection to a database using an unsupported URL
     * format. Please see the documentation on the supported URL format and
     * examples. Example:
     * <pre>
     * jdbc:h2:;;
     * </pre>
     */
    const val URL_FORMAT_ERROR_2 = 90046

    /**
     * The error with code `90047` is thrown when
     * trying to connect to a TCP server with an incompatible client.
     */
    const val DRIVER_VERSION_ERROR_2 = 90047

    /**
     * The error with code `90048` is thrown when
     * the file header of a database files (*.db) does not match the
     * expected version, or if it is corrupted.
     */
    const val FILE_VERSION_ERROR_1 = 90048

    /**
     * The error with code `90049` is thrown when
     * trying to open an encrypted database with the wrong file encryption
     * password or algorithm.
     */
    const val FILE_ENCRYPTION_ERROR_1 = 90049

    /**
     * The error with code `90050` is thrown when trying to open an
     * encrypted database, but not separating the file password from the user
     * password. The file password is specified in the password field, before
     * the user password. A single space needs to be added between the file
     * password and the user password; the file password itself may not contain
     * spaces. File passwords (as well as user passwords) are case sensitive.
     * Example of wrong usage:
     * <pre>
     * String url = &quot;jdbc:h2:/test;CIPHER=AES&quot;;
     * String passwords = &quot;filePasswordUserPassword&quot;;
     * DriverManager.getConnection(url, &quot;sa&quot;, pwds);
    </pre> *
     * Correct:
     * <pre>
     * String url = &quot;jdbc:h2:/test;CIPHER=AES&quot;;
     * String passwords = &quot;filePassword userPassword&quot;;
     * DriverManager.getConnection(url, &quot;sa&quot;, pwds);
    </pre> *
     */
    const val WRONG_PASSWORD_FORMAT = 90050

    // 90051 was removed

    // 90051 was removed
    /**
     * The error with code `90052` is thrown when a single-column
     * subquery is expected but a subquery with other number of columns was
     * specified.
     * Example:
     * <pre>
     * VALUES ARRAY(SELECT A, B FROM TEST)
    </pre> *
     */
    const val SUBQUERY_IS_NOT_SINGLE_COLUMN = 90052

    /**
     * The error with code `90053` is thrown when
     * a subquery that is used as a value contains more than one row.
     * Example:
     * <pre>
     * CREATE TABLE TEST(ID INT, NAME VARCHAR);
     * INSERT INTO TEST VALUES(1, 'Hello'), (1, 'World');
     * SELECT X, (SELECT NAME FROM TEST WHERE ID=X) FROM DUAL;
    </pre> *
     */
    const val SCALAR_SUBQUERY_CONTAINS_MORE_THAN_ONE_ROW = 90053

    /**
     * The error with code `90054` is thrown when
     * an aggregate function is used where it is not allowed.
     * Example:
     * <pre>
     * CREATE TABLE TEST(ID INT);
     * INSERT INTO TEST VALUES(1), (2);
     * SELECT MAX(ID) FROM TEST WHERE ID = MAX(ID) GROUP BY ID;
    </pre> *
     */
    const val INVALID_USE_OF_AGGREGATE_FUNCTION_1 = 90054

    /**
     * The error with code `90055` is thrown when
     * trying to open a database with an unsupported cipher algorithm.
     * Supported is AES.
     * Example:
     * <pre>
     * jdbc:h2:~/test;CIPHER=DES
    </pre> *
     */
    const val UNSUPPORTED_CIPHER = 90055

    /**
     * The error with code `90056` is thrown when trying to format a
     * timestamp using TO_DATE and TO_TIMESTAMP  with an invalid format.
     */
    const val INVALID_TO_DATE_FORMAT = 90056

    /**
     * The error with code `90057` is thrown when
     * trying to drop a constraint that does not exist.
     * Example:
     * <pre>
     * CREATE TABLE TEST(ID INT);
     * ALTER TABLE TEST DROP CONSTRAINT CID;
    </pre> *
     */
    const val CONSTRAINT_NOT_FOUND_1 = 90057

    /**
     * The error with code `90058` is thrown when trying to call
     * commit or rollback inside a trigger, or when trying to call a method
     * inside a trigger that implicitly commits the current transaction, if an
     * object is locked. This is not because it would release the lock too
     * early.
     */
    const val COMMIT_ROLLBACK_NOT_ALLOWED = 90058

    /**
     * The error with code `90059` is thrown when
     * a query contains a column that could belong to multiple tables.
     * Example:
     * <pre>
     * CREATE TABLE PARENT(ID INT, NAME VARCHAR);
     * CREATE TABLE CHILD(PID INT, NAME VARCHAR);
     * SELECT ID, NAME FROM PARENT P, CHILD C WHERE P.ID = C.PID;
    </pre> *
     */
    const val AMBIGUOUS_COLUMN_NAME_1 = 90059

    /**
     * The error with code `90060` is thrown when
     * trying to use a file locking mechanism that is not supported.
     * Currently only FILE (the default) and SOCKET are supported
     * Example:
     * <pre>
     * jdbc:h2:~/test;FILE_LOCK=LDAP
    </pre> *
     */
    const val UNSUPPORTED_LOCK_METHOD_1 = 90060

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
     * The error with code `90063` is thrown when
     * trying to rollback to a savepoint that is not defined.
     * Example:
     * <pre>
     * ROLLBACK TO SAVEPOINT S_UNKNOWN;
    </pre> *
     */
    const val SAVEPOINT_IS_INVALID_1 = 90063

    /**
     * The error with code `90064` is thrown when
     * Savepoint.getSavepointName() is called on an unnamed savepoint.
     * Example:
     * <pre>
     * Savepoint sp = conn.setSavepoint();
     * sp.getSavepointName();
    </pre> *
     */
    const val SAVEPOINT_IS_UNNAMED = 90064

    /**
     * The error with code `90065` is thrown when
     * Savepoint.getSavepointId() is called on a named savepoint.
     * Example:
     * <pre>
     * Savepoint sp = conn.setSavepoint("Joe");
     * sp.getSavepointId();
    </pre> *
     */
    const val SAVEPOINT_IS_NAMED = 90065

    /**
     * The error with code `90066` is thrown when
     * the same property appears twice in the database URL or in
     * the connection properties.
     * Example:
     * <pre>
     * jdbc:h2:~/test;LOCK_TIMEOUT=0;LOCK_TIMEOUT=1
    </pre> *
     */
    const val DUPLICATE_PROPERTY_1 = 90066

    /**
     * The error with code `90067` is thrown when the client could
     * not connect to the database, or if the connection was lost. Possible
     * reasons are: the database server is not running at the given port, the
     * connection was closed due to a shutdown, or the server was stopped. Other
     * possible causes are: the server is not an H2 server, or the network
     * connection is broken.
     */
    const val CONNECTION_BROKEN_1 = 90067

    /**
     * The error with code `90068` is thrown when the given
     * expression that is used in the ORDER BY is not in the result list. This
     * is required for distinct queries, otherwise the result would be
     * ambiguous.
     * Example of wrong usage:
     * <pre>
     * CREATE TABLE TEST(ID INT, NAME VARCHAR);
     * INSERT INTO TEST VALUES(2, 'Hello'), (1, 'Hello');
     * SELECT DISTINCT NAME FROM TEST ORDER BY ID;
     * Order by expression ID must be in the result list in this case
    </pre> *
     * Correct:
     * <pre>
     * SELECT DISTINCT ID, NAME FROM TEST ORDER BY ID;
    </pre> *
     */
    const val ORDER_BY_NOT_IN_RESULT = 90068

    /**
     * The error with code `90069` is thrown when
     * trying to create a role if an object with this name already exists.
     * Example:
     * <pre>
     * CREATE ROLE TEST_ROLE;
     * CREATE ROLE TEST_ROLE;
    </pre> *
     */
    const val ROLE_ALREADY_EXISTS_1 = 90069

    /**
     * The error with code `90070` is thrown when
     * trying to drop or grant a role that does not exist.
     * Example:
     * <pre>
     * DROP ROLE TEST_ROLE_2;
    </pre> *
     */
    const val ROLE_NOT_FOUND_1 = 90070

    /**
     * The error with code `90071` is thrown when
     * trying to grant or revoke if no role or user with that name exists.
     * Example:
     * <pre>
     * GRANT SELECT ON TEST TO UNKNOWN;
    </pre> *
     */
    const val USER_OR_ROLE_NOT_FOUND_1 = 90071

    /**
     * The error with code `90072` is thrown when
     * trying to grant or revoke both roles and rights at the same time.
     * Example:
     * <pre>
     * GRANT SELECT, TEST_ROLE ON TEST TO SA;
    </pre> *
     */
    const val ROLES_AND_RIGHT_CANNOT_BE_MIXED = 90072

    /**
     * The error with code `90073` is thrown when trying to create
     * an alias for a Java method, if two methods exists in this class that have
     * this name and the same number of parameters.
     * Example of wrong usage:
     * <pre>
     * CREATE ALIAS GET_LONG FOR
     * "java.lang.Long.getLong";
    </pre> *
     * Correct:
     * <pre>
     * CREATE ALIAS GET_LONG FOR
     * "java.lang.Long.getLong(java.lang.String, java.lang.Long)";
    </pre> *
     */
    const val METHODS_MUST_HAVE_DIFFERENT_PARAMETER_COUNTS_2 = 90073

    /**
     * The error with code `90074` is thrown when
     * trying to grant a role that has already been granted.
     * Example:
     * <pre>
     * CREATE ROLE TEST_A;
     * CREATE ROLE TEST_B;
     * GRANT TEST_A TO TEST_B;
     * GRANT TEST_B TO TEST_A;
    </pre> *
     */
    const val ROLE_ALREADY_GRANTED_1 = 90074

    /**
     * The error with code `90075` is thrown when
     * trying to alter a table and allow null for a column that is part of a
     * primary key or hash index.
     * Example:
     * <pre>
     * CREATE TABLE TEST(ID INT PRIMARY KEY);
     * ALTER TABLE TEST ALTER COLUMN ID NULL;
    </pre> *
     */
    const val COLUMN_IS_PART_OF_INDEX_1 = 90075

    /**
     * The error with code `90076` is thrown when
     * trying to create a function alias for a system function or for a function
     * that is already defined.
     * Example:
     * <pre>
     * CREATE ALIAS SQRT FOR "java.lang.Math.sqrt"
    </pre> *
     */
    const val FUNCTION_ALIAS_ALREADY_EXISTS_1 = 90076

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
     * The error with code `90087` is thrown when
     * a method with matching number of arguments was not found in the class.
     * Example:
     * <pre>
     * CREATE ALIAS TO_BINARY FOR "java.lang.Long.toBinaryString(long)";
     * CALL TO_BINARY(10, 2);
    </pre> *
     */
    const val METHOD_NOT_FOUND_1 = 90087

    /**
     * The error with code `90088` is thrown when
     * trying to switch to an unknown mode.
     * Example:
     * <pre>
     * SET MODE UNKNOWN;
    </pre> *
     */
    const val UNKNOWN_MODE_1 = 90088

    /**
     * The error with code `90089` is thrown when
     * trying to change the collation while there was already data in
     * the database. The collation of the database must be set when the
     * database is empty.
     * Example of wrong usage:
     * <pre>
     * CREATE TABLE TEST(NAME VARCHAR PRIMARY KEY);
     * INSERT INTO TEST VALUES('Hello', 'World');
     * SET COLLATION DE;
     * Collation cannot be changed because there is a data table: PUBLIC.TEST
    </pre> *
     * Correct:
     * <pre>
     * SET COLLATION DE;
     * CREATE TABLE TEST(NAME VARCHAR PRIMARY KEY);
     * INSERT INTO TEST VALUES('Hello', 'World');
    </pre> *
     */
    const val COLLATION_CHANGE_WITH_DATA_TABLE_1 = 90089

    /**
     * The error with code `90090` is thrown when
     * trying to drop a schema that may not be dropped (the schema PUBLIC
     * and the schema INFORMATION_SCHEMA).
     * Example:
     * <pre>
     * DROP SCHEMA PUBLIC;
    </pre> *
     */
    const val SCHEMA_CAN_NOT_BE_DROPPED_1 = 90090

    /**
     * The error with code `90091` is thrown when
     * trying to drop the role PUBLIC.
     * Example:
     * <pre>
     * DROP ROLE PUBLIC;
    </pre> *
     */
    const val ROLE_CAN_NOT_BE_DROPPED_1 = 90091

    /**
     * The error with code `90093` is thrown when
     * trying to connect to a clustered database that runs in standalone
     * mode. This can happen if clustering is not enabled on the database,
     * or if one of the clients disabled clustering because it can not see
     * the other cluster node.
     */
    const val CLUSTER_ERROR_DATABASE_RUNS_ALONE = 90093

    /**
     * The error with code `90094` is thrown when
     * trying to connect to a clustered database that runs together with a
     * different cluster node setting than what is used when trying to connect.
     */
    const val CLUSTER_ERROR_DATABASE_RUNS_CLUSTERED_1 = 90094

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
     * The error with code `90096` is thrown when
     * trying to perform an operation with a non-admin user if the
     * user does not have enough rights.
     */
    const val NOT_ENOUGH_RIGHTS_FOR_1 = 90096

    /**
     * The error with code `90097` is thrown when
     * trying to delete or update a database if it is open in read-only mode.
     * Example:
     * <pre>
     * jdbc:h2:~/test;ACCESS_MODE_DATA=R
     * CREATE TABLE TEST(ID INT);
    </pre> *
     */
    const val DATABASE_IS_READ_ONLY = 90097

    /**
     * The error with code `90098` is thrown when the database has
     * been closed, for example because the system ran out of memory or because
     * the self-destruction counter has reached zero. This counter is only used
     * for recovery testing, and not set in normal operation.
     */
    const val DATABASE_IS_CLOSED = 90098

    /**
     * The error with code `90099` is thrown when an error occurred
     * trying to initialize the database event listener. Example:
     * <pre>
     * jdbc:h2:/test;DATABASE_EVENT_LISTENER='java.lang.String'
    </pre> *
     */
    const val ERROR_SETTING_DATABASE_EVENT_LISTENER_2 = 90099

    /**
     * The error with code `90101` is thrown when
     * the XA API detected unsupported transaction names. This can happen
     * when mixing application generated transaction names and transaction names
     * generated by this databases XAConnection API.
     */
    const val WRONG_XID_FORMAT_1 = 90101

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
     * The error with code `90109` is thrown when
     * trying to run a query against an invalid view.
     * Example:
     * <pre>
     * CREATE FORCE VIEW TEST_VIEW AS SELECT * FROM TEST;
     * SELECT * FROM TEST_VIEW;
    </pre> *
     */
    const val VIEW_IS_INVALID_2 = 90109

    /**
     * The error with code `90110` is thrown when
     * trying to compare values of incomparable data types.
     * Example:
     * <pre>
     * CREATE TABLE test (id INT NOT NULL, name VARCHAR);
     * select * from test where id = (1, 2);
    </pre> *
     */
    const val TYPES_ARE_NOT_COMPARABLE_2 = 90110

    /**
     * The error with code `90111` is thrown when
     * an exception occurred while accessing a linked table.
     */
    const val ERROR_ACCESSING_LINKED_TABLE_2 = 90111

    /**
     * The error with code `90112` is thrown when a row was deleted
     * twice while locking was disabled. This is an intern exception that should
     * never be thrown to the application, because such deleted should be
     * detected and the resulting exception ignored inside the database engine.
     * <pre>
     * Row not found when trying to delete from index UID_INDEX_0
    </pre> *
     */
    const val ROW_NOT_FOUND_WHEN_DELETING_1 = 90112

    /**
     * The error with code `90131` is thrown when using multi version
     * concurrency control, and trying to update the same row from within two
     * connections at the same time, or trying to insert two rows with the same
     * key from two connections. Example:
     * <pre>
     * jdbc:h2:~/test
     * Session 1:
     * CREATE TABLE TEST(ID INT);
     * INSERT INTO TEST VALUES(1);
     * SET AUTOCOMMIT FALSE;
     * UPDATE TEST SET ID = 2;
     * Session 2:
     * SET AUTOCOMMIT FALSE;
     * UPDATE TEST SET ID = 3;
    </pre> *
     */
    const val CONCURRENT_UPDATE_1 = 90131

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
     * The error with code `90135` is thrown when
     * trying to open a connection to a database that is currently open
     * in exclusive mode. The exclusive mode is set using:
     * <pre>
     * SET EXCLUSIVE TRUE;
    </pre> *
     */
    const val DATABASE_IS_IN_EXCLUSIVE_MODE = 90135

    /**
     * The error with code `90136` is thrown when
     * trying to reference a window that does not exist.
     * Example:
     * <pre>
     * SELECT LEAD(X) OVER W FROM TEST;
    </pre> *
     */
    const val WINDOW_NOT_FOUND_1 = 90136

    /**
     * The error with code `90137` is thrown when
     * trying to assign a value to something that is not a variable.
     * <pre>
     * SELECT AMOUNT, SET(@V, COALESCE(@V, 0)+AMOUNT) FROM TEST;
    </pre> *
     */
    const val CAN_ONLY_ASSIGN_TO_VARIABLE_1 = 90137

    /**
     * The error with code `90138` is thrown when
     *
     * trying to open a persistent database using an incorrect database name.
     * The name of a persistent database contains the path and file name prefix
     * where the data is stored. The file name part of a database name must be
     * at least two characters.
     *
     * Example of wrong usage:
     * <pre>
     * DriverManager.getConnection("jdbc:h2:~/t");
     * DriverManager.getConnection("jdbc:h2:~/test/");
    </pre> *
     * Correct:
     * <pre>
     * DriverManager.getConnection("jdbc:h2:~/te");
     * DriverManager.getConnection("jdbc:h2:~/test/te");
    </pre> *
     */
    const val INVALID_DATABASE_NAME_1 = 90138

    /**
     * The error with code `90141` is thrown when
     * The error with code `90154` is thrown when trying to assign a
     * trying to change the java object serializer while there was already data
     * in the database. The serializer of the database must be set when the
     * database is empty.
     */
    const val JAVA_OBJECT_SERIALIZER_CHANGE_WITH_DATA_TABLE = 90141

    /**
     * The error with code `90142` is thrown when
     * trying to set zero for step size.
     */
    const val STEP_SIZE_MUST_NOT_BE_ZERO = 90142

    /**
     * The error with code `90143` is thrown when
     * trying to fetch a row from the primary index and the row is not there.
     */
    const val ROW_NOT_FOUND_IN_PRIMARY_INDEX = 90143

    /**
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

    /**
     * INTERNAL
     * @param errorCode to check
     * @return true if provided code is common, false otherwise
     */
    fun isCommon(errorCode: Int): Boolean {
// this list is sorted alphabetically
        return when (errorCode) {
            DATA_CONVERSION_ERROR_1,
            ErrorCode.DUPLICATE_KEY_1,
            ErrorCode.FUNCTION_ALIAS_ALREADY_EXISTS_1,
            LOCK_TIMEOUT_1, NULL_NOT_ALLOWED,
            NO_DATA_AVAILABLE,
            NUMERIC_VALUE_OUT_OF_RANGE_1,
            OBJECT_CLOSED,
            ErrorCode.REFERENTIAL_INTEGRITY_VIOLATED_CHILD_EXISTS_1,
            ErrorCode.REFERENTIAL_INTEGRITY_VIOLATED_PARENT_MISSING_1,
            ErrorCode.SYNTAX_ERROR_1,
            ErrorCode.SYNTAX_ERROR_2,
            TABLE_OR_VIEW_ALREADY_EXISTS_1,
            TABLE_OR_VIEW_NOT_FOUND_1,
            ErrorCode.TABLE_OR_VIEW_NOT_FOUND_WITH_CANDIDATES_2,
            ErrorCode.TABLE_OR_VIEW_NOT_FOUND_DATABASE_EMPTY_1,
            VALUE_TOO_LONG_2 -> true

            else -> false
        }
    }
}