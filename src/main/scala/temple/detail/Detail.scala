package temple.detail

/** Maintains any extra required information the user did not provide in the Templefile */
sealed trait Detail

object Detail {

  /**
    * Keeps track of the details needed to generate go
    *
    * @param modulePath The initial module component for generated go
    */
  case class GoDetail(modulePath: String) extends Detail

}
