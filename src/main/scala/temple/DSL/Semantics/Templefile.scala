package temple.DSL.Semantics

import temple.DSL.Semantics.Metadata._
import temple.DSL.Semantics.Templefile._

/** The semantic representation of a Templefile */
case class Templefile(
  projectName: String,
  projectBlock: ProjectBlock,
  targets: Map[String, TargetBlock],
  services: Map[String, ServiceBlock]
)

object Templefile {
  type ProjectBlock = List[ProjectMetadata]
  type TargetBlock  = List[TargetMetadata]
}
