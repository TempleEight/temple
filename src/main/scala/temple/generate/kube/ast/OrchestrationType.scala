package temple.generate.kube.ast

object OrchestrationType {

  /** Input information to generate kubernetes scripts */
  case class OrchestrationRoot(services: Seq[Service])

  /**
    * Describes one microservice deployment in Kubernetes
    * @param name Service Name
    * @param image The name *including registry* of the docker image for this service
    * @param ports The ports the Pod should expose, including the names of the ports (i.e www -> 80)
    * @param replicas The number of replicas of the pod that should be exposed
    * @param secretName The name of the Kubernetes secret used to fetch images from the registr
    */
  case class Service(name: String, image: String, ports: Seq[(String, Int)], replicas: Int, secretName: String)

}
