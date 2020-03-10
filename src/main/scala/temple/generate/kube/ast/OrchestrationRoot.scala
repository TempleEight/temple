package temple.generate.kube.ast

case class OrchestrationRoot(services: Seq[Service])

case class Service(name: String, image: String, ports: Seq[Int])
