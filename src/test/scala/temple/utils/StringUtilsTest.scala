package temple.utils

import org.scalatest.{FlatSpec, Matchers}
import temple.utils.StringUtils.{indent, snakeCase}

class StringUtilsTest extends FlatSpec with Matchers {
  behavior of "indent"

  it should "add spaces to an empty string" in {
    indent("") shouldEqual "  "
  }

  it should "add n spaces to an empty string" in {
    indent("", 1) shouldEqual " "
    indent("", 3) shouldEqual "   "
  }

  it should "add spaces to a single line" in {
    indent("abcd") shouldEqual "  abcd"
    indent("efgh", 4) shouldEqual "    efgh"
  }

  it should "add spaces on each line" in {
    indent("abcd\nefg", 1) shouldEqual " abcd\n efg"
    indent("abcd\nefg\n", 1) shouldEqual " abcd\n efg\n "
  }

  behavior of "snakeCase"

  it should "convert an all upper case string to lower case" in {
    snakeCase("ABCDEFG") shouldBe "abcdefg"
  }

  it should "correctly convert from camel case to snake case" in {
    snakeCase("someRandomCamelCase") shouldBe "some_random_camel_case"
  }

  it should "ignore random capitalised words" in {
    snakeCase("someRANDOMCamelCaseCHARACTERS") shouldBe "some_random_camel_case_characters"
  }

  it should "numbers" in {
    snakeCase("some1917Numbers") shouldBe "some1917_numbers"
  }
}
