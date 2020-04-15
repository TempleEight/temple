package temple.generate.server

import temple.ast.Metadata.Metrics

trait AbstractAttributesRoot extends ServiceName {
  def idAttribute: IDAttribute
}

object AbstractAttributesRoot {

  trait AbstractServiceRoot extends AbstractAttributesRoot {
    def module: String
    def port: Int
    def metrics: Option[Metrics]
  }
}
