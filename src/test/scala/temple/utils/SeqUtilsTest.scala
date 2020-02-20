package temple.utils

import org.scalatest.{FlatSpec, Matchers}
import temple.utils.SeqUtils.SeqOptionExtras

class SeqUtilsTest extends FlatSpec with Matchers {

  behavior of "sequence"

  it should "successfully return an empty list" in {
    List().sequence shouldBe Some(List())
  }

  it should "successfully return a list of Some" in {
    List(Some(1)).sequence shouldBe Some(List(1))

    List(Some(1), Some(2)).sequence shouldBe Some(List(1, 2))
  }

  it should "return None for a list of Nones" in {
    List(None).sequence shouldBe None

    List(None, None).sequence shouldBe None
  }

  it should "return None for a mixed list" in {
    List(None, Some(1)).sequence shouldBe None

    List(Some(1), Some(2), None).sequence shouldBe None
  }

}
