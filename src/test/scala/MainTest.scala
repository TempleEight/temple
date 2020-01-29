import org.scalatest.FunSuite

class MainTest extends FunSuite {
  test("Main.square") {
    assert(Main.square(4) === 16)
  }
}
