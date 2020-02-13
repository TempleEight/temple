package temple.utils

import org.scalatest.{FlatSpec, Matchers}
import temple.utils.StringUtils.{escapeQuotes, indent, wrap}

class StringUtilsTest extends FlatSpec with Matchers {
  "indent" should "add spaces to an empty string" in {
    indent("") shouldEqual "  "
  }
  "indent" should "add n spaces to an empty string" in {
    indent("", 1) shouldEqual " "
    indent("", 3) shouldEqual "   "
  }
  "indent" should "add spaces to a single line" in {
    indent("abcd") shouldEqual "  abcd"
    indent("efgh", 4) shouldEqual "    efgh"
  }
  "indent" should "add spaces on each line" in {
    indent("abcd\nefg", 1) shouldEqual " abcd\n efg"
    indent("abcd\nefg\n", 1) shouldEqual " abcd\n efg\n "
  }

  "wrap" should "add quotes to an empty string" in {
    wrap("") shouldEqual "\"\""
  }
  "wrap" should "add quotes to a single line string" in {
    wrap("test") shouldEqual "\"test\""
  }
  "wrap" should "add a different char to a single line string" in {
    wrap("test", '_') shouldEqual "_test_"
  }

  "escapeQuotes" should "do nothing to a string without quotes" in {
    escapeQuotes("test") shouldEqual "test"
  }
  "escapeQuotes" should "escape quotes in a string with quotes" in {
    escapeQuotes("""foo " bar""") shouldEqual """foo \" bar"""
  }
}
