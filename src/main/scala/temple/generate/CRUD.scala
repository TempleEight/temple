package temple.generate

object CRUD extends Enumeration {
  type CRUD = Value
  val List, Create, Read, Update, Delete, Identity = Value

  def presentParticiple(crud: CRUD): String = crud match {
    case List     => "listing"
    case Create   => "creating"
    case Read     => "reading"
    case Update   => "updating"
    case Delete   => "deleting"
    case Identity => "identifying"
  }
}
