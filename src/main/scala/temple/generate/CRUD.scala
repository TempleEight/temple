package temple.generate

abstract class CRUD(val verb: String)

object CRUD {
  case object ReadAll extends CRUD("List")
  case object Create  extends CRUD("Create")
  case object Read    extends CRUD("Read")
  case object Update  extends CRUD("Update")
  case object Delete  extends CRUD("Delete")

  val values = Seq(ReadAll, Create, Read, Update, Delete)
}
