package temple.ast

import temple.ast.Metadata.ProjectMetadata

import scala.reflect.ClassTag

/** Any of the root blocks in the Templefile */
abstract class TempleBlock[+M <: Metadata] extends TempleNode {

  /** The list of metadata items in the block */
  def metadata: Seq[M]

  /** The Templefile that this block is within */
  private var parent: TempleNode = _

  /** Set the parent that this Templefile is within */
  private[temple] def setParent(parent: TempleNode): Unit = this.parent = parent

  /** Fall back to the default metadata for the project */
  final private def lookupDefaultMetadata[T <: ProjectMetadata: ClassTag]: Option[T] = {
    if (parent == null) {
      throw new NullPointerException(
        "Cannot lookup metadata: block not registered as part of a Templefile. " +
        "Use lookupLocalMetadata if this was intentional",
      )
    }
    parent.lookupMetadata[T]
  }

  /**
    * Find a metadata item by type
    * @tparam T The type of metadata to be provided. This must be explicitly given, in square brackets
    * @return an option of the metadata item
    */
  final def lookupLocalMetadata[T <: Metadata: ClassTag]: Option[T] =
    metadata.iterator.collectFirst { case m: T => m }

  /**
    * Find a metadata item by type, defaulting to the value defined at the project level
    * @tparam T The type of metadata to be provided. This must be explicitly given, in square brackets
    * @return an option of the metadata item
    */
  final def lookupMetadata[T <: ProjectMetadata: ClassTag]: Option[T] =
    lookupLocalMetadata[T] orElse lookupDefaultMetadata[T]

  /**
    * Whether a specific metadata value is given.
    *
    * For binary metadata (e.g. enumerable), this is just whether it’s set, else whether an exact value of the metadata
    * is provided locally.
    */
  final override def hasMetadata(m: Metadata): Boolean = metadata contains m
}
