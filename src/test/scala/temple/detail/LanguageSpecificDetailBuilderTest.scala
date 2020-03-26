package temple.detail

import org.scalatest.{FlatSpec, Matchers}
import temple.detail.LanguageDetail.GoLanguageDetail

class LanguageSpecificDetailBuilderTest extends FlatSpec with Matchers {

  behavior of "LanguageSpecificDetailBuilder"

  it should "build correct Go details" in {
    val detail =
      LanguageSpecificDetailBuilder.build(LanguageSpecificDetailBuilderTestData.simpleTemplefile, MockQuestionAsker)
    detail shouldBe GoLanguageDetail("github.com/squat/and/dab")
  }
}
