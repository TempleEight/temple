package temple.collection

import org.scalatest.{FlatSpec, Matchers}

class FlagMapViewTest extends FlatSpec with Matchers {

  behavior of "FlagMapView"

  it should "get" in {
    val map = FlagMapView("x" -> 5, "y" -> 4)
    map.flag("x")

    map.get("x") shouldBe Some(5)
    map("x") shouldBe 5

    map.get("y") shouldBe None
    a[NoSuchElementException] shouldBe thrownBy { map("y") }
  }

  it should "toMap" in {
    val map = FlagMapView("x" -> 5, "y" -> 4)
    map.flag("x")

    map.toMap shouldBe Map("x" -> 5)
  }

  it should "unflag" in {
    val map = FlagMapView("x" -> 5, "y" -> 4)
    map.flag("x")

    map.get("x") shouldBe Some(5)
    map.unflag("x")

    map.get("x") shouldBe None
  }

  it should "view" in {
    val map = FlagMapView(Map("x" -> 5, "y" -> 4))
    map.flag("x")

    map.view.size shouldBe 1
  }

  it should "map" in {
    val map = FlagMapView(Map("x" -> 5, "y" -> 4))
    map.flag("x")

    map.toMap shouldBe Map("x" -> 5)
  }

}
