package temple.ast

import temple.ast.Metadata.ServiceMetadata

/** A service block, representing one microservice on its own isolated server */
case class ServiceBlock(
  attributes: Map[String, Attribute],
  metadata: Seq[ServiceMetadata] = Nil,
  structs: Map[String, StructBlock] = Map.empty,
) extends TempleBlock[ServiceMetadata] {

  /**
    * Flatten a service block into a sequence of structs, including the service’s root struct.
    *
    * @param rootName The name of the root struct, typically the name of the service.
    * @return An iterator of pairs of names and struct blocks,
    *         represented as an iterator of pairs of names and attributes
    */
  def structIterator(rootName: String): Iterator[(String, Map[String, Attribute])] =
    Iterator((rootName, attributes)) ++ structs.iterator.map { case (str, block) => (str, block.attributes) }

  override private[temple] def setParent(templefile: Templefile): Unit = {
    super.setParent(templefile)
    structs.valuesIterator.foreach(_.setParent(templefile))
  }
}
