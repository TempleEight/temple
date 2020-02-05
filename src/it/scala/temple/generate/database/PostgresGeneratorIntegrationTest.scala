package temple.generate.database

import org.postgresql.util.PSQLException
import org.scalatest.{BeforeAndAfter, Matchers}
import temple.containers.PostgresSpec

class PostgresGeneratorIntegrationTest extends PostgresSpec with Matchers with BeforeAndAfter {

  implicit val context: PostgresContext = PostgresContext(PreparedType.QuestionMarks)

  // The Postgres container is persisted for every test in this spec: clean up any changes made by each test
  before {
    executeWithoutResults("DROP TABLE IF EXISTS Users;")
  }

  "Users table" should "not exist" in {
    a[PSQLException] should be thrownBy executeWithResults("SELECT * FROM Users;")
  }

  "Users table" should "be created, insert values, and return results" in {
    executeWithoutResults("CREATE TABLE Users (id INT);")
    executeWithoutResults("INSERT INTO Users (id) VALUES (1);")
    executeWithResults("SELECT * FROM Users;") match {
      case Some(result) =>
        result.next()
        result.getInt("id") shouldBe 1
        result.isLast shouldBe true
      case None => fail("Database connection could not be established")
    }
  }

  "Users table" should "be created correctly" in {
    val createStatement = PostgresGenerator.generate(TestData.createStatement)
    executeWithoutResults(createStatement)
    executeWithResults("SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users');") match {
      case Some(result) =>
        result.next()
        result.getBoolean("exists") shouldBe true
      case None => fail("Database connection could not be established")
    }
  }

  "Insert statement" should "be executed correctly" in {
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatement))
    executeWithoutResultsPrepared(PostgresGenerator.generate(TestData.insertStatement), TestData.insertDataA)
    executeWithResults("SELECT * FROM USERS;") match {
      case Some(result) =>
        result.next()
        result.getInt("id") shouldBe 3
        result.getFloat("bankBalance") shouldBe 100.1f
        result.getString("name") shouldBe "John Smith"
        result.getBoolean("isStudent") shouldBe true
        result.getDate("dateOfBirth").toString shouldBe "1998-03-05"
        result.getTime("timeOfDay").toString shouldBe "12:00:00"
        result.getTimestamp("expiry").toString shouldBe "2020-01-01 00:00:00.0"
        result.isLast shouldBe true
      case None => fail("Database connection could not be established")
    }
  }

  "Users table" should "be empty after delete following insert" in {
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatement))
    executeWithoutResultsPrepared(PostgresGenerator.generate(TestData.insertStatement), TestData.insertDataA)
    executeWithoutResults(PostgresGenerator.generate(TestData.deleteStatement))
    executeWithResults("SELECT * FROM USERS;") match {
      case Some(result) => result.isBeforeFirst shouldBe false
      case None         => fail("Database connection could not be established")
    }
  }

  "Users table" should "be dropped successfully" in {
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatement))
    executeWithResults("SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users');") match {
      case Some(result) =>
        result.next()
        result.getBoolean("exists") shouldBe true
      case None => fail("Database connection could not be established")
    }
    executeWithoutResults(PostgresGenerator.generate(TestData.dropStatement))
    executeWithResults("SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users');") match {
      case Some(result) =>
        result.next()
        result.getBoolean("exists") shouldBe false
      case None => fail("Database connection could not be established")
    }
  }

  "Complex select statements" should "be executed correctly" in {
    executeWithoutResults(PostgresGenerator.generate(TestData.createStatement))
    //The query should select both
    executeWithoutResultsPrepared(PostgresGenerator.generate(TestData.insertStatement), TestData.insertDataA)
    executeWithoutResultsPrepared(PostgresGenerator.generate(TestData.insertStatement), TestData.insertDataB)
    executeWithResults(PostgresGenerator.generate(TestData.readStatementComplex)) match {
      case Some(result) =>
        result.next()
        result.getInt("id") shouldBe 3
        result.getFloat("bankBalance") shouldBe 100.1f
        result.getString("name") shouldBe "John Smith"
        result.getBoolean("isStudent") shouldBe true
        result.getDate("dateOfBirth").toString shouldBe "1998-03-05"
        result.getTime("timeOfDay").toString shouldBe "12:00:00"
        result.getTimestamp("expiry").toString shouldBe "2020-01-01 00:00:00.0"
        result.isLast shouldBe false
        result.next()
        result.getInt("id") shouldBe 123456
        result.getFloat("bankBalance") shouldBe 23.42f
        result.getString("name") shouldBe "Jane Doe"
        result.getBoolean("isStudent") shouldBe false
        result.getDate("dateOfBirth").toString shouldBe "1998-03-05"
        result.getTime("timeOfDay").toString shouldBe "12:00:00"
        result.getTimestamp("expiry").toString shouldBe "2019-02-03 02:23:50.0"
        result.isLast shouldBe true
      case None => fail("Database connection could not be established")
    }
  }
}
