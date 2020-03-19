package temple.ast

import temple.ast.Metadata.TargetMetadata

/** A block describing one client to generate code for */
case class TargetBlock(
  metadata: Seq[TargetMetadata] = Nil,
) extends TempleBlock[TargetMetadata]
