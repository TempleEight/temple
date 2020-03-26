package temple.detail

/** Maintains any extra required information the user did not provide in the Templefile */
sealed trait LanguageDetail

object LanguageDetail {

  /**
    * Keeps track of the details needed to generate go
    *
    * @param modulePath The initial module component for generated go
    */
  case class GoLanguageDetail(modulePath: String) extends LanguageDetail

}
