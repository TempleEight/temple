package temple.generate.language.service

import temple.generate.language.service.adt._
import temple.generate.FileSystem._

/** ServiceGenerator provides an interface for generating service boilerplate from an ADT */
trait ServiceGenerator {

  /** Given a ServiceRoot ADT, generate the service boilerplate in a specific language */
  def generate(serviceRoot: ServiceRoot): Map[File, FileContent]
}
