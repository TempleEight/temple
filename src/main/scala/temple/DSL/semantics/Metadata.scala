package temple.DSL.semantics

import temple.collection.enumeration._

/** A piece of metadata modifying a service/project/target block */
sealed trait Metadata

object Metadata {
  sealed trait TargetMetadata  extends Metadata
  sealed trait ServiceMetadata extends Metadata
  sealed trait ProjectMetadata extends Metadata

  sealed abstract class TargetLanguage private (name: String, aliases: String*)
      extends EnumEntry(name, aliases)
      with TargetMetadata

  object TargetLanguage extends Enum[TargetLanguage] {
    val values: IndexedSeq[TargetLanguage] = findValues

    case object Swift      extends TargetLanguage("Swift")
    case object JavaScript extends TargetLanguage("JavaScript", "js")
  }

  sealed abstract class ServiceLanguage private (name: String, aliases: String*)
      extends EnumEntry(name, aliases)
      with ServiceMetadata
      with ProjectMetadata

  object ServiceLanguage extends Enum[ServiceLanguage] {
    val values: IndexedSeq[ServiceLanguage] = findValues

    case object Go    extends ServiceLanguage("Go", "golang")
    case object Scala extends ServiceLanguage("Scala")
  }

  sealed abstract class Provider private (name: String) extends EnumEntry(name) with ProjectMetadata

  object Provider extends Enum[Provider] {
    val values: IndexedSeq[Provider] = findValues
    case object Aws extends Provider("aws")
  }

  sealed abstract class Database private (name: String, aliases: String*)
      extends EnumEntry(name, aliases)
      with ProjectMetadata
      with ServiceMetadata

  object Database extends Enum[Database] {
    override def values: IndexedSeq[Database] = findValues
    case object Postgres extends Database("postgres", "PostgreSQL")
  }

  case class ServiceAuth(login: String)           extends ServiceMetadata
  case class ServiceEnumerate(by: Option[String]) extends ServiceMetadata
  case class TargetAuth(services: Seq[String])    extends TargetMetadata
  case class Uses(services: Seq[String])          extends ServiceMetadata

}
