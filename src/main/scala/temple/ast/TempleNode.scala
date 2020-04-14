package temple.ast

import temple.ast.Metadata.ProjectMetadata

import scala.reflect.ClassTag

trait TempleNode {

  /**
    * Find a metadata item by type
    * @tparam T The type of metadata to be provided. This must be explicitly given, in square brackets
    * @return an option of the metadata item
    */
  def lookupMetadata[T <: ProjectMetadata: ClassTag]: Option[T]

  def hasMetadata(m: Metadata): Boolean
}
