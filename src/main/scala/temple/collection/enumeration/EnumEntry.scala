package temple.collection.enumeration

/**
  * Create an enum with case-insensitive lookup by name and by aliases. This is applied to the class that the entries
  * extend.
  * @param name the primary name of the entry
  * @param aliases the alternative names that this item can also be looked up by.
  */
abstract class EnumEntry protected (val name: String, aliases: Seq[String] = Nil) extends enumeratum.EnumEntry {
  private[enumeration] val allNames: Seq[String] = (name +: aliases).map(_.toLowerCase)
}
