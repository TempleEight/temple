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
    executeWithoutResultsPrepared(PostgresGenerator.generate(TestData.insertStatement), TestData.insertData)
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
}
