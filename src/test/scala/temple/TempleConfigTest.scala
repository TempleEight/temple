package temple

import org.scalatest.{FlatSpec, Matchers}

class TempleConfigTest extends FlatSpec with Matchers {
  "Generate" should "not correctly parse without a filename" in {
    a[RuntimeException] should be thrownBy new TempleConfig(Seq("generate"))
  }

  "Generate" should "correctly parse with a filename" in {
    val config = new TempleConfig(Seq("generate", "foo.temple"))
    config.subcommand shouldBe Some(config.Generate)
    config.Generate.filename.toOption shouldBe Some("foo.temple")
  }

  "Empty arguments" should "have no sub commands" in {
    val config = new TempleConfig(Seq())
    config.subcommand shouldBe None
  }

  "Unknown subcommand" should "throw an exception" in {
    an[IllegalArgumentException] should be thrownBy new TempleConfig(Seq("foobarbaz"))
  }

  "Unknown flag" should "throw an exception" in {
    an[IllegalArgumentException] should be thrownBy new TempleConfig(Seq("-z"))
  }
}
