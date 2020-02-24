package temple.generate.language.service

import temple.generate.language.service.adt._
import temple.utils.FileUtils.{File, FileContent}

trait ServiceGenerator {
  def generate(serviceRoot: ServiceRoot): Map[File, FileContent]
}
