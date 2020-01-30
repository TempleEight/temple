package generate.database
import org.scalatest.FunSuite

class PostgresGeneratorTest extends FunSuite {

  test("Postgres Generation Simple Create Statement") {
    assert(PostgresGenerator.generate(Assets.create_Statement) === Assets.create_String)
  }
}
