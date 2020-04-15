package temple.generate.server

import temple.DSL.semantics.SemanticParsingException
import temple.utils.StringUtils

trait ServiceName {
  def name: String

  def decapitalizedName: String = StringUtils.decapitalize(name)

  def kebabName: String = StringUtils.kebabCase(name)
}

object ServiceName {
  def apply(name: String): ServiceName = BareName(name)

  private case class BareName(name: String) extends ServiceName {
    if (!name.head.isUpper) throw new SemanticParsingException(s"ServiceRoot name ($name) must be capitalized")

    override def toString: String = s"ServiceName($name)"
  }
}
