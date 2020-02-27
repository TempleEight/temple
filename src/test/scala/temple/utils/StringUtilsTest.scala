package temple.utils

import org.scalatest.{FlatSpec, Matchers}
import temple.utils.StringUtils.{indent, snakeCase, españolQue}

class StringUtilsTest extends FlatSpec with Matchers {

  behavior of "indent"

  it should "not change an empty string" in {
    indent("") shouldEqual ""
  }

  it should "add n spaces to a string" in {
    indent("x", 1) shouldEqual " x"
    indent("x", 3) shouldEqual "   x"
  }

  it should "add spaces to a single line" in {
    indent("abcd") shouldEqual "  abcd"
    indent("efgh", 4) shouldEqual "    efgh"
  }

  it should "add spaces on each line except for blank lines" in {
    indent("abcd\nefg", 1) shouldEqual " abcd\n efg"
    indent("abcd\n\nefg\n", 1) shouldEqual " abcd\n\n efg\n"
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

  it should "handle numbers" in {
    snakeCase("some1917Numbers") shouldBe "some1917_numbers"
  }

  it should "not change snake case strings" in {
    snakeCase("already_has_snake_case") shouldBe "already_has_snake_case"
  }

  it should "deal with mixed naming" in {
    snakeCase("foo_barBaz_boo") shouldBe "foo_bar_baz_boo"
  }

  it should "not remove leading underscores" in {
    snakeCase("_fooBarBaz") shouldBe "_foo_bar_baz"
  }

  it should "correctly convert a capitalised phrase" in {
    snakeCase("CustomerID") shouldBe "customer_id"
  }

  behavior of "españolQue"

  it should "wrap a string as a Spanish question" in {
    españolQue("x") shouldEqual ("¿x?")
  }
}
