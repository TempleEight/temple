package temple.DSL.semantics

import temple.DSL.semantics.Metadata._
import temple.DSL.semantics.Templefile._

/** The semantic representation of a Templefile */
case class Templefile(
  projectName: String,
  projectBlock: ProjectBlock,
  targets: Map[String, TargetBlock],
  services: Map[String, ServiceBlock]
)

object Templefile {
  type ProjectBlock = Seq[ProjectMetadata]
  type TargetBlock  = Seq[TargetMetadata]
}
