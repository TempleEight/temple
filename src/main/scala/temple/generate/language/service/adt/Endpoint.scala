package temple.generate.language.service.adt

sealed abstract class Endpoint(val verb: String)

object Endpoint {
  case object ReadAll extends Endpoint("List")
  case object Create  extends Endpoint("Create")
  case object Read    extends Endpoint("Read")
  case object Update  extends Endpoint("Update")
  case object Delete  extends Endpoint("Delete")

  val values = Seq(ReadAll, Create, Read, Update, Delete)
}
