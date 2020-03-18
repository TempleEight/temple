package temple.generate.server.go.auth

import temple.utils.FileUtils

object GoAuthServiceDAOGenerator {

  private[auth] def generateErrors(): String =
    FileUtils.readResources("go/genFiles/auth/dao/errors.go.snippet").stripLineEnd
}
