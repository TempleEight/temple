package temple.generate.language.service.adt

case class ServiceRoot(name: String, module: String, comms: Seq[String], endpoints: Set[Endpoint], port: Int)
