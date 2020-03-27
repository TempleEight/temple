package temple.ast

import temple.ast.Templefile.Ports

/** The semantic representation of a Templefile */
case class Templefile(
  projectName: String,
  projectBlock: ProjectBlock = ProjectBlock(),
  targets: Map[String, TargetBlock] = Map(),
  services: Map[String, ServiceBlock] = Map(),
) {
  // Inform every child node of their parent, so that they can access the project information
  for (block <- Iterator(projectBlock) ++ targets.valuesIterator ++ services.valuesIterator) {
    block.setParent(this)
  }

  def servicesWithPorts: Iterable[(String, ServiceBlock, Ports)] =
    services
      .zip(Iterator.from(1024, step = 2))
      .map { case ((name, service), port) => (name, service, Ports(port, port + 1)) }
}

object Templefile {
  case class Ports(service: Int, metrics: Int)
}
