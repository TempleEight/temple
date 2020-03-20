package temple.generate.kube.ast

object OrchestrationType {

  /** Input information to generate kubernetes scripts */
  case class OrchestrationRoot(services: Seq[Service])

  /**
    * Describes one microservice deployment in Kubernetes
    *
    * @param name               Service Name
    * @param image              The name *including registry* of the docker image for this service
    * @param dbImage            The name of the docker image to use for the service's datastore
    * @param ports              The ports the Pod should expose, including the names of the ports (i.e www -> 80)
    * @param replicas           The number of replicas of the pod that should be exposed
    * @param secretName         The name of the Kubernetes secret used to fetch images from the registry
    * @param appEnvVars         A sequence of key -> value to set as environment variables in the app container
    * @param dbEnvVars          A sequence of key -> value to set as environment variables in the db container
    * @param dbStorage          Object that defines how the service db should store data
    * @param dbLifecycleCommand The command to be ran in the database container on startup e.g to init the db
    * @param usesAuth           Whether this service requires authentication with the API Gateway before requests
    */
  case class Service(
    name: String,
    image: String,
    dbImage: String,
    ports: Seq[(String, Int)],
    replicas: Int,
    secretName: String,
    appEnvVars: Seq[(String, String)],
    dbEnvVars: Seq[(String, String)],
    dbStorage: DbStorage,
    dbLifecycleCommand: String,
    usesAuth: Boolean,
  )

  /**
    * Encapsulates how the db stores data
    * @param dataMount the file system location for the db container to store data at
    * @param initMount Where to mount the db init script (i.e schema) to
    * @param initFile The filename of the db init script
    * @param hostPath Where on the *database host* to store the data
    */
  case class DbStorage(dataMount: String, initMount: String, initFile: String, hostPath: String)

}
