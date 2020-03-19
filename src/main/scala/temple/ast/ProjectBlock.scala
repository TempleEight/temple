package temple.ast

import temple.ast.Metadata.ProjectMetadata

/** The project information at the root level */
case class ProjectBlock(
  metadata: Seq[ProjectMetadata] = Nil,
) extends TempleBlock[ProjectMetadata]
