package temple.DSL.semantics

import temple.DSL.semantics.Metadata.ProjectMetadata

/** The project information at the root level */
case class ProjectBlock(metadata: Seq[ProjectMetadata]) extends TempleBlock[ProjectMetadata]
