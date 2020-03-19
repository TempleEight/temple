package temple.builder.project

import temple.ast.AttributeType._
import temple.ast.Metadata.Database
import temple.ast._
import temple.generate.FileSystem.{File, FileContent}
import temple.utils.FileUtils


import scala.collection.immutable.ListMap

object ProjectBuilderTestData {

  private val simpleServiceAttributes = ListMap(
    "intField"      -> Attribute(IntType()),
    "doubleField"   -> Attribute(FloatType()),
    "stringField"   -> Attribute(StringType()),
    "boolField"     -> Attribute(BoolType),
    "dateField"     -> Attribute(DateType),
    "timeField"     -> Attribute(TimeType),
    "dateTimeField" -> Attribute(DateTimeType),
    "blobField"     -> Attribute(BlobType()),
  )

  private val complexServiceAttributes = ListMap(
    "smallIntField"      -> Attribute(IntType(max = Some(100), min = Some(10), precision = 2)),
    "intField"           -> Attribute(IntType(max = Some(100), min = Some(10))),
    "bigIntField"        -> Attribute(IntType(max = Some(100), min = Some(10), precision = 8)),
    "floatField"         -> Attribute(FloatType(max = Some(300), min = Some(0), precision = 4)),
    "doubleField"        -> Attribute(FloatType(max = Some(123), min = Some(0))),
    "stringField"        -> Attribute(StringType(max = None, min = Some(1))),
    "boundedStringField" -> Attribute(StringType(max = Some(5), min = Some(0))),
    "boolField"          -> Attribute(BoolType),
    "dateField"          -> Attribute(DateType),
    "timeField"          -> Attribute(TimeType),
    "dateTimeField"      -> Attribute(DateTimeType),
    "blobField"          -> Attribute(BlobType()),
  )

  val simpleTemplefile: Templefile = Templefile(
    "SampleProject",
    ProjectBlock(),
    Map(),
    Map(
      "TempleUser" -> ServiceBlock(simpleServiceAttributes),
    ),
  )

  val simpleTemplefilePostgresProject: Templefile = Templefile(
    "SampleProject",
    ProjectBlock(Seq(Database.Postgres)),
    Map(),
    Map(
      "TempleUser" -> ServiceBlock(simpleServiceAttributes),
    ),
  )

  val simpleTemplefilePostgresService: Templefile = Templefile(
    "SampleProject",
    ProjectBlock(),
    Map(),
    Map(
      "TempleUser" -> ServiceBlock(simpleServiceAttributes, metadata = Seq(Database.Postgres)),
    ),
  )

  val simpleTemplefilePostgresCreateOutput: String =
    """CREATE TABLE temple_user (
      |  intField INT NOT NULL,
      |  doubleField DOUBLE PRECISION NOT NULL,
      |  stringField TEXT NOT NULL,
      |  boolField BOOLEAN NOT NULL,
      |  dateField DATE NOT NULL,
      |  timeField TIME NOT NULL,
      |  dateTimeField TIMESTAMPTZ NOT NULL,
      |  blobField BYTEA NOT NULL
      |);""".stripMargin

  val simpleTemplefileUsersDockerfile: String =
    """FROM golang:1.13.7-alpine
      |
      |WORKDIR /templeuser
      |
      |COPY go.mod go.sum ./
      |
      |RUN ["go", "mod", "download"]
      |
      |COPY . .
      |
      |COPY config.json /etc/templeuser-service
      |
      |RUN ["go", "build", "-o", "templeuser"]
      |
      |ENTRYPOINT ["./templeuser"]
      |
      |EXPOSE 1024
      |""".stripMargin

  val kongFiles: Map[File, FileContent] = Map(
    File("kube/kong", "kong-deployment.yaml")    -> FileUtils.readResources("kong/kong-deployment.yaml"),
    File("kube/kong", "kong-service.yaml")       -> FileUtils.readResources("kong/kong-service.yaml"),
    File("kube/kong", "kong-db-deployment.yaml") -> FileUtils.readResources("kong/kong-db-deployment.yaml"),
    File("kube/kong", "kong-db-service.yaml")    -> FileUtils.readResources("kong/kong-db-service.yaml"),
    File("kube/kong", "kong-migration-job.yaml") -> FileUtils.readResources("kong/kong-migration-job.yaml"),
  )

  val simpleTemplefileKubeScripts: Map[File, FileContent] = Map(
      File("kube/TempleUser", "deployment.yaml") ->
      """apiVersion: apps/v1
        |kind: Deployment
        |metadata:
        |  name: TempleUser
        |  labels:
        |    app: TempleUser
        |    kind: service
        |spec:
        |  replicas: 1
        |  selector:
        |    matchLabels:
        |      app: TempleUser
        |      kind: service
        |  template:
        |    metadata:
        |      name: TempleUser
        |      labels:
        |        app: TempleUser
        |        kind: service
        |    spec:
        |      hostname: TempleUser
        |      containers:
        |      - image: temple-SampleProject-TempleUser
        |        name: TempleUser
        |        ports:
        |        - containerPort: 1024
        |      imagePullSecrets:
        |      - name: regcred
        |      restartPolicy: Always
        |""".stripMargin,
      File("kube/TempleUser", "db-deployment.yaml") ->
      """apiVersion: apps/v1
        |kind: Deployment
        |metadata:
        |  name: TempleUser-db
        |  labels:
        |    app: TempleUser
        |    kind: db
        |spec:
        |  replicas: 1
        |  selector:
        |    matchLabels:
        |      app: TempleUser
        |      kind: db
        |  strategy:
        |    type: Recreate
        |  template:
        |    metadata:
        |      name: TempleUser-db
        |      labels:
        |        app: TempleUser
        |        kind: db
        |    spec:
        |      hostname: TempleUser-db
        |      containers:
        |      - env:
        |        - name: PGUSER
        |          value: postgres
        |        image: postgres:12.1
        |        name: TempleUser-db
        |        volumeMounts:
        |        - mountPath: /var/lib/postgresql/data
        |          name: TempleUser-db-claim
        |        - mountPath: /docker-entrypoint-initdb.d/init.sql
        |          subPath: init.sql
        |          name: TempleUser-db-init
        |        lifecycle:
        |          postStart:
        |            exec:
        |              command:
        |              - /bin/sh
        |              - -c
        |              - psql -U postgres -f /docker-entrypoint-initdb.d/init.sql && echo done
        |      restartPolicy: Always
        |      volumes:
        |      - name: TempleUser-db-init
        |        configMap:
        |          name: TempleUser-db-config
        |      - name: TempleUser-db-claim
        |        persistentVolumeClaim:
        |          claimName: TempleUser-db-claim
        |""".stripMargin,
      File("kube/TempleUser", "service.yaml") ->
      """apiVersion: v1
        |kind: Service
        |metadata:
        |  name: TempleUser
        |  labels:
        |    app: TempleUser
        |    kind: service
        |spec:
        |  ports:
        |  - name: api
        |    port: 1024
        |    targetPort: 1024
        |  selector:
        |    app: TempleUser
        |    kind: service
        |""".stripMargin,
      File("kube/TempleUser", "db-storage.yaml") ->
      """apiVersion: v1
        |kind: PersistentVolume
        |metadata:
        |  name: TempleUser-db-volume
        |  labels:
        |    app: TempleUser
        |    type: local
        |spec:
        |  storageClassName: manual
        |  capacity:
        |    storage: 1.0Gi
        |  accessModes:
        |  - ReadWriteMany
        |  persistentVolumeReclaimPolicy: Delete
        |  hostPath:
        |    path: /data/TempleUser-db
        |---
        |apiVersion: v1
        |kind: PersistentVolumeClaim
        |metadata:
        |  name: TempleUser-db-claim
        |  labels:
        |    app: TempleUser
        |spec:
        |  accessModes:
        |  - ReadWriteMany
        |  volumeName: TempleUser-db-volume
        |  storageClassName: manual
        |  resources:
        |    requests:
        |      storage: 100.0Mi
        |""".stripMargin,
      File("kube/TempleUser", "db-service.yaml") ->
      """apiVersion: v1
        |kind: Service
        |metadata:
        |  name: TempleUser-db
        |  labels:
        |    app: TempleUser
        |    kind: db
        |spec:
        |  ports:
        |  - name: db
        |    port: 5432
        |    targetPort: 5432
        |  selector:
        |    app: TempleUser
        |    kind: db
        |""".stripMargin,
      File("kong", "configure-kong.sh") ->
      """#!/bin/sh
        |
        |curl -X POST \
        |  --url $KONG_ADMIN/services/ \
        |  --data 'name=TempleUser-service' \
        |  --data 'url=http://TempleUser:1024/TempleUser'
        |
        |curl -X POST \
        |  --url $KONG_ADMIN/services/TempleUser-service/routes \
        |  --data "hosts[]=$KONG_ENTRY" \
        |  --data 'paths[]=/api/TempleUser'
        |
        |curl -X POST \
        |  --url $KONG_ADMIN/services/TempleUser-service/plugins \
        |  --data 'name=jwt' \
        |  --data 'config.claims_to_verify=exp'""".stripMargin,
    ) ++ kongFiles

  val complexTemplefile: Templefile = Templefile(
    "SampleComplexProject",
    ProjectBlock(),
    Map(),
    Map(
      "ComplexUser" -> ServiceBlock(
        complexServiceAttributes,
        structs = Map("TempleUser" -> StructBlock(simpleServiceAttributes)),
      ),
    ),
  )

  val complexTemplefilePostgresCreateOutput: String =
    """CREATE TABLE complex_user (
      |  smallIntField SMALLINT CHECK (smallIntField <= 100) CHECK (smallIntField >= 10) NOT NULL,
      |  intField INT CHECK (intField <= 100) CHECK (intField >= 10) NOT NULL,
      |  bigIntField BIGINT CHECK (bigIntField <= 100) CHECK (bigIntField >= 10) NOT NULL,
      |  floatField REAL CHECK (floatField <= 300.0) CHECK (floatField >= 0.0) NOT NULL,
      |  doubleField DOUBLE PRECISION CHECK (doubleField <= 123.0) CHECK (doubleField >= 0.0) NOT NULL,
      |  stringField TEXT CHECK (length(stringField) >= 1) NOT NULL,
      |  boundedStringField VARCHAR(5) CHECK (length(boundedStringField) >= 0) NOT NULL,
      |  boolField BOOLEAN NOT NULL,
      |  dateField DATE NOT NULL,
      |  timeField TIME NOT NULL,
      |  dateTimeField TIMESTAMPTZ NOT NULL,
      |  blobField BYTEA NOT NULL
      |);""".stripMargin + "\n\n" + simpleTemplefilePostgresCreateOutput

  val complexTemplefileUsersDockerfile: String =
    """FROM golang:1.13.7-alpine
      |
      |WORKDIR /complexuser
      |
      |COPY go.mod go.sum ./
      |
      |RUN ["go", "mod", "download"]
      |
      |COPY . .
      |
      |COPY config.json /etc/complexuser-service
      |
      |RUN ["go", "build", "-o", "complexuser"]
      |
      |ENTRYPOINT ["./complexuser"]
      |
      |EXPOSE 1024
      |""".stripMargin

  val complexTemplefileUsersKubeScripts: Map[File, FileContent] = Map(
      File("kube/ComplexUser", "deployment.yaml") ->
      """apiVersion: apps/v1
        |kind: Deployment
        |metadata:
        |  name: ComplexUser
        |  labels:
        |    app: ComplexUser
        |    kind: service
        |spec:
        |  replicas: 1
        |  selector:
        |    matchLabels:
        |      app: ComplexUser
        |      kind: service
        |  template:
        |    metadata:
        |      name: ComplexUser
        |      labels:
        |        app: ComplexUser
        |        kind: service
        |    spec:
        |      hostname: ComplexUser
        |      containers:
        |      - image: temple-SampleComplexProject-ComplexUser
        |        name: ComplexUser
        |        ports:
        |        - containerPort: 1024
        |      imagePullSecrets:
        |      - name: regcred
        |      restartPolicy: Always
        |""".stripMargin,
      File("kube/ComplexUser", "db-deployment.yaml") ->
      """apiVersion: apps/v1
        |kind: Deployment
        |metadata:
        |  name: ComplexUser-db
        |  labels:
        |    app: ComplexUser
        |    kind: db
        |spec:
        |  replicas: 1
        |  selector:
        |    matchLabels:
        |      app: ComplexUser
        |      kind: db
        |  strategy:
        |    type: Recreate
        |  template:
        |    metadata:
        |      name: ComplexUser-db
        |      labels:
        |        app: ComplexUser
        |        kind: db
        |    spec:
        |      hostname: ComplexUser-db
        |      containers:
        |      - env:
        |        - name: PGUSER
        |          value: postgres
        |        image: postgres:12.1
        |        name: ComplexUser-db
        |        volumeMounts:
        |        - mountPath: /var/lib/postgresql/data
        |          name: ComplexUser-db-claim
        |        - mountPath: /docker-entrypoint-initdb.d/init.sql
        |          subPath: init.sql
        |          name: ComplexUser-db-init
        |        lifecycle:
        |          postStart:
        |            exec:
        |              command:
        |              - /bin/sh
        |              - -c
        |              - psql -U postgres -f /docker-entrypoint-initdb.d/init.sql && echo done
        |      restartPolicy: Always
        |      volumes:
        |      - name: ComplexUser-db-init
        |        configMap:
        |          name: ComplexUser-db-config
        |      - name: ComplexUser-db-claim
        |        persistentVolumeClaim:
        |          claimName: ComplexUser-db-claim
        |""".stripMargin,
      File("kube/ComplexUser", "service.yaml") ->
      """apiVersion: v1
        |kind: Service
        |metadata:
        |  name: ComplexUser
        |  labels:
        |    app: ComplexUser
        |    kind: service
        |spec:
        |  ports:
        |  - name: api
        |    port: 1024
        |    targetPort: 1024
        |  selector:
        |    app: ComplexUser
        |    kind: service
        |""".stripMargin,
      File("kube/ComplexUser", "db-storage.yaml") ->
      """apiVersion: v1
        |kind: PersistentVolume
        |metadata:
        |  name: ComplexUser-db-volume
        |  labels:
        |    app: ComplexUser
        |    type: local
        |spec:
        |  storageClassName: manual
        |  capacity:
        |    storage: 1.0Gi
        |  accessModes:
        |  - ReadWriteMany
        |  persistentVolumeReclaimPolicy: Delete
        |  hostPath:
        |    path: /data/ComplexUser-db
        |---
        |apiVersion: v1
        |kind: PersistentVolumeClaim
        |metadata:
        |  name: ComplexUser-db-claim
        |  labels:
        |    app: ComplexUser
        |spec:
        |  accessModes:
        |  - ReadWriteMany
        |  volumeName: ComplexUser-db-volume
        |  storageClassName: manual
        |  resources:
        |    requests:
        |      storage: 100.0Mi
        |""".stripMargin,
      File("kube/ComplexUser", "db-service.yaml") ->
      """apiVersion: v1
        |kind: Service
        |metadata:
        |  name: ComplexUser-db
        |  labels:
        |    app: ComplexUser
        |    kind: db
        |spec:
        |  ports:
        |  - name: db
        |    port: 5432
        |    targetPort: 5432
        |  selector:
        |    app: ComplexUser
        |    kind: db
        |""".stripMargin,
      File("kong", "configure-kong.sh") ->
      """#!/bin/sh
        |
        |curl -X POST \
        |  --url $KONG_ADMIN/services/ \
        |  --data 'name=ComplexUser-service' \
        |  --data 'url=http://ComplexUser:1024/ComplexUser'
        |
        |curl -X POST \
        |  --url $KONG_ADMIN/services/ComplexUser-service/routes \
        |  --data "hosts[]=$KONG_ENTRY" \
        |  --data 'paths[]=/api/ComplexUser'
        |
        |curl -X POST \
        |  --url $KONG_ADMIN/services/ComplexUser-service/plugins \
        |  --data 'name=jwt' \
        |  --data 'config.claims_to_verify=exp'""".stripMargin,
    ) ++ kongFiles
}
