package temple.generate.service

import temple.generate.Endpoint

case class ServiceRoot(name: String, module: String, comms: Seq[String], endpoints: Set[Endpoint], port: Int)
