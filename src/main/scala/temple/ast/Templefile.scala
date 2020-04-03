package temple.ast

import temple.ast.Templefile.Ports
import temple.builder.project.ProjectConfig

import scala.reflect.ClassTag

/** The semantic representation of a Templefile */
case class Templefile(
  projectName: String,
  projectBlock: ProjectBlock = ProjectBlock(),
  targets: Map[String, TargetBlock] = Map(),
  var services: Map[String, GeneratedBlock] = Map(),
) extends TempleNode {
  // Inform every child node of their parent, so that they can access the project information
  for (block <- Iterator(projectBlock) ++ targets.valuesIterator ++ services.valuesIterator) {
    block.setParent(this)
  }

  def providedServices: Map[String, ServiceBlock] = services.collect {
    case (name: String, service: ServiceBlock) => name -> service
  }

  def servicesWithPorts: Iterable[(String, GeneratedBlock, Ports)] =
    services
      .zip(Iterator.from(1025, step = 2)) // 1024 is reserved for auth service
      .map {
        case ((name, service), port) =>
          if (service == AuthServiceBlock) (name, service, Ports(ProjectConfig.authPort, ProjectConfig.authPort + 1))
          else (name, service, Ports(port, port + 1))
      }

  def servicesWithPortsWithoutAuth: Iterable[(String, GeneratedBlock, Ports)] =
    servicesWithPorts.filterNot { case (_, service, _) => service == AuthServiceBlock }

  /**
    * Add an extra service to an exisiting Templefile - useful for adding auth when needed.
    * @param name - the name of the new service
    * @param block - the [[ServiceBlock]] to add
    */
  def addService(name: String, block: GeneratedBlock): Unit = {
    block.setParent(this)
    services += name -> block
  }

  /** Fall back to the default metadata for the project */
  override protected def lookupDefaultMetadata[T <: Metadata: ClassTag]: Option[T] = None

  /**
    * Find a metadata item by type
    *
    * @tparam T The type of metadata to be provided. This must be explicitly given, in square brackets
    * @return an option of the metadata item
    */
  override def lookupLocalMetadata[T <: Metadata: ClassTag]: Option[T] = projectBlock.lookupLocalMetadata[T]
}

object Templefile {
  case class Ports(service: Int, metrics: Int)
}
