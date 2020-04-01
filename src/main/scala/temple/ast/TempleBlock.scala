package temple.ast

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
  final protected def lookupDefaultMetadata[T <: Metadata: ClassTag]: Option[T] = {
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
}
