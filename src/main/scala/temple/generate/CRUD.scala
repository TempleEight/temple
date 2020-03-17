package temple.generate

sealed trait CRUD

object CRUD {
  case object ReadAll extends CRUD
  case object Create  extends CRUD
  case object Read    extends CRUD
  case object Update  extends CRUD
  case object Delete  extends CRUD

  val values = Seq(ReadAll, Create, Read, Update, Delete)
}
