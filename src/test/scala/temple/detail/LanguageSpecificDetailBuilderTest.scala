package temple.detail

import org.scalatest.{FlatSpec, Matchers}
import temple.detail.Detail.GoDetail

class LanguageSpecificDetailBuilderTest extends FlatSpec with Matchers {

  behavior of "LanguageSpecificDetailBuilder"

  it should "build correct Go details" in {
    val detail =
      LanguageSpecificDetailBuilder.build(LanguageSpecificDetailBuilderTestData.simpleTemplefile, MockQuestionAsker)
    detail shouldBe GoDetail("github.com/squat/and/dab")
  }
}
