package temple.ast

import scala.reflect.ClassTag

trait TempleNode {

  /** Fall back to the default metadata for the project */
  protected def lookupDefaultMetadata[T <: Metadata: ClassTag]: Option[T]

  /**
    * Find a metadata item by type
    * @tparam T The type of metadata to be provided. This must be explicitly given, in square brackets
    * @return an option of the metadata item
    */
  def lookupLocalMetadata[T <: Metadata: ClassTag]: Option[T]

  /**
    * Find a metadata item by type, defaulting to the value defined at the project level
    * @tparam T The type of metadata to be provided. This must be explicitly given, in square brackets
    * @return an option of the metadata item
    */
  final def lookupMetadata[T <: Metadata: ClassTag]: Option[T] =
    lookupLocalMetadata[T] orElse lookupDefaultMetadata[T]
}
