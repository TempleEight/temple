package temple

import org.scalatest.{FlatSpec, Matchers}

class TempleConfigTest extends FlatSpec with Matchers {
  behavior of "TempleConfig"

  it should "not parse the generate command correctly without a filename" in {
    a[RuntimeException] should be thrownBy new TempleConfig(Seq("generate"))
  }

  it should "parse the generate command correctly with a filename" in {
    val config = new TempleConfig(Seq("generate", "foo.temple"))
    config.subcommand shouldBe Some(config.Generate)
    config.Generate.filename.toOption shouldBe Some("foo.temple")
  }

  it should "have no sub commands if none were provided" in {
    val config = new TempleConfig(Seq())
    config.subcommand shouldBe None
  }

  it should "throw an exception if an unknown subcommand is used" in {
    an[IllegalArgumentException] should be thrownBy new TempleConfig(Seq("foobarbaz"))
  }

  it should "throw an exception if an unknown flag is used" in {
    an[IllegalArgumentException] should be thrownBy new TempleConfig(Seq("-z"))
  }
}
