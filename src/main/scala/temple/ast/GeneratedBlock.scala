package temple.ast

import temple.ast.Metadata.ServiceMetadata

trait GeneratedBlock extends TempleBlock[ServiceMetadata] {
  def attributes: Map[String, Attribute]
  def metadata: Seq[ServiceMetadata]
  def structs: Map[String, StructBlock]

  /**
    * Flatten a service block into a sequence of structs, including the service's root struct.
    *
    * @param rootName The name of the root struct, typically the name of the service.
    * @return An iterator of pairs of names and struct blocks,
    *         represented as an iterator of pairs of names and attributes
    */
  def structIterator(rootName: String): Iterator[(String, Map[String, Attribute])] =
    Iterator((rootName, attributes)) ++ structs.iterator.map { case (str, block) => (str, block.attributes) }

  /** Set the parent that this Templefile is within */
  override private[temple] def setParent(parent: TempleNode): Unit = {
    super.setParent(parent)
    structs.valuesIterator.foreach(_.setParent(this))
  }
}
