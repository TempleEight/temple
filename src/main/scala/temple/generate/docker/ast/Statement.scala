package temple.generate.docker.ast

sealed trait Statement

object Statement {
  case object Env                                     extends Statement
  case class From(image: String, tag: Option[String]) extends Statement
  case object Run                                     extends Statement
  case object Cmd                                     extends Statement
  case object Label                                   extends Statement
  case object Expose                                  extends Statement
  case object Add                                     extends Statement
  case object Copy                                    extends Statement
  case object EntryPoint                              extends Statement
  case object Volume                                  extends Statement
  case object User                                    extends Statement
  case object WorkDir                                 extends Statement
  case object Arg                                     extends Statement
}
