package temple.DSL.semantics

import scala.reflect.ClassTag

/** Any of the root blocks in the Templefile */
abstract class TempleBlock[M <: Metadata] {

  /** The list of metadata items in the block */
  def metadata: Seq[M]

  /** The Templefile that this block is within */
  protected var parent: Templefile = _

  /** Set the parent that this Templefile is within */
  final def setParent(templefile: Templefile): Unit = parent = templefile

  /** Fall back to the default metadata for the project */
  final protected def lookupDefaultMetadata[T: ClassTag]: Option[T] = {
    if (parent == null)
      throw new NullPointerException(
        "Cannot lookup metadata: block not registered as part of a Templefile. " +
        "Use lookupLocalMetadata if this was intentional",
      )
    Option.when(parent.projectBlock != this)(parent) flatMap { _.projectBlock.lookupMetadata[T] }
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
  def lookupMetadata[T <: Metadata: ClassTag]: Option[T] =
    lookupLocalMetadata[T] orElse lookupDefaultMetadata[T]
}
