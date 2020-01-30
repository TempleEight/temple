package generate.database
import org.scalatest.FunSuite

class PostgresGeneratorTest extends FunSuite {

  test("Postgres Generation Simple Create Statement") {
    assert(PostgresGenerator.generate(TestData.create_Statement) === TestData.create_String)
  }
}
