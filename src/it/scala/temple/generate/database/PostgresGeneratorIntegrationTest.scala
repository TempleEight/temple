package temple.generate.database

import java.sql.{Date, Time, Timestamp}
import java.util.UUID

import org.postgresql.util.PSQLException
import org.scalatest.{BeforeAndAfter, Matchers}
import temple.containers.PostgresSpec
import temple.generate.database.ast.ColType.BlobCol
import temple.generate.database.ast.{Column, ColumnDef, Statement}
import temple.utils.FileUtils

class PostgresGeneratorIntegrationTest extends PostgresSpec with Matchers with BeforeAndAfter {

  implicit val context: PostgresContext = PostgresContext(PreparedType.QuestionMarks)

  // The Postgres container is persisted for every test in this spec: clean up any changes made by each test
  before {
    executeWithoutResults("DROP TABLE IF EXISTS unique_test;")
    executeWithoutResults("DROP TABLE IF EXISTS reference_test;")
    executeWithoutResults("DROP TABLE IF EXISTS check_test;")
    executeWithoutResults("DROP TABLE IF EXISTS temple_user;")
  }

  behavior of "PostgresService"

  it should "not contain a temple_user table" in {
    a[PSQLException] should be thrownBy executeWithResults("SELECT * FROM temple_user;")
  }

  it should "create a temple_user table, insert values, and return these values later" in {
    executeWithoutResults("CREATE TABLE temple_user (id INT);")
    executeWithoutResults("INSERT INTO temple_user (id) VALUES (1);")
    val result =
      executeWithResults("SELECT * FROM temple_user;").getOrElse(fail("Database connection could not be established"))
    result.next()
    result.getInt("id") shouldBe 1
    result.isLast shouldBe true
  }

  it should "create a temple_user table correctly" in {
    val createStatement = PostgresGenerator.generate(TestData.createStatement)
    executeWithoutResults(createStatement)
    val result = executeWithResults(
      "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'temple_user');",
    ).getOrElse(fail("Database connection could not be established"))
    result.next()
    result.getBoolean("exists") shouldBe true
  }

  behavior of "InsertStatements"

  it should "be executed correctly" in {
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatement))
    executePreparedWithoutResults(PostgresGenerator.generate(TestData.insertStatement), TestData.insertDataA)
    val result =
      executeWithResults("SELECT * FROM temple_user;").getOrElse(fail("Database connection could not be established"))
    result.next()
    result.getShort("id") shouldBe 3
    result.getInt("anotherId") shouldBe 4
    result.getLong("yetAnotherId") shouldBe 5
    result.getFloat("bankBalance") shouldBe 100.1f
    result.getDouble("bigBankBalance") shouldBe 1000.2f
    result.getString("name") shouldBe "John Smith"
    result.getString("initials") shouldBe "ABC"
    result.getBoolean("isStudent") shouldBe true
    result.getDate("dateOfBirth") shouldBe Date.valueOf("1998-03-05")
    result.getTime("timeOfDay") shouldBe Time.valueOf("12:00:00")
    result.getTimestamp("expiry") shouldBe Timestamp.valueOf("2020-01-01 00:00:00.0")
    result.getObject("veryUnique") shouldBe UUID.fromString("00000000-1234-5678-9012-000000000000")
    result.isLast shouldBe true
  }

  it should "fail when inserting the same data into a column with unique constraint" in {
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatement))
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatementWithUniqueConstraint))

    noException should be thrownBy executePreparedWithoutResults(
      PostgresGenerator.generate(TestData.insertStatementForUniqueConstraint),
      TestData.insertDataUniqueConstraintA,
    )

    a[PSQLException] should be thrownBy executePreparedWithoutResults(
      PostgresGenerator.generate(TestData.insertStatementForUniqueConstraint),
      TestData.insertDataUniqueConstraintB,
    )
  }

  it should "succeed when inserting data that references a valid row in another table" in {
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatement))
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatementWithReferenceConstraint))

    // Insert user into temple_user table, has ID 3
    executePreparedWithoutResults(
      PostgresGenerator.generate(TestData.insertStatement),
      TestData.insertDataA,
    )

    noException should be thrownBy executePreparedWithoutResults(
      PostgresGenerator.generate(TestData.insertStatementForReferenceConstraint),
      TestData.insertDataReferenceConstraintA,
    )
  }

  it should "fail when inserting data that references a non-existent row in another table" in {
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatement))
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatementWithReferenceConstraint))

    // Insert user into temple_users table, has ID 3
    executePreparedWithoutResults(
      PostgresGenerator.generate(TestData.insertStatement),
      TestData.insertDataA,
    )

    a[PSQLException] should be thrownBy executePreparedWithoutResults(
      PostgresGenerator.generate(TestData.insertStatementForReferenceConstraint),
      TestData.insertDataReferenceConstraintB,
    )
  }

  it should "fail when inserting data that doesn't satisfy the lower bound of a check constraint" in {
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatementWithCheckConstraint))
    a[PSQLException] should be thrownBy executePreparedWithoutResults(
      PostgresGenerator.generate(TestData.insertStatementForCheckConstraint),
      TestData.insertDataCheckConstraintLowerFails,
    )
  }

  it should "fail when inserting data that doesn't satisfy the upper bound of a check constraint" in {
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatementWithCheckConstraint))
    a[PSQLException] should be thrownBy executePreparedWithoutResults(
      PostgresGenerator.generate(TestData.insertStatementForCheckConstraint),
      TestData.insertDataCheckConstraintUpperFails,
    )
  }

  it should "succeed when inserting data that satisfies both check constraints" in {
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatementWithCheckConstraint))
    noException should be thrownBy executePreparedWithoutResults(
      PostgresGenerator.generate(TestData.insertStatementForCheckConstraint),
      TestData.insertDataCheckConstraintPasses,
    )
  }

  // Since Blobs require files, we create a test dynamically
  it should "correctly store blobs" in {
    val fileContents = FileUtils.readBinaryFile("src/it/scala/temple/testfiles/cat.jpeg")

    val createStatement = Statement.Create("temple_user", Seq(ColumnDef("picture", BlobCol)))
    executeWithoutResults(PostgresGenerator.generate(createStatement))

    val insertStatement = Statement.Insert("temple_user", Seq(Column("picture")))
    val insertData      = Seq(PreparedVariable.BlobVariable(fileContents))
    executePreparedWithoutResults(PostgresGenerator.generate(insertStatement), insertData)

    val result =
      executeWithResults("SELECT * FROM temple_user;").getOrElse(fail("Database connection could not be established"))
    result.next()
    val returnedFileContents = result.getBytes("picture")
    returnedFileContents.length shouldBe fileContents.length
    returnedFileContents shouldBe fileContents
  }

  behavior of "DeleteStatements"
  it should "clear the table following an insert" in {
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatement))
    executePreparedWithoutResults(PostgresGenerator.generate(TestData.insertStatement), TestData.insertDataA)
    executeWithoutResults(PostgresGenerator.generate(TestData.deleteStatement))
    val result =
      executeWithResults("SELECT * FROM temple_user;").getOrElse(fail("Database connection could not be established"))
    result.isBeforeFirst shouldBe false
  }

  behavior of "DropStatements"
  it should "remove the table from the database" in {
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatement))
    var result = executeWithResults(
      "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'temple_user');",
    ).getOrElse(fail("Database connection could not be established"))
    result.next()
    result.getBoolean("exists") shouldBe true
    executeWithoutResults(PostgresGenerator.generate(TestData.dropStatement))
    result = executeWithResults(
      "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'temple_users');",
    ).getOrElse(fail("Database connection could not be established"))
    result.next()
    result.getBoolean("exists") shouldBe false
  }

  behavior of "SelectStatements"
  it should "be executed correctly" in {
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatement))
    //The query should select both
    executePreparedWithoutResults(PostgresGenerator.generate(TestData.insertStatement), TestData.insertDataA)
    executePreparedWithoutResults(PostgresGenerator.generate(TestData.insertStatement), TestData.insertDataB)
    val result = executeWithResults(PostgresGenerator.generate(TestData.readStatementComplex))
      .getOrElse(fail("Database connection could not be established"))
    result.next()
    result.getShort("id") shouldBe 3
    result.getInt("anotherId") shouldBe 4
    result.getLong("yetAnotherId") shouldBe 5
    result.getFloat("bankBalance") shouldBe 100.1f
    result.getDouble("bigBankBalance") shouldBe 1000.2f
    result.getString("name") shouldBe "John Smith"
    result.getString("initials") shouldBe "ABC"
    result.getBoolean("isStudent") shouldBe true
    result.getDate("dateOfBirth") shouldBe Date.valueOf("1998-03-05")
    result.getTime("timeOfDay") shouldBe Time.valueOf("12:00:00")
    result.getTimestamp("expiry") shouldBe Timestamp.valueOf("2020-01-01 00:00:00.0")
    result.getObject("veryUnique") shouldBe UUID.fromString("00000000-1234-5678-9012-000000000000")
    result.isLast shouldBe false
    result.next()
    result.getShort("id") shouldBe 12345
    result.getInt("anotherId") shouldBe 123456
    result.getLong("yetAnotherId") shouldBe 1234567
    result.getFloat("bankBalance") shouldBe 23.42f
    result.getDouble("bigBankBalance") shouldBe 3.141592f
    result.getString("name") shouldBe "Jane Doe"
    result.getString("initials") shouldBe "WJVS"
    result.getBoolean("isStudent") shouldBe false
    result.getDate("dateOfBirth") shouldBe Date.valueOf("1998-03-05")
    result.getTime("timeOfDay") shouldBe Time.valueOf("12:00:00")
    result.getTimestamp("expiry") shouldBe Timestamp.valueOf("2019-02-03 02:23:50.0")
    result.getObject("veryUnique") shouldBe UUID.fromString("00000000-1234-5678-9012-000000000001")
    result.isLast shouldBe true
  }

  behavior of "UpdateStatements"
  it should "update all rows in a table" in {
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatement))
    executePreparedWithoutResults(PostgresGenerator.generate(TestData.insertStatement), TestData.insertDataA)
    executePreparedWithoutResults(PostgresGenerator.generate(TestData.insertStatement), TestData.insertDataB)
    executeWithoutResults(PostgresGenerator.generate(TestData.updateStatement))
    val result =
      executeWithResults("SELECT * FROM temple_user;").getOrElse(fail("Database connection could not be established"))
    result.next()
    result.getShort("id") shouldBe 3
    result.getInt("anotherId") shouldBe 4
    result.getLong("yetAnotherId") shouldBe 5
    result.getFloat("bankBalance") shouldBe 123.456f
    result.getDouble("bigBankBalance") shouldBe 1000.2f
    result.getString("name") shouldBe "Will"
    result.getString("initials") shouldBe "ABC"
    result.getBoolean("isStudent") shouldBe true
    result.getDate("dateOfBirth") shouldBe Date.valueOf("1998-03-05")
    result.getTime("timeOfDay") shouldBe Time.valueOf("12:00:00")
    result.getTimestamp("expiry") shouldBe Timestamp.valueOf("2020-01-01 00:00:00.0")
    result.getObject("veryUnique") shouldBe UUID.fromString("00000000-1234-5678-9012-000000000000")
    result.isLast shouldBe false
    result.next()
    result.getShort("id") shouldBe 12345
    result.getInt("anotherId") shouldBe 123456
    result.getLong("yetAnotherId") shouldBe 1234567
    result.getFloat("bankBalance") shouldBe 123.456f
    result.getDouble("bigBankBalance") shouldBe 3.141592f
    result.getString("name") shouldBe "Will"
    result.getString("initials") shouldBe "WJVS"
    result.getBoolean("isStudent") shouldBe false
    result.getDate("dateOfBirth") shouldBe Date.valueOf("1998-03-05")
    result.getTime("timeOfDay") shouldBe Time.valueOf("12:00:00")
    result.getTimestamp("expiry") shouldBe Timestamp.valueOf("2019-02-03 02:23:50.0")
    result.getObject("veryUnique") shouldBe UUID.fromString("00000000-1234-5678-9012-000000000001")
    result.isLast shouldBe true
  }

  it should "update some rows in a table using WHERE" in {
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatement))
    executePreparedWithoutResults(PostgresGenerator.generate(TestData.insertStatement), TestData.insertDataA)
    executePreparedWithoutResults(PostgresGenerator.generate(TestData.insertStatement), TestData.insertDataB)
    executeWithoutResults(PostgresGenerator.generate(TestData.updateStatementWithWhere))
    val result =
      executeWithResults("SELECT * FROM temple_user;").getOrElse(fail("Database connection could not be established"))
    result.next()
    result.getShort("id") shouldBe 3
    result.getInt("anotherId") shouldBe 4
    result.getLong("yetAnotherId") shouldBe 5
    result.getFloat("bankBalance") shouldBe 100.1f
    result.getDouble("bigBankBalance") shouldBe 1000.2f
    result.getString("name") shouldBe "John Smith"
    result.getString("initials") shouldBe "ABC"
    result.getBoolean("isStudent") shouldBe true
    result.getDate("dateOfBirth") shouldBe Date.valueOf("1998-03-05")
    result.getTime("timeOfDay") shouldBe Time.valueOf("12:00:00")
    result.getTimestamp("expiry") shouldBe Timestamp.valueOf("2020-01-01 00:00:00.0")
    result.getObject("veryUnique") shouldBe UUID.fromString("00000000-1234-5678-9012-000000000000")
    result.isLast shouldBe false
    result.next()
    result.getShort("id") shouldBe 12345
    result.getInt("anotherId") shouldBe 123456
    result.getLong("yetAnotherId") shouldBe 1234567
    result.getFloat("bankBalance") shouldBe 123.456f
    result.getDouble("bigBankBalance") shouldBe 3.141592f
    result.getString("name") shouldBe "Will"
    result.getString("initials") shouldBe "WJVS"
    result.getBoolean("isStudent") shouldBe false
    result.getDate("dateOfBirth") shouldBe Date.valueOf("1998-03-05")
    result.getTime("timeOfDay") shouldBe Time.valueOf("12:00:00")
    result.getTimestamp("expiry") shouldBe Timestamp.valueOf("2019-02-03 02:23:50.0")
    result.getObject("veryUnique") shouldBe UUID.fromString("00000000-1234-5678-9012-000000000001")
    result.isLast shouldBe true
  }
}
