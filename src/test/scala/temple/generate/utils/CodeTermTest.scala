package temple.generate.utils

import org.scalatest.{FlatSpec, Matchers}
import temple.generate.utils.CodeTerm.CodeWrap

import scala.Option.when

class CodeTermTest extends FlatSpec with Matchers {

  behavior of "CodeTerm"

  it should "generate lists correctly" in {
    CodeWrap.parens.spacedList("x", "y") shouldBe "(\n  x,\n  y\n)"

    CodeWrap.parens.prefix("f")("x", "+", "y") shouldBe "f(x + y)"

    CodeWrap.parens.prefix("f").list("x", "y") shouldBe "f(x, y)"

    CodeWrap.parens.prefix("f").tabbedList("x", when(1 < 2) { "y" }, when(2 < 2) { "z" }) shouldBe "f(\n\tx,\n\ty\n)"
  }

}
