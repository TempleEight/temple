package temple.DSL.semantics

import temple.collection.enumeration._

/** A piece of metadata modifying a service/project/target block */
sealed trait Metadata

object Metadata {
  sealed trait TargetMetadata            extends Metadata
  sealed trait ServiceMetadata           extends Metadata
  sealed trait ProjectMetadata           extends Metadata
  sealed trait ProjectAndServiceMetadata extends ServiceMetadata with ProjectMetadata
  sealed trait GlobalMetadata            extends TargetMetadata with ProjectAndServiceMetadata

  sealed abstract class Language private (name: String, aliases: String*)
      extends EnumEntry(name, aliases)
      with GlobalMetadata

  object Language extends Enum[Language] {
    val values: IndexedSeq[Language] = findValues

    case object Go         extends Language("Go", "golang")
    case object Scala      extends Language("Scala")
    case object Swift      extends Language("Swift")
    case object JavaScript extends Language("JavaScript", "js")
  }

  sealed abstract class Provider private (name: String) extends EnumEntry(name) with ProjectMetadata

  object Provider extends Enum[Provider] {
    val values: IndexedSeq[Provider] = findValues
    case object Aws extends Provider("aws")
  }

  sealed abstract class Database private (name: String, aliases: String*)
      extends EnumEntry(name, aliases)
      with ProjectAndServiceMetadata

  object Database extends Enum[Database] {
    override def values: IndexedSeq[Database] = findValues
    case object Postgres extends Database("postgres", "PostgreSQL")
  }

  case class ServiceAuth(login: String)        extends ServiceMetadata
  case class TargetAuth(services: Seq[String]) extends TargetMetadata
  case class Uses(services: Seq[String])       extends ServiceMetadata

}
