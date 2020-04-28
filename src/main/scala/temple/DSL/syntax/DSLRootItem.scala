package temple.DSL.syntax

import temple.utils.StringUtils.indent

/** An item at the root of the Templefile, e.g. services */
case class DSLRootItem(key: String, tag: String, entries: Seq[Entry]) extends Entry(s"$tag block ($key)") {

  override def toString: String = {
    val contents = indent(entries.mkString("\n"))
    s"$key: $tag {\n$contents\n}"
  }
}
