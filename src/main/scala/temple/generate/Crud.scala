package temple.generate

sealed trait Crud

object Crud {
  case object ReadAll extends Crud
  case object Create  extends Crud
  case object Read    extends Crud
  case object Update  extends Crud
  case object Delete  extends Crud

  val values = Seq(ReadAll, Create, Read, Update, Delete)
}
