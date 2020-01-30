package generate.database
import org.scalatest.{FlatSpec, Matchers}

class PostgresGeneratorTest extends FlatSpec with Matchers {
    "PostgresGenerator" should "generate correct CREATE statements" in {
      PostgresGenerator.generate(TestData.create_Statement) shouldBe TestData.create_String
    }
}
