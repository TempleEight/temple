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

  val simpleTemplefileKubeDeployment: String =
    """apiVersion: apps/v1
      |kind: Deployment
      |metadata:
      |  name: temple-user
      |  labels:
      |    app: temple-user
      |    kind: service
      |spec:
      |  replicas: 1
      |  selector:
      |    matchLabels:
      |      app: temple-user
      |      kind: service
      |  template:
      |    metadata:
      |      name: temple-user
      |      labels:
      |        app: temple-user
      |        kind: service
      |    spec:
      |      hostname: temple-user
      |      containers:
      |      - image: sample-project-temple-user
      |        name: temple-user
      |        ports:
      |        - containerPort: 1024
      |      imagePullSecrets:
      |      - name: regcred
      |      restartPolicy: Always
      |""".stripMargin

  val simpleTemplefileKubeDbDeployment: String =
    """apiVersion: apps/v1
      |kind: Deployment
      |metadata:
      |  name: temple-user-db
      |  labels:
      |    app: temple-user
      |    kind: db
      |spec:
      |  replicas: 1
      |  selector:
      |    matchLabels:
      |      app: temple-user
      |      kind: db
      |  strategy:
      |    type: Recreate
      |  template:
      |    metadata:
      |      name: temple-user-db
      |      labels:
      |        app: temple-user
      |        kind: db
      |    spec:
      |      hostname: temple-user-db
      |      containers:
      |      - env:
      |        - name: PGUSER
      |          value: postgres
      |        image: postgres:12.1
      |        name: temple-user-db
      |        volumeMounts:
      |        - mountPath: /var/lib/postgresql/data
      |          name: temple-user-db-claim
      |        - mountPath: /docker-entrypoint-initdb.d/init.sql
      |          subPath: init.sql
      |          name: temple-user-db-init
      |        lifecycle:
      |          postStart:
      |            exec:
      |              command:
      |              - /bin/sh
      |              - -c
      |              - psql -U postgres -f /docker-entrypoint-initdb.d/init.sql && echo done
      |      restartPolicy: Always
      |      volumes:
      |      - name: temple-user-db-init
      |        configMap:
      |          name: temple-user-db-config
      |      - name: temple-user-db-claim
      |        persistentVolumeClaim:
      |          claimName: temple-user-db-claim
      |""".stripMargin

  val simpleTemplefileKubeService: String =
    """apiVersion: v1
      |kind: Service
      |metadata:
      |  name: temple-user
      |  labels:
      |    app: temple-user
      |    kind: service
      |spec:
      |  ports:
      |  - name: api
      |    port: 1024
      |    targetPort: 1024
      |  selector:
      |    app: temple-user
      |    kind: service
      |""".stripMargin

  val simpleTemplefileKubeDbStorage: String =
    """apiVersion: v1
      |kind: PersistentVolume
      |metadata:
      |  name: temple-user-db-volume
      |  labels:
      |    app: temple-user
      |    type: local
      |spec:
      |  storageClassName: manual
      |  capacity:
      |    storage: 1.0Gi
      |  accessModes:
      |  - ReadWriteMany
      |  persistentVolumeReclaimPolicy: Delete
      |  hostPath:
      |    path: /data/temple-user-db
      |---
      |apiVersion: v1
      |kind: PersistentVolumeClaim
      |metadata:
      |  name: temple-user-db-claim
      |  labels:
      |    app: temple-user
      |spec:
      |  accessModes:
      |  - ReadWriteMany
      |  volumeName: temple-user-db-volume
      |  storageClassName: manual
      |  resources:
      |    requests:
      |      storage: 100.0Mi
      |""".stripMargin

  val simpleTemplefileKubeDbService: String =
    """apiVersion: v1
      |kind: Service
      |metadata:
      |  name: temple-user-db
      |  labels:
      |    app: temple-user
      |    kind: db
      |spec:
      |  ports:
      |  - name: db
      |    port: 5432
      |    targetPort: 5432
      |  selector:
      |    app: temple-user
      |    kind: db
      |""".stripMargin

  val simpleTemplefileConfigureKong: String =
    """#!/bin/sh
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/ \
      |  --data 'name=temple-user-service' \
      |  --data 'url=http://temple-user:1024/temple-user'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/temple-user-service/routes \
      |  --data "hosts[]=$KONG_ENTRY" \
      |  --data 'paths[]=/api/temple-user'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/temple-user-service/plugins \
      |  --data 'name=jwt' \
      |  --data 'config.claims_to_verify=exp'""".stripMargin

  val simpleTemplefileGrafanaDashboard: String = FileUtils.readResources("grafana/templeuser.json").init

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

  val complexTemplefileKubeDeployment: String =
    """apiVersion: apps/v1
      |kind: Deployment
      |metadata:
      |  name: complex-user
      |  labels:
      |    app: complex-user
      |    kind: service
      |spec:
      |  replicas: 1
      |  selector:
      |    matchLabels:
      |      app: complex-user
      |      kind: service
      |  template:
      |    metadata:
      |      name: complex-user
      |      labels:
      |        app: complex-user
      |        kind: service
      |    spec:
      |      hostname: complex-user
      |      containers:
      |      - image: sample-complex-project-complex-user
      |        name: complex-user
      |        ports:
      |        - containerPort: 1024
      |      imagePullSecrets:
      |      - name: regcred
      |      restartPolicy: Always
      |""".stripMargin

  val complexTemplefileKubeDbDeployment: String =
    """apiVersion: apps/v1
      |kind: Deployment
      |metadata:
      |  name: complex-user-db
      |  labels:
      |    app: complex-user
      |    kind: db
      |spec:
      |  replicas: 1
      |  selector:
      |    matchLabels:
      |      app: complex-user
      |      kind: db
      |  strategy:
      |    type: Recreate
      |  template:
      |    metadata:
      |      name: complex-user-db
      |      labels:
      |        app: complex-user
      |        kind: db
      |    spec:
      |      hostname: complex-user-db
      |      containers:
      |      - env:
      |        - name: PGUSER
      |          value: postgres
      |        image: postgres:12.1
      |        name: complex-user-db
      |        volumeMounts:
      |        - mountPath: /var/lib/postgresql/data
      |          name: complex-user-db-claim
      |        - mountPath: /docker-entrypoint-initdb.d/init.sql
      |          subPath: init.sql
      |          name: complex-user-db-init
      |        lifecycle:
      |          postStart:
      |            exec:
      |              command:
      |              - /bin/sh
      |              - -c
      |              - psql -U postgres -f /docker-entrypoint-initdb.d/init.sql && echo done
      |      restartPolicy: Always
      |      volumes:
      |      - name: complex-user-db-init
      |        configMap:
      |          name: complex-user-db-config
      |      - name: complex-user-db-claim
      |        persistentVolumeClaim:
      |          claimName: complex-user-db-claim
      |""".stripMargin

  val complexTemplefileKubeService: String =
    """apiVersion: v1
      |kind: Service
      |metadata:
      |  name: complex-user
      |  labels:
      |    app: complex-user
      |    kind: service
      |spec:
      |  ports:
      |  - name: api
      |    port: 1024
      |    targetPort: 1024
      |  selector:
      |    app: complex-user
      |    kind: service
      |""".stripMargin

  val complexTemplefileKubeDbStorage: String =
    """apiVersion: v1
      |kind: PersistentVolume
      |metadata:
      |  name: complex-user-db-volume
      |  labels:
      |    app: complex-user
      |    type: local
      |spec:
      |  storageClassName: manual
      |  capacity:
      |    storage: 1.0Gi
      |  accessModes:
      |  - ReadWriteMany
      |  persistentVolumeReclaimPolicy: Delete
      |  hostPath:
      |    path: /data/complex-user-db
      |---
      |apiVersion: v1
      |kind: PersistentVolumeClaim
      |metadata:
      |  name: complex-user-db-claim
      |  labels:
      |    app: complex-user
      |spec:
      |  accessModes:
      |  - ReadWriteMany
      |  volumeName: complex-user-db-volume
      |  storageClassName: manual
      |  resources:
      |    requests:
      |      storage: 100.0Mi
      |""".stripMargin

  val complexTemplefileKubeDbService: String =
    """apiVersion: v1
      |kind: Service
      |metadata:
      |  name: complex-user-db
      |  labels:
      |    app: complex-user
      |    kind: db
      |spec:
      |  ports:
      |  - name: db
      |    port: 5432
      |    targetPort: 5432
      |  selector:
      |    app: complex-user
      |    kind: db
      |""".stripMargin

  val complexTemplefileConfigureKong: String =
    """#!/bin/sh
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/ \
      |  --data 'name=complex-user-service' \
      |  --data 'url=http://complex-user:1024/complex-user'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/complex-user-service/routes \
      |  --data "hosts[]=$KONG_ENTRY" \
      |  --data 'paths[]=/api/complex-user'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/complex-user-service/plugins \
      |  --data 'name=jwt' \
      |  --data 'config.claims_to_verify=exp'""".stripMargin

  val complexTemplefileGrafanaDashboard: String = FileUtils.readResources("grafana/complexuser.json").init
}
