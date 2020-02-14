package temple.generate.database

import java.sql.{Date, Time, Timestamp}

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
    executeWithoutResults("DROP TABLE IF EXISTS Users;")
  }

  behavior of "PostgresService"
  it should "not contain a users table" in {
    a[PSQLException] should be thrownBy executeWithResults("SELECT * FROM Users;")
  }

  it should "create a users table, insert values, and return these values later" in {
    executeWithoutResults("CREATE TABLE Users (id INT);")
    executeWithoutResults("INSERT INTO Users (id) VALUES (1);")
    val result =
      executeWithResults("SELECT * FROM Users;").getOrElse(fail("Database connection could not be established"))
    result.next()
    result.getInt("id") shouldBe 1
    result.isLast shouldBe true
  }

  it should "create a users table correctly" in {
    val createStatement = PostgresGenerator.generate(TestData.createStatement)
    executeWithoutResults(createStatement)
    val result = executeWithResults(
      "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users');",
    ).getOrElse(fail("Database connection could not be established"))
    result.next()
    result.getBoolean("exists") shouldBe true
  }

  behavior of "InsertStatements"
  it should "be executed correctly" in {
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatement))
    executeWithoutResultsPrepared(PostgresGenerator.generate(TestData.insertStatement), TestData.insertDataA)
    val result =
      executeWithResults("SELECT * FROM USERS;").getOrElse(fail("Database connection could not be established"))
    result.next()
    result.getInt("id") shouldBe 3
    result.getFloat("bankBalance") shouldBe 100.1f
    result.getString("name") shouldBe "John Smith"
    result.getBoolean("isStudent") shouldBe true
    result.getDate("dateOfBirth") shouldBe Date.valueOf("1998-03-05")
    result.getTime("timeOfDay") shouldBe Time.valueOf("12:00:00")
    result.getTimestamp("expiry") shouldBe Timestamp.valueOf("2020-01-01 00:00:00.0")
    result.isLast shouldBe true
  }

  // Since Blobs require files, we create a test dynamically
  it should "correctly store blobs" in {
    val fileContents = FileUtils.readBinaryFile("src/it/scala/temple/testfiles/cat.jpeg")

    val createStatement = Statement.Create("Users", Seq(ColumnDef("picture", BlobCol)))
    executeWithoutResults(PostgresGenerator.generate(createStatement))

    val insertStatement = Statement.Insert("Users", Seq(Column("picture")))
    val insertData      = Seq(PreparedVariable.BlobVariable(fileContents))
    executeWithoutResultsPrepared(PostgresGenerator.generate(insertStatement), insertData)

    val result =
      executeWithResults("SELECT * FROM USERS;").getOrElse(fail("Database connection could not be established"))
    result.next()
    val returnedFileContents = result.getBytes("picture")
    returnedFileContents.length shouldBe fileContents.length
    returnedFileContents shouldBe fileContents
  }

  behavior of "DeleteStatements"
  it should "clear the table following an insert" in {
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatement))
    executeWithoutResultsPrepared(PostgresGenerator.generate(TestData.insertStatement), TestData.insertDataA)
    executeWithoutResults(PostgresGenerator.generate(TestData.deleteStatement))
    val result =
      executeWithResults("SELECT * FROM USERS;").getOrElse(fail("Database connection could not be established"))
    result.isBeforeFirst shouldBe false
  }

  behavior of "DropStatements"
  it should "remove the table from the database" in {
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatement))
    var result = executeWithResults(
      "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users');",
    ).getOrElse(fail("Database connection could not be established"))
    result.next()
    result.getBoolean("exists") shouldBe true
    executeWithoutResults(PostgresGenerator.generate(TestData.dropStatement))
    result = executeWithResults(
      "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users');",
    ).getOrElse(fail("Database connection could not be established"))
    result.next()
    result.getBoolean("exists") shouldBe false
  }

  behavior of "SelectStatements"
  it should "be executed correctly" in {
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatement))
    //The query should select both
    executeWithoutResultsPrepared(PostgresGenerator.generate(TestData.insertStatement), TestData.insertDataA)
    executeWithoutResultsPrepared(PostgresGenerator.generate(TestData.insertStatement), TestData.insertDataB)
    val result = executeWithResults(PostgresGenerator.generate(TestData.readStatementComplex))
      .getOrElse(fail("Database connection could not be established"))
    result.next()
    result.getInt("id") shouldBe 3
    result.getFloat("bankBalance") shouldBe 100.1f
    result.getString("name") shouldBe "John Smith"
    result.getBoolean("isStudent") shouldBe true
    result.getDate("dateOfBirth") shouldBe Date.valueOf("1998-03-05")
    result.getTime("timeOfDay") shouldBe Time.valueOf("12:00:00")
    result.getTimestamp("expiry") shouldBe Timestamp.valueOf("2020-01-01 00:00:00.0")
    result.isLast shouldBe false
    result.next()
    result.getInt("id") shouldBe 123456
    result.getFloat("bankBalance") shouldBe 23.42f
    result.getString("name") shouldBe "Jane Doe"
    result.getBoolean("isStudent") shouldBe false
    result.getDate("dateOfBirth") shouldBe Date.valueOf("1998-03-05")
    result.getTime("timeOfDay") shouldBe Time.valueOf("12:00:00")
    result.getTimestamp("expiry") shouldBe Timestamp.valueOf("2019-02-03 02:23:50.0")
    result.isLast shouldBe true
  }

  behavior of "UpdateStatements"
  it should "update all rows in a table" in {
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatement))
    executeWithoutResultsPrepared(PostgresGenerator.generate(TestData.insertStatement), TestData.insertDataA)
    executeWithoutResultsPrepared(PostgresGenerator.generate(TestData.insertStatement), TestData.insertDataB)
    executeWithoutResults(PostgresGenerator.generate(TestData.updateStatement))
    val result =
      executeWithResults("SELECT * FROM Users;").getOrElse(fail("Database connection could not be established"))
    result.next()
    result.getInt("id") shouldBe 3
    result.getFloat("bankBalance") shouldBe 123.456f
    result.getString("name") shouldBe "Will"
    result.getBoolean("isStudent") shouldBe true
    result.getDate("dateOfBirth") shouldBe Date.valueOf("1998-03-05")
    result.getTime("timeOfDay") shouldBe Time.valueOf("12:00:00")
    result.getTimestamp("expiry") shouldBe Timestamp.valueOf("2020-01-01 00:00:00.0")
    result.isLast shouldBe false
    result.next()
    result.getInt("id") shouldBe 123456
    result.getFloat("bankBalance") shouldBe 123.456f
    result.getString("name") shouldBe "Will"
    result.getBoolean("isStudent") shouldBe false
    result.getDate("dateOfBirth") shouldBe Date.valueOf("1998-03-05")
    result.getTime("timeOfDay") shouldBe Time.valueOf("12:00:00")
    result.getTimestamp("expiry") shouldBe Timestamp.valueOf("2019-02-03 02:23:50.0")
    result.isLast shouldBe true
  }

  it should "update some rows in a table using WHERE" in {
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatement))
    executeWithoutResultsPrepared(PostgresGenerator.generate(TestData.insertStatement), TestData.insertDataA)
    executeWithoutResultsPrepared(PostgresGenerator.generate(TestData.insertStatement), TestData.insertDataB)
    executeWithoutResults(PostgresGenerator.generate(TestData.updateStatementWithWhere))
    val result =
      executeWithResults("SELECT * FROM Users;").getOrElse(fail("Database connection could not be established"))
    result.next()
    result.getInt("id") shouldBe 3
    result.getFloat("bankBalance") shouldBe 100.1f
    result.getString("name") shouldBe "John Smith"
    result.getBoolean("isStudent") shouldBe true
    result.getDate("dateOfBirth") shouldBe Date.valueOf("1998-03-05")
    result.getTime("timeOfDay") shouldBe Time.valueOf("12:00:00")
    result.getTimestamp("expiry") shouldBe Timestamp.valueOf("2020-01-01 00:00:00.0")
    result.isLast shouldBe false
    result.next()
    result.getInt("id") shouldBe 123456
    result.getFloat("bankBalance") shouldBe 123.456f
    result.getString("name") shouldBe "Will"
    result.getBoolean("isStudent") shouldBe false
    result.getDate("dateOfBirth") shouldBe Date.valueOf("1998-03-05")
    result.getTime("timeOfDay") shouldBe Time.valueOf("12:00:00")
    result.getTimestamp("expiry") shouldBe Timestamp.valueOf("2019-02-03 02:23:50.0")
    result.isLast shouldBe true
  }
}
