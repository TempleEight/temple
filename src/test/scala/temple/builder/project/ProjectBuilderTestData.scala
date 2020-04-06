package temple.builder.project

import temple.ast.AbstractServiceBlock._
import temple.ast.AttributeType._
import temple.ast.Metadata.{Database, ServiceAuth}
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
    services = Map(
      "TempleUser" -> ServiceBlock(simpleServiceAttributes),
    ),
  )

  val simpleTemplefilePostgresProject: Templefile = Templefile(
    "SampleProject",
    ProjectBlock(Seq(Database.Postgres)),
    services = Map(
      "TempleUser" -> ServiceBlock(simpleServiceAttributes),
    ),
  )

  val simpleTemplefilePostgresService: Templefile = Templefile(
    "SampleProject",
    services = Map(
      "TempleUser" -> ServiceBlock(simpleServiceAttributes, metadata = Seq(Database.Postgres)),
    ),
  )

  val simpleTemplefileAPISpec: String =
    """openapi: 3.0.0
      |info:
      |  title: SampleProject
      |  version: 0.0.1
      |paths:
      |  /templeuser:
      |    post:
      |      summary: Register a new templeuser
      |      tags:
      |      - TempleUser
      |      requestBody:
      |        content:
      |          application/json:
      |            schema:
      |              type: object
      |              properties:
      |                intField:
      |                  type: number
      |                  format: int32
      |                doubleField:
      |                  type: number
      |                  format: double
      |                stringField:
      |                  type: string
      |                boolField:
      |                  type: boolean
      |                dateField:
      |                  type: string
      |                  format: date
      |                timeField:
      |                  type: string
      |                  format: time
      |                dateTimeField:
      |                  type: string
      |                  format: date-time
      |                blobField:
      |                  type: string
      |      responses:
      |        '200':
      |          description: TempleUser successfully created
      |          content:
      |            application/json:
      |              schema:
      |                type: object
      |                properties:
      |                  intField:
      |                    type: number
      |                    format: int32
      |                  doubleField:
      |                    type: number
      |                    format: double
      |                  stringField:
      |                    type: string
      |                  boolField:
      |                    type: boolean
      |                  dateField:
      |                    type: string
      |                    format: date
      |                  timeField:
      |                    type: string
      |                    format: time
      |                  dateTimeField:
      |                    type: string
      |                    format: date-time
      |                  blobField:
      |                    type: string
      |        '400':
      |          $ref: '#/components/responses/Error400'
      |        '401':
      |          $ref: '#/components/responses/Error401'
      |        '500':
      |          $ref: '#/components/responses/Error500'
      |  /templeuser/{id}:
      |    parameters:
      |    - in: path
      |      name: id
      |      schema:
      |        type: number
      |        format: int32
      |      required: true
      |      description: ID of the templeuser to perform operations on
      |    get:
      |      summary: Look up a single templeuser
      |      tags:
      |      - TempleUser
      |      responses:
      |        '200':
      |          description: TempleUser details
      |          content:
      |            application/json:
      |              schema:
      |                type: object
      |                properties:
      |                  intField:
      |                    type: number
      |                    format: int32
      |                  doubleField:
      |                    type: number
      |                    format: double
      |                  stringField:
      |                    type: string
      |                  boolField:
      |                    type: boolean
      |                  dateField:
      |                    type: string
      |                    format: date
      |                  timeField:
      |                    type: string
      |                    format: time
      |                  dateTimeField:
      |                    type: string
      |                    format: date-time
      |                  blobField:
      |                    type: string
      |        '400':
      |          $ref: '#/components/responses/Error400'
      |        '401':
      |          $ref: '#/components/responses/Error401'
      |        '404':
      |          $ref: '#/components/responses/Error404'
      |        '500':
      |          $ref: '#/components/responses/Error500'
      |    put:
      |      summary: Update a single templeuser
      |      tags:
      |      - TempleUser
      |      requestBody:
      |        content:
      |          application/json:
      |            schema:
      |              type: object
      |              properties:
      |                intField:
      |                  type: number
      |                  format: int32
      |                doubleField:
      |                  type: number
      |                  format: double
      |                stringField:
      |                  type: string
      |                boolField:
      |                  type: boolean
      |                dateField:
      |                  type: string
      |                  format: date
      |                timeField:
      |                  type: string
      |                  format: time
      |                dateTimeField:
      |                  type: string
      |                  format: date-time
      |                blobField:
      |                  type: string
      |      responses:
      |        '200':
      |          description: TempleUser successfully updated
      |          content:
      |            application/json:
      |              schema:
      |                type: object
      |                properties:
      |                  intField:
      |                    type: number
      |                    format: int32
      |                  doubleField:
      |                    type: number
      |                    format: double
      |                  stringField:
      |                    type: string
      |                  boolField:
      |                    type: boolean
      |                  dateField:
      |                    type: string
      |                    format: date
      |                  timeField:
      |                    type: string
      |                    format: time
      |                  dateTimeField:
      |                    type: string
      |                    format: date-time
      |                  blobField:
      |                    type: string
      |        '400':
      |          $ref: '#/components/responses/Error400'
      |        '401':
      |          $ref: '#/components/responses/Error401'
      |        '404':
      |          $ref: '#/components/responses/Error404'
      |        '500':
      |          $ref: '#/components/responses/Error500'
      |    delete:
      |      summary: Delete a single templeuser
      |      tags:
      |      - TempleUser
      |      responses:
      |        '200':
      |          description: TempleUser successfully deleted
      |          content:
      |            application/json:
      |              schema:
      |                type: object
      |                properties: {}
      |        '400':
      |          $ref: '#/components/responses/Error400'
      |        '401':
      |          $ref: '#/components/responses/Error401'
      |        '404':
      |          $ref: '#/components/responses/Error404'
      |        '500':
      |          $ref: '#/components/responses/Error500'
      |components:
      |  responses:
      |    Error400:
      |      description: Invalid request
      |      content:
      |        application/json:
      |          schema:
      |            type: object
      |            properties:
      |              error:
      |                type: string
      |                example: 'Invalid request parameters: name'
      |    Error401:
      |      description: Valid request but forbidden by server
      |      content:
      |        application/json:
      |          schema:
      |            type: object
      |            properties:
      |              error:
      |                type: string
      |                example: Not authorised to create this object
      |    Error404:
      |      description: ID not found
      |      content:
      |        application/json:
      |          schema:
      |            type: object
      |            properties:
      |              error:
      |                type: string
      |                example: Object not found with ID 1
      |    Error500:
      |      description: The server encountered an error while serving this request
      |      content:
      |        application/json:
      |          schema:
      |            type: object
      |            properties:
      |              error:
      |                type: string
      |                example: 'Unable to reach user service: connection timeout'
      |""".stripMargin

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
      |);
      |""".stripMargin

  val simpleTemplefileUsersDockerfile: String =
    """FROM golang:1.13.7-alpine
      |
      |WORKDIR /temple-user
      |
      |COPY go.mod go.sum ./
      |
      |RUN ["go", "mod", "download"]
      |
      |COPY . .
      |
      |COPY config.json /etc/temple-user-service/
      |
      |RUN ["go", "build", "-o", "temple-user"]
      |
      |ENTRYPOINT ["./temple-user"]
      |
      |EXPOSE 1026
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
      |        - containerPort: 1026
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
      |    port: 1026
      |    targetPort: 1026
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
      |  --data 'url=http://temple-user:1026/temple-user'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/temple-user-service/routes \
      |  --data "hosts[]=$KONG_ENTRY" \
      |  --data 'paths[]=/api/temple-user'
      |""".stripMargin

  val simpleTemplefileGrafanaDashboard: String = FileUtils.readResources("grafana/temple-user.json").stripLineEnd

  val simpleTemplefileGrafanaDashboardConfig: String =
    """apiVersion: 1
      |providers:
      |- name: Prometheus
      |  orgId: 1
      |  folder: ''
      |  type: file
      |  disableDeletion: false
      |  editable: true
      |  allowUiUpdates: true
      |  options:
      |    path: /etc/grafana/provisioning/dashboards
      |""".stripMargin

  val simpleTemplefileTempleUserGoFile: String = FileUtils.readResources("go/simple-user/simple-user.go.snippet")
  val simpleTemplefileHookGoFile: String       = FileUtils.readResources("go/simple-user/hook.go.snippet")
  val simpleTemplefileGoModFile: String        = FileUtils.readResources("go/simple-user/go.mod.snippet")
  val simpleTemplefileDaoFile: String          = FileUtils.readResources("go/simple-user/dao/dao.go.snippet")
  val simpleTemplefileErrorsFile: String       = FileUtils.readResources("go/simple-user/dao/errors.go.snippet")
  val simpleTemplefileUtilFile: String         = FileUtils.readResources("go/simple-user/util/util.go.snippet")
  val simpleTemplefileMetricFile: String       = FileUtils.readResources("go/simple-user/metric/metric.go.snippet")

  val simpleTemplefileGrafanaDatasourceConfig: String =
    """apiVersion: 1
      |datasources:
      |- name: Prometheus
      |  type: prometheus
      |  access: proxy
      |  orgId: 1
      |  url: http://prom:9090
      |  basicAuth: false
      |  isDefault: true
      |  editable: true
      |""".stripMargin

  val simpleTemplefilePrometheusConfig: String =
    """global:
      |  scrape_interval: 15s
      |  evaluation_interval: 15s
      |scrape_configs:
      |- job_name: temple-user
      |  static_configs:
      |  - targets:
      |    - temple-user:1027
      |""".stripMargin

  val complexTemplefile: Templefile = Templefile(
    "SampleComplexProject",
    services = Map(
      "ComplexUser" -> ServiceBlock(
        complexServiceAttributes,
        metadata = Seq(ServiceAuth.Email),
        structs = Map("TempleUser" -> StructBlock(simpleServiceAttributes)),
      ),
    ),
  )

  val complexTemplefileAPISpec: String =
    """openapi: 3.0.0
      |info:
      |  title: SampleComplexProject
      |  version: 0.0.1
      |paths:
      |  /complexuser:
      |    post:
      |      summary: Register a new complexuser
      |      tags:
      |      - ComplexUser
      |      requestBody:
      |        content:
      |          application/json:
      |            schema:
      |              type: object
      |              properties:
      |                smallIntField:
      |                  type: number
      |                  format: int32
      |                  minimum: 10
      |                  maximum: 100
      |                intField:
      |                  type: number
      |                  format: int32
      |                  minimum: 10
      |                  maximum: 100
      |                bigIntField:
      |                  type: number
      |                  format: int64
      |                  minimum: 10
      |                  maximum: 100
      |                floatField:
      |                  type: number
      |                  format: float
      |                  minimum: 0.0
      |                  maximum: 300.0
      |                doubleField:
      |                  type: number
      |                  format: double
      |                  minimum: 0.0
      |                  maximum: 123.0
      |                stringField:
      |                  type: string
      |                  minLength: 1
      |                boundedStringField:
      |                  type: string
      |                  minLength: 0
      |                  maxLength: 5
      |                boolField:
      |                  type: boolean
      |                dateField:
      |                  type: string
      |                  format: date
      |                timeField:
      |                  type: string
      |                  format: time
      |                dateTimeField:
      |                  type: string
      |                  format: date-time
      |                blobField:
      |                  type: string
      |      responses:
      |        '200':
      |          description: ComplexUser successfully created
      |          content:
      |            application/json:
      |              schema:
      |                type: object
      |                properties:
      |                  smallIntField:
      |                    type: number
      |                    format: int32
      |                    minimum: 10
      |                    maximum: 100
      |                  intField:
      |                    type: number
      |                    format: int32
      |                    minimum: 10
      |                    maximum: 100
      |                  bigIntField:
      |                    type: number
      |                    format: int64
      |                    minimum: 10
      |                    maximum: 100
      |                  floatField:
      |                    type: number
      |                    format: float
      |                    minimum: 0.0
      |                    maximum: 300.0
      |                  doubleField:
      |                    type: number
      |                    format: double
      |                    minimum: 0.0
      |                    maximum: 123.0
      |                  stringField:
      |                    type: string
      |                    minLength: 1
      |                  boundedStringField:
      |                    type: string
      |                    minLength: 0
      |                    maxLength: 5
      |                  boolField:
      |                    type: boolean
      |                  dateField:
      |                    type: string
      |                    format: date
      |                  timeField:
      |                    type: string
      |                    format: time
      |                  dateTimeField:
      |                    type: string
      |                    format: date-time
      |                  blobField:
      |                    type: string
      |        '400':
      |          $ref: '#/components/responses/Error400'
      |        '401':
      |          $ref: '#/components/responses/Error401'
      |        '500':
      |          $ref: '#/components/responses/Error500'
      |  /complexuser/{id}:
      |    parameters:
      |    - in: path
      |      name: id
      |      schema:
      |        type: number
      |        format: int32
      |      required: true
      |      description: ID of the complexuser to perform operations on
      |    get:
      |      summary: Look up a single complexuser
      |      tags:
      |      - ComplexUser
      |      responses:
      |        '200':
      |          description: ComplexUser details
      |          content:
      |            application/json:
      |              schema:
      |                type: object
      |                properties:
      |                  smallIntField:
      |                    type: number
      |                    format: int32
      |                    minimum: 10
      |                    maximum: 100
      |                  intField:
      |                    type: number
      |                    format: int32
      |                    minimum: 10
      |                    maximum: 100
      |                  bigIntField:
      |                    type: number
      |                    format: int64
      |                    minimum: 10
      |                    maximum: 100
      |                  floatField:
      |                    type: number
      |                    format: float
      |                    minimum: 0.0
      |                    maximum: 300.0
      |                  doubleField:
      |                    type: number
      |                    format: double
      |                    minimum: 0.0
      |                    maximum: 123.0
      |                  stringField:
      |                    type: string
      |                    minLength: 1
      |                  boundedStringField:
      |                    type: string
      |                    minLength: 0
      |                    maxLength: 5
      |                  boolField:
      |                    type: boolean
      |                  dateField:
      |                    type: string
      |                    format: date
      |                  timeField:
      |                    type: string
      |                    format: time
      |                  dateTimeField:
      |                    type: string
      |                    format: date-time
      |                  blobField:
      |                    type: string
      |        '400':
      |          $ref: '#/components/responses/Error400'
      |        '401':
      |          $ref: '#/components/responses/Error401'
      |        '404':
      |          $ref: '#/components/responses/Error404'
      |        '500':
      |          $ref: '#/components/responses/Error500'
      |    put:
      |      summary: Update a single complexuser
      |      tags:
      |      - ComplexUser
      |      requestBody:
      |        content:
      |          application/json:
      |            schema:
      |              type: object
      |              properties:
      |                smallIntField:
      |                  type: number
      |                  format: int32
      |                  minimum: 10
      |                  maximum: 100
      |                intField:
      |                  type: number
      |                  format: int32
      |                  minimum: 10
      |                  maximum: 100
      |                bigIntField:
      |                  type: number
      |                  format: int64
      |                  minimum: 10
      |                  maximum: 100
      |                floatField:
      |                  type: number
      |                  format: float
      |                  minimum: 0.0
      |                  maximum: 300.0
      |                doubleField:
      |                  type: number
      |                  format: double
      |                  minimum: 0.0
      |                  maximum: 123.0
      |                stringField:
      |                  type: string
      |                  minLength: 1
      |                boundedStringField:
      |                  type: string
      |                  minLength: 0
      |                  maxLength: 5
      |                boolField:
      |                  type: boolean
      |                dateField:
      |                  type: string
      |                  format: date
      |                timeField:
      |                  type: string
      |                  format: time
      |                dateTimeField:
      |                  type: string
      |                  format: date-time
      |                blobField:
      |                  type: string
      |      responses:
      |        '200':
      |          description: ComplexUser successfully updated
      |          content:
      |            application/json:
      |              schema:
      |                type: object
      |                properties:
      |                  smallIntField:
      |                    type: number
      |                    format: int32
      |                    minimum: 10
      |                    maximum: 100
      |                  intField:
      |                    type: number
      |                    format: int32
      |                    minimum: 10
      |                    maximum: 100
      |                  bigIntField:
      |                    type: number
      |                    format: int64
      |                    minimum: 10
      |                    maximum: 100
      |                  floatField:
      |                    type: number
      |                    format: float
      |                    minimum: 0.0
      |                    maximum: 300.0
      |                  doubleField:
      |                    type: number
      |                    format: double
      |                    minimum: 0.0
      |                    maximum: 123.0
      |                  stringField:
      |                    type: string
      |                    minLength: 1
      |                  boundedStringField:
      |                    type: string
      |                    minLength: 0
      |                    maxLength: 5
      |                  boolField:
      |                    type: boolean
      |                  dateField:
      |                    type: string
      |                    format: date
      |                  timeField:
      |                    type: string
      |                    format: time
      |                  dateTimeField:
      |                    type: string
      |                    format: date-time
      |                  blobField:
      |                    type: string
      |        '400':
      |          $ref: '#/components/responses/Error400'
      |        '401':
      |          $ref: '#/components/responses/Error401'
      |        '404':
      |          $ref: '#/components/responses/Error404'
      |        '500':
      |          $ref: '#/components/responses/Error500'
      |    delete:
      |      summary: Delete a single complexuser
      |      tags:
      |      - ComplexUser
      |      responses:
      |        '200':
      |          description: ComplexUser successfully deleted
      |          content:
      |            application/json:
      |              schema:
      |                type: object
      |                properties: {}
      |        '400':
      |          $ref: '#/components/responses/Error400'
      |        '401':
      |          $ref: '#/components/responses/Error401'
      |        '404':
      |          $ref: '#/components/responses/Error404'
      |        '500':
      |          $ref: '#/components/responses/Error500'
      |components:
      |  responses:
      |    Error400:
      |      description: Invalid request
      |      content:
      |        application/json:
      |          schema:
      |            type: object
      |            properties:
      |              error:
      |                type: string
      |                example: 'Invalid request parameters: name'
      |    Error401:
      |      description: Valid request but forbidden by server
      |      content:
      |        application/json:
      |          schema:
      |            type: object
      |            properties:
      |              error:
      |                type: string
      |                example: Not authorised to create this object
      |    Error404:
      |      description: ID not found
      |      content:
      |        application/json:
      |          schema:
      |            type: object
      |            properties:
      |              error:
      |                type: string
      |                example: Object not found with ID 1
      |    Error500:
      |      description: The server encountered an error while serving this request
      |      content:
      |        application/json:
      |          schema:
      |            type: object
      |            properties:
      |              error:
      |                type: string
      |                example: 'Unable to reach user service: connection timeout'
      |""".stripMargin

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
      |WORKDIR /complex-user
      |
      |COPY go.mod go.sum ./
      |
      |RUN ["go", "mod", "download"]
      |
      |COPY . .
      |
      |COPY config.json /etc/complex-user-service/
      |
      |RUN ["go", "build", "-o", "complex-user"]
      |
      |ENTRYPOINT ["./complex-user"]
      |
      |EXPOSE 1026
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
      |        - containerPort: 1026
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
      |    port: 1026
      |    targetPort: 1026
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
      |  --data 'url=http://complex-user:1026/complex-user'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/ \
      |  --data 'name=auth-service' \
      |  --data 'url=http://auth:1024/auth'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/complex-user-service/routes \
      |  --data "hosts[]=$KONG_ENTRY" \
      |  --data 'paths[]=/api/complex-user'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/auth-service/routes \
      |  --data "hosts[]=$KONG_ENTRY" \
      |  --data 'paths[]=/api/auth'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/complex-user-service/plugins \
      |  --data 'name=jwt' \
      |  --data 'config.claims_to_verify=exp'
      |""".stripMargin

  val complexTemplefileGrafanaDashboard: String     = FileUtils.readResources("grafana/complex-user.json").init
  val complexTemplefileAuthGrafanaDashboard: String = FileUtils.readResources("grafana/auth.json").init

  val complexTemplefileGrafanaDashboardConfig: String =
    """apiVersion: 1
      |providers:
      |- name: Prometheus
      |  orgId: 1
      |  folder: ''
      |  type: file
      |  disableDeletion: false
      |  editable: true
      |  allowUiUpdates: true
      |  options:
      |    path: /etc/grafana/provisioning/dashboards
      |""".stripMargin

  val complexTemplefileTempleUserGoFile: String = FileUtils.readResources("go/complex-user/complex-user.go.snippet")
  val complexTemplefileHookGoFile: String       = FileUtils.readResources("go/complex-user/hook.go.snippet")
  val complexTemplefileGoModFile: String        = FileUtils.readResources("go/complex-user/go.mod.snippet")
  val complexTemplefileDaoFile: String          = FileUtils.readResources("go/complex-user/dao/dao.go.snippet")
  val complexTemplefileErrorsFile: String       = FileUtils.readResources("go/complex-user/dao/errors.go.snippet")
  val complexTemplefileUtilFile: String         = FileUtils.readResources("go/complex-user/util/util.go.snippet")
  val complexTemplefileMetricFile: String       = FileUtils.readResources("go/complex-user/metric/metric.go.snippet")

  val complexTemplefileAuthGoFile: String      = FileUtils.readResources("go/auth/auth.go.snippet")
  val complexTemplefileAuthHookGoFile: String  = FileUtils.readResources("go/auth/hook.go.snippet")
  val complexTemplefileAuthGoModFile: String   = FileUtils.readResources("go/auth/go.mod.snippet")
  val complexTemplefileAuthUtilFile: String    = FileUtils.readResources("go/auth/util/util.go.snippet")
  val complexTemplefileAuthDaoFile: String     = FileUtils.readResources("go/auth/dao/dao.go.snippet")
  val complexTemplefileAuthErrorsFile: String  = FileUtils.readResources("go/auth/dao/errors.go.snippet")
  val complexTemplefileAuthHandlerFile: String = FileUtils.readResources("go/auth/comm/handler.go.snippet")
  val complexTemplefileAuthMetricFile: String  = FileUtils.readResources("go/auth/metric/metric.go.snippet")

  val complexTemplefileGrafanaDatasourceConfig: String =
    """apiVersion: 1
      |datasources:
      |- name: Prometheus
      |  type: prometheus
      |  access: proxy
      |  orgId: 1
      |  url: http://prom:9090
      |  basicAuth: false
      |  isDefault: true
      |  editable: true
      |""".stripMargin

  val complexTemplefilePrometheusConfig: String =
    """global:
      |  scrape_interval: 15s
      |  evaluation_interval: 15s
      |scrape_configs:
      |- job_name: complex-user
      |  static_configs:
      |  - targets:
      |    - complex-user:1027
      |- job_name: auth
      |  static_configs:
      |  - targets:
      |    - auth:1025
      |""".stripMargin

  val complexTemplefilePostgresAuthOutput: String =
    """CREATE TABLE auth (
      |  id UUID UNIQUE NOT NULL,
      |  email TEXT UNIQUE NOT NULL,
      |  password TEXT NOT NULL
      |);
      |""".stripMargin

  val complexTemplefileAuthDockerfile: String =
    """FROM golang:1.13.7-alpine
      |
      |WORKDIR /auth
      |
      |COPY go.mod go.sum ./
      |
      |RUN ["go", "mod", "download"]
      |
      |COPY . .
      |
      |COPY config.json /etc/auth-service/
      |
      |RUN ["go", "build", "-o", "auth"]
      |
      |ENTRYPOINT ["./auth"]
      |
      |EXPOSE 1024
      |""".stripMargin

  val complexTemplefileAuthKubeDeployment: String   = FileUtils.readResources("kube/auth/auth-deployment.yaml")
  val complexTemplefileAuthKubeService: String      = FileUtils.readResources("kube/auth/auth-service.yaml")
  val complexTemplefileAuthKubeDbDeployment: String = FileUtils.readResources("kube/auth/auth-db-deployment.yaml")
  val complexTemplefileAuthKubeDbService: String    = FileUtils.readResources("kube/auth/auth-db-service.yaml")
  val complexTemplefileAuthKubeDbStorage: String    = FileUtils.readResources("kube/auth/auth-db-storage.yaml")
}
