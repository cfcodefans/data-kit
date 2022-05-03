package org.h2.command

import org.h2.expression.ParameterInterface
import org.h2.result.ResultInterface
import org.h2.result.ResultWithGeneratedKeys

/**
 * Represents a SQL statement
 */
interface CommandInterface : AutoCloseable {
    companion object {
        /**
         * The type for unknown statement.
         */
        const val UNKNOWN = 0

        // ddl operations

        // ddl operations
        /**
         * The type of a ALTER INDEX RENAME statement.
         */
        const val ALTER_INDEX_RENAME = 1

        /**
         * The type of an ALTER SCHEMA RENAME statement.
         */
        const val ALTER_SCHEMA_RENAME = 2

        /**
         * The type of an ALTER TABLE ADD CHECK statement.
         */
        const val ALTER_TABLE_ADD_CONSTRAINT_CHECK = 3

        /**
         * The type of an ALTER TABLE ADD UNIQUE statement.
         */
        const val ALTER_TABLE_ADD_CONSTRAINT_UNIQUE = 4

        /**
         * The type of an ALTER TABLE ADD FOREIGN KEY statement.
         */
        const val ALTER_TABLE_ADD_CONSTRAINT_REFERENTIAL = 5

        /**
         * The type of an ALTER TABLE ADD PRIMARY KEY statement.
         */
        const val ALTER_TABLE_ADD_CONSTRAINT_PRIMARY_KEY = 6

        /**
         * The type of an ALTER TABLE ADD statement.
         */
        const val ALTER_TABLE_ADD_COLUMN = 7

        /**
         * The type of an ALTER TABLE ALTER COLUMN SET NOT NULL statement.
         */
        const val ALTER_TABLE_ALTER_COLUMN_NOT_NULL = 8

        /**
         * The type of an ALTER TABLE ALTER COLUMN DROP NOT NULL statement.
         */
        const val ALTER_TABLE_ALTER_COLUMN_DROP_NOT_NULL = 9

        /**
         * The type of an ALTER TABLE ALTER COLUMN SET DEFAULT and ALTER TABLE ALTER
         * COLUMN DROP DEFAULT statements.
         */
        const val ALTER_TABLE_ALTER_COLUMN_DEFAULT = 10

        /**
         * The type of an ALTER TABLE ALTER COLUMN statement that changes the column
         * data type.
         */
        const val ALTER_TABLE_ALTER_COLUMN_CHANGE_TYPE = 11

        /**
         * The type of an ALTER TABLE DROP COLUMN statement.
         */
        const val ALTER_TABLE_DROP_COLUMN = 12

        /**
         * The type of an ALTER TABLE ALTER COLUMN SELECTIVITY statement.
         */
        const val ALTER_TABLE_ALTER_COLUMN_SELECTIVITY = 13

        /**
         * The type of an ALTER TABLE DROP CONSTRAINT statement.
         */
        const val ALTER_TABLE_DROP_CONSTRAINT = 14

        /**
         * The type of an ALTER TABLE RENAME statement.
         */
        const val ALTER_TABLE_RENAME = 15

        /**
         * The type of an ALTER TABLE ALTER COLUMN RENAME statement.
         */
        const val ALTER_TABLE_ALTER_COLUMN_RENAME = 16

        /**
         * The type of an ALTER USER ADMIN statement.
         */
        const val ALTER_USER_ADMIN = 17

        /**
         * The type of an ALTER USER RENAME statement.
         */
        const val ALTER_USER_RENAME = 18

        /**
         * The type of an ALTER USER SET PASSWORD statement.
         */
        const val ALTER_USER_SET_PASSWORD = 19

        /**
         * The type of an ALTER VIEW statement.
         */
        const val ALTER_VIEW = 20

        /**
         * The type of an ANALYZE statement.
         */
        const val ANALYZE = 21

        /**
         * The type of a CREATE AGGREGATE statement.
         */
        const val CREATE_AGGREGATE = 22

        /**
         * The type of a CREATE CONSTANT statement.
         */
        const val CREATE_CONSTANT = 23

        /**
         * The type of a CREATE ALIAS statement.
         */
        const val CREATE_ALIAS = 24

        /**
         * The type of a CREATE INDEX statement.
         */
        const val CREATE_INDEX = 25

        /**
         * The type of a CREATE LINKED TABLE statement.
         */
        const val CREATE_LINKED_TABLE = 26

        /**
         * The type of a CREATE ROLE statement.
         */
        const val CREATE_ROLE = 27

        /**
         * The type of a CREATE SCHEMA statement.
         */
        const val CREATE_SCHEMA = 28

        /**
         * The type of a CREATE SEQUENCE statement.
         */
        const val CREATE_SEQUENCE = 29

        /**
         * The type of a CREATE TABLE statement.
         */
        const val CREATE_TABLE = 30

        /**
         * The type of a CREATE TRIGGER statement.
         */
        const val CREATE_TRIGGER = 31

        /**
         * The type of a CREATE USER statement.
         */
        const val CREATE_USER = 32

        /**
         * The type of a CREATE DOMAIN statement.
         */
        const val CREATE_DOMAIN = 33

        /**
         * The type of a CREATE VIEW statement.
         */
        const val CREATE_VIEW = 34

        /**
         * The type of a DEALLOCATE statement.
         */
        const val DEALLOCATE = 35

        /**
         * The type of a DROP AGGREGATE statement.
         */
        const val DROP_AGGREGATE = 36

        /**
         * The type of a DROP CONSTANT statement.
         */
        const val DROP_CONSTANT = 37

        /**
         * The type of a DROP ALL OBJECTS statement.
         */
        const val DROP_ALL_OBJECTS = 38

        /**
         * The type of a DROP ALIAS statement.
         */
        const val DROP_ALIAS = 39

        /**
         * The type of a DROP INDEX statement.
         */
        const val DROP_INDEX = 40

        /**
         * The type of a DROP ROLE statement.
         */
        const val DROP_ROLE = 41

        /**
         * The type of a DROP SCHEMA statement.
         */
        const val DROP_SCHEMA = 42

        /**
         * The type of a DROP SEQUENCE statement.
         */
        const val DROP_SEQUENCE = 43

        /**
         * The type of a DROP TABLE statement.
         */
        const val DROP_TABLE = 44

        /**
         * The type of a DROP TRIGGER statement.
         */
        const val DROP_TRIGGER = 45

        /**
         * The type of a DROP USER statement.
         */
        const val DROP_USER = 46

        /**
         * The type of a DROP DOMAIN statement.
         */
        const val DROP_DOMAIN = 47

        /**
         * The type of a DROP VIEW statement.
         */
        const val DROP_VIEW = 48

        /**
         * The type of a GRANT statement.
         */
        const val GRANT = 49

        /**
         * The type of a REVOKE statement.
         */
        const val REVOKE = 50

        /**
         * The type of a PREPARE statement.
         */
        const val PREPARE = 51

        /**
         * The type of a COMMENT statement.
         */
        const val COMMENT = 52

        /**
         * The type of a TRUNCATE TABLE statement.
         */
        const val TRUNCATE_TABLE = 53

        // dml operations

        // dml operations
        /**
         * The type of an ALTER SEQUENCE statement.
         */
        const val ALTER_SEQUENCE = 54

        /**
         * The type of an ALTER TABLE SET REFERENTIAL_INTEGRITY statement.
         */
        const val ALTER_TABLE_SET_REFERENTIAL_INTEGRITY = 55

        /**
         * The type of a BACKUP statement.
         */
        const val BACKUP = 56

        /**
         * The type of a CALL statement.
         */
        const val CALL = 57

        /**
         * The type of a DELETE statement.
         */
        const val DELETE = 58

        /**
         * The type of an EXECUTE statement.
         */
        const val EXECUTE = 59

        /**
         * The type of an EXPLAIN statement.
         */
        const val EXPLAIN = 60

        /**
         * The type of an INSERT statement.
         */
        const val INSERT = 61

        /**
         * The type of a MERGE statement.
         */
        const val MERGE = 62

        /**
         * The type of a REPLACE statement.
         */
        const val REPLACE = 63

        /**
         * The type of a no operation statement.
         */
        const val NO_OPERATION = 63

        /**
         * The type of a RUNSCRIPT statement.
         */
        const val RUNSCRIPT = 64

        /**
         * The type of a SCRIPT statement.
         */
        const val SCRIPT = 65

        /**
         * The type of a SELECT statement.
         */
        const val SELECT = 66

        /**
         * The type of a SET statement.
         */
        const val SET = 67

        /**
         * The type of an UPDATE statement.
         */
        const val UPDATE = 68

        // transaction commands

        // transaction commands
        /**
         * The type of a SET AUTOCOMMIT statement.
         */
        const val SET_AUTOCOMMIT_TRUE = 69

        /**
         * The type of a SET AUTOCOMMIT statement.
         */
        const val SET_AUTOCOMMIT_FALSE = 70

        /**
         * The type of a COMMIT statement.
         */
        const val COMMIT = 71

        /**
         * The type of a ROLLBACK statement.
         */
        const val ROLLBACK = 72

        /**
         * The type of a CHECKPOINT statement.
         */
        const val CHECKPOINT = 73

        /**
         * The type of a SAVEPOINT statement.
         */
        const val SAVEPOINT = 74

        /**
         * The type of a ROLLBACK TO SAVEPOINT statement.
         */
        const val ROLLBACK_TO_SAVEPOINT = 75

        /**
         * The type of a CHECKPOINT SYNC statement.
         */
        const val CHECKPOINT_SYNC = 76

        /**
         * The type of a PREPARE COMMIT statement.
         */
        const val PREPARE_COMMIT = 77

        /**
         * The type of a COMMIT TRANSACTION statement.
         */
        const val COMMIT_TRANSACTION = 78

        /**
         * The type of a ROLLBACK TRANSACTION statement.
         */
        const val ROLLBACK_TRANSACTION = 79

        /**
         * The type of a SHUTDOWN statement.
         */
        const val SHUTDOWN = 80

        /**
         * The type of a SHUTDOWN IMMEDIATELY statement.
         */
        const val SHUTDOWN_IMMEDIATELY = 81

        /**
         * The type of a SHUTDOWN COMPACT statement.
         */
        const val SHUTDOWN_COMPACT = 82

        /**
         * The type of a BEGIN {WORK|TRANSACTION} statement.
         */
        const val BEGIN = 83

        /**
         * The type of a SHUTDOWN DEFRAG statement.
         */
        const val SHUTDOWN_DEFRAG = 84

        /**
         * The type of an ALTER TABLE RENAME CONSTRAINT statement.
         */
        const val ALTER_TABLE_RENAME_CONSTRAINT = 85

        /**
         * The type of an EXPLAIN ANALYZE statement.
         */
        const val EXPLAIN_ANALYZE = 86

        /**
         * The type of an ALTER TABLE ALTER COLUMN SET INVISIBLE statement.
         */
        const val ALTER_TABLE_ALTER_COLUMN_VISIBILITY = 87

        /**
         * The type of a CREATE SYNONYM statement.
         */
        const val CREATE_SYNONYM = 88

        /**
         * The type of a DROP SYNONYM statement.
         */
        const val DROP_SYNONYM = 89

        /**
         * The type of an ALTER TABLE ALTER COLUMN SET ON UPDATE statement.
         */
        const val ALTER_TABLE_ALTER_COLUMN_ON_UPDATE = 90

        /**
         * The type of an EXECUTE IMMEDIATELY statement.
         */
        const val EXECUTE_IMMEDIATELY = 91

        /**
         * The type of ALTER DOMAIN ADD CONSTRAINT statement.
         */
        const val ALTER_DOMAIN_ADD_CONSTRAINT = 92

        /**
         * The type of ALTER DOMAIN DROP CONSTRAINT statement.
         */
        const val ALTER_DOMAIN_DROP_CONSTRAINT = 93

        /**
         * The type of an ALTER DOMAIN SET DEFAULT and ALTER DOMAIN DROP DEFAULT
         * statements.
         */
        const val ALTER_DOMAIN_DEFAULT = 94

        /**
         * The type of an ALTER DOMAIN SET ON UPDATE and ALTER DOMAIN DROP ON UPDATE
         * statements.
         */
        const val ALTER_DOMAIN_ON_UPDATE = 95

        /**
         * The type of an ALTER DOMAIN RENAME statement.
         */
        const val ALTER_DOMAIN_RENAME = 96

        /**
         * The type of a HELP statement.
         */
        const val HELP = 97

        /**
         * The type of an ALTER TABLE ALTER COLUMN DROP EXPRESSION statement.
         */
        const val ALTER_TABLE_ALTER_COLUMN_DROP_EXPRESSION = 98

        /**
         * The type of an ALTER TABLE ALTER COLUMN DROP IDENTITY statement.
         */
        const val ALTER_TABLE_ALTER_COLUMN_DROP_IDENTITY = 99

        /**
         * The type of ALTER TABLE ALTER COLUMN SET DEFAULT ON NULL and ALTER TABLE
         * ALTER COLUMN DROP DEFAULT ON NULL statements.
         */
        const val ALTER_TABLE_ALTER_COLUMN_DEFAULT_ON_NULL = 100

        /**
         * The type of an ALTER DOMAIN RENAME CONSTRAINT statement.
         */
        const val ALTER_DOMAIN_RENAME_CONSTRAINT = 101
    }

    /**
     * Get command type.
     * @return one of the constants above
     */
    fun getCommandType(): Int

    /**
     * Check if this is a query.
     * @return true if it is a query
     */
    fun isQuery(): Boolean

    /**
     * Get the parameters (if any).
     * @return the parameters
     */
    fun getParameters(): ArrayList<out ParameterInterface?>

    /**
     * Execute the query.
     *
     * @param maxRows the maximum number of rows returned
     * @param scrollable if the result set must be scrollable
     * @return the result
     */
    fun executeQuery(maxRows: Long, scrollable: Boolean): ResultInterface?

    /**
     * Execute the statement
     *
     * @param generatedKeysRequest
     * `null` or `false` if generated keys are not
     * needed, `true` if generated keys should be configured
     * automatically, `int[]` to specify column indices to
     * return generated keys from, or `String[]` to specify
     * column names to return generated keys from
     *
     * @return the update count and generated keys, if any
     */
    fun executeUpdate(generatedKeysRequest: Any?): ResultWithGeneratedKeys?

    /**
     * Stop the command execution, release all locks and resources
     */
    fun stop()

    /**
     * Close the statement.
     */
    override fun close()

    /**
     * Cancel the statement if it is still processing.
     */
    fun cancel()

    /**
     * Get an empty result set containing the meta data of the result.
     *
     * @return the empty result
     */
    fun getMetaData(): ResultInterface?
}