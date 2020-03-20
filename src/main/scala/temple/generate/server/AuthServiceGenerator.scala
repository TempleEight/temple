package temple.generate.server

import temple.generate.FileSystem._

/** AuthServiceGenerator provides an interface for generating Auth service boilerplate from an ADT */
trait AuthServiceGenerator {

  /** Given an AuthServiceRoot ADT, generate the Auth service boilerplate in a specific language */
  def generate(authServiceRoot: AuthServiceRoot): Files
}
