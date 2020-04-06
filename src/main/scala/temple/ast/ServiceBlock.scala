package temple.ast

import temple.ast.Metadata.ServiceMetadata

/** A service block, representing one microservice on its own isolated server */
case class ServiceBlock(
  attributes: Map[String, Attribute],
  metadata: Seq[ServiceMetadata] = Nil,
  structs: Map[String, NestedStructBlock] = Map.empty,
) extends StructBlock[ServiceMetadata] {

  /**
    * Flatten a service block into a sequence of structs, including the service’s root struct.
    *
    * @param rootName The name of the root struct, typically the name of the service.
    * @return An iterator of pairs of names and struct blocks,
    *         represented as an iterator of pairs of names and attributes
    */
  def structIterator(rootName: String): Iterator[(String, StructBlock[_])] =
    Iterator(rootName -> this) ++ structs

  /** Set the parent that this Templefile is within */
  override private[temple] def setParent(parent: TempleNode): Unit = {
    super.setParent(parent)
    structs.valuesIterator.foreach(_.setParent(this))
  }
}
