package temple.ast

import temple.ast.Templefile.Ports

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

  def servicesWithPorts: Iterable[(String, ServiceBlock, Ports)] =
    services
      .zip(Iterator.from(1025, step = 2)) // 1024 is reserved for auth service
      .map { case ((name, service), port) => (name, service, Ports(port, port + 1)) }

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
