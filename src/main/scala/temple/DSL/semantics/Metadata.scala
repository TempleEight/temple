package temple.DSL.semantics

/** A piece of metadata modifying a service/project/target block */
sealed trait Metadata

object Metadata {
  sealed trait TargetMetadata            extends Metadata
  sealed trait ServiceMetadata           extends Metadata
  sealed trait ProjectMetadata           extends Metadata
  sealed trait ProjectAndServiceMetadata extends ServiceMetadata with ProjectMetadata
  sealed trait GlobalMetadata            extends TargetMetadata with ProjectAndServiceMetadata

  // TODO: is string right here? Could these three be enums?
  case class Language(name: String)             extends GlobalMetadata
  case class Provider(name: String)             extends ProjectMetadata
  case class Database(name: String)             extends ProjectAndServiceMetadata
  case class ServiceAuth(login: String)         extends ServiceMetadata
  case class TargetAuth(services: List[String]) extends TargetMetadata
  case class Uses(services: List[String])       extends ServiceMetadata
}
