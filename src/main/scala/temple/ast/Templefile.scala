package temple.ast

import temple.ast.AbstractServiceBlock._

import temple.ast.Metadata.{Metrics, ServiceAuth}

import temple.ast.Templefile.Ports
import temple.builder.project.ProjectConfig

import scala.collection.immutable.ListMap
import scala.reflect.ClassTag

/** The semantic representation of a Templefile */
case class Templefile(
  projectName: String,
  projectBlock: ProjectBlock = ProjectBlock(),
  targets: Map[String, TargetBlock] = Map(),
  services: Map[String, ServiceBlock] = Map(),
) extends TempleNode {
  // Inform every child node of their parent, so that they can access the project information
  for (block <- Iterator(projectBlock) ++ targets.valuesIterator ++ services.valuesIterator) {
    block.setParent(this)
  }

  val providedServices: Map[String, ServiceBlock] = services

  // Whether or not to generate an auth service - based on whether any service has #auth
  val usesAuth: Boolean = lookupMetadata[Metadata.AuthMethod].isDefined

  // Whether or not to generate metrics
  val usesMetrics: Boolean = lookupMetadata[Metrics].nonEmpty

  val providedServicesWithPorts: Iterable[(String, ServiceBlock, Ports)] =
    providedServices
      .zip(Iterator.from(ProjectConfig.serviceStartPort, step = 2))
      .map { case ((name, service), port) => (name, service, Templefile.Ports(port, port + 1)) }

  val allServicesWithPorts: Iterable[(String, AbstractServiceBlock, Ports)] = {
    if (usesAuth) {
      val authBlock = AuthServiceBlock
      authBlock.setParent(this)
      providedServicesWithPorts ++ Iterable(
        ("Auth", authBlock, Ports(ProjectConfig.authPort, ProjectConfig.authMetricPort)),
      )
    } else providedServicesWithPorts
  }

  val allServices: Map[String, AbstractServiceBlock] = allServicesWithPorts
    .map { case (name, service, _) => (name, service) }
    .to(ListMap)

  /**
    * Find a metadata item by type
    *
    * @tparam T The type of metadata to be provided. This must be explicitly given, in square brackets
    * @return an option of the metadata item
    */
  override def lookupMetadata[T <: Metadata: ClassTag]: Option[T] = projectBlock.lookupLocalMetadata[T]

  /** A mapping from struct name to the service it is contained in */
  lazy val structLocations: Map[String, String] = services.flatMap {
    case (serviceName, block) => block.structs.keys.map(structName => structName -> serviceName)
  }
  def structNames: Iterable[String]        = structLocations.keys
  lazy val providedBlockNames: Set[String] = (services.keys ++ structNames).toSet

  /** Get a block by name, either as a service or a block */
  def getBlock(name: String): AttributeBlock[_] =
    services.getOrElse(name, services(structLocations(name)).structs(name))
}

object Templefile {
  case class Ports(service: Int, metrics: Int)
}
