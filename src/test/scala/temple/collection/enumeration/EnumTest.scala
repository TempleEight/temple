package temple.collection.enumeration

import org.scalatest.{FlatSpec, Matchers}
import temple.utils.MapUtils.FailThrower

class EnumTest extends FlatSpec with Matchers {

  sealed abstract class MyEnum(name: String, aliases: String*) extends EnumEntry(name, aliases)

  object MyEnum extends Enum[MyEnum] {
    override def values: IndexedSeq[MyEnum] = findValues

    case object Case1 extends MyEnum("case1", "alias", "This")
    case object Case2 extends MyEnum("other")
  }

  behavior of "Enums"

  it should "have the correct values" in {
    MyEnum.values shouldBe IndexedSeq(MyEnum.Case1, MyEnum.Case2)
  }

  it should "parse correctly" in {
    MyEnum.parseOption("case1") shouldBe Some(MyEnum.Case1)
    MyEnum.parseOption("other") shouldBe Some(MyEnum.Case2)
    MyEnum.parseOption("x") shouldBe None
  }

  it should "parse aliases correctly" in {
    MyEnum.parseOption("alias") shouldBe Some(MyEnum.Case1)
    MyEnum.parseOption("This") shouldBe Some(MyEnum.Case1)
  }

  it should "parse case-insensitively" in {
    MyEnum.parseOption("Alias") shouldBe Some(MyEnum.Case1)
    MyEnum.parseOption("thiS") shouldBe Some(MyEnum.Case1)
    MyEnum.parseOption("OTHER") shouldBe Some(MyEnum.Case2)
    MyEnum.parseOption("case 1") shouldBe None
  }

  it should "call a fail thrower if nothing is found" in {
    implicit val failThrower: FailThrower = _ => throw new RuntimeException

    MyEnum.parse("alias") shouldBe MyEnum.Case1

    a[RuntimeException] should be thrownBy { MyEnum.parse("notfound") }
  }
}
