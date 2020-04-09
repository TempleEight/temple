package temple.generate.server

import temple.DSL.semantics.SemanticParsingException
import temple.utils.StringUtils

class ServiceName(val name: String) {
  if (!name.head.isUpper) throw new SemanticParsingException(s"ServiceRoot name ($name) must be capitalized")
  def decapitalizedName: String = StringUtils.decapitalize(name)
  def kebabName: String         = StringUtils.kebabCase(name)
}

object ServiceName {
  def apply(name: String): ServiceName = new ServiceName(name)
}
