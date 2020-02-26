package temple.generate.language.service.adt

sealed trait Endpoint

object Endpoint {
  case object All    extends Endpoint
  case object Create extends Endpoint
  case object Read   extends Endpoint
  case object Update extends Endpoint
  case object Delete extends Endpoint
}
