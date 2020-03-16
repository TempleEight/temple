package temple.generate.service

import temple.generate.Crud

case class ServiceRoot(name: String, module: String, comms: Seq[String], operations: Set[Crud], port: Int)
