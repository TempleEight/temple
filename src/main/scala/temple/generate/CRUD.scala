package temple.generate

abstract class CRUD

object CRUD {
  case object List   extends CRUD
  case object Create extends CRUD
  case object Read   extends CRUD
  case object Update extends CRUD
  case object Delete extends CRUD

  val values = Seq(List, Create, Read, Update, Delete)
}
