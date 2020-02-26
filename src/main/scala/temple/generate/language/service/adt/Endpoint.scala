package temple.generate.language.service.adt

sealed trait Endpoint

object Endpoint {

  case object ReadAll extends Endpoint {
    override def toString = "List"
  }

  case object Create extends Endpoint {
    override def toString = "Create"
  }

  case object Read extends Endpoint {
    override def toString = "Read"
  }

  case object Update extends Endpoint {
    override def toString = "Update"
  }

  case object Delete extends Endpoint {
    override def toString = "Delete"
  }

  val values = Seq(ReadAll, Create, Read, Update, Delete)
}
