package temple.generate.kube.ast

/** Input information to generate kubernetes scripts */
case class OrchestrationRoot(services: Seq[Service])

case class Service(name: String, image: String, ports: Seq[Int])
