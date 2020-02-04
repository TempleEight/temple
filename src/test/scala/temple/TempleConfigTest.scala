package temple

import org.scalatest.{FlatSpec, Matchers}
import org.rogach.scallop.ScallopOption

class TempleConfigTest extends FlatSpec with Matchers {
  "Generate" should "not correctly parse without a filename" in {
    a[RuntimeException] should be thrownBy new TempleConfig(List("generate"))
  }

  "Generate" should "correctly parse with a filename" in {
    val config = new TempleConfig(List("generate", "foo.temple"))
    config.subcommand shouldBe Some(config.generate)
    config.generate.filename.toOption shouldBe Some("foo.temple")
  }

  "Empty arguments" should "have no sub commands" in {
    val config = new TempleConfig(List())
    config.subcommand shouldBe None
  }

  "Unknown subcommand" should "throw an exception" in {
    an[IllegalArgumentException] should be thrownBy new TempleConfig(List("foobarbaz"))
  }

  "Unknown flag" should "throw an exception" in {
    an[IllegalArgumentException] should be thrownBy new TempleConfig(List("-z"))
  }
}
