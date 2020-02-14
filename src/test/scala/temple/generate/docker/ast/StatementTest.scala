package temple.generate.docker.ast

import org.scalatest.{FlatSpec, Matchers}
import temple.generate.docker.ast.Statement.Expose

class StatementTest extends FlatSpec with Matchers {

  behavior of "Statement"

  it should "Throw on negative port numbers" in {
    a[IllegalArgumentException] should be thrownBy Expose(-1)
  }

  it should "Throw on large port numbers" in {
    a[IllegalArgumentException] should be thrownBy Expose(1234567)
  }

  it should "Allow valid port numbers" in {
    noException should be thrownBy Expose(1234)
  }

}
