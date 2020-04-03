package temple

import temple.utils.FileUtils

object SimpleE2ETestData {

  val createStatement: String =
    """CREATE TABLE simple_temple_test_user (
      |  simpleTempleTestUser TEXT NOT NULL,
      |  email VARCHAR(40) CHECK (length(email) >= 5) NOT NULL,
      |  firstName TEXT NOT NULL,
      |  lastName TEXT NOT NULL,
      |  createdAt TIMESTAMPTZ NOT NULL,
      |  numberOfDogs INT NOT NULL,
      |  yeets BOOLEAN UNIQUE NOT NULL,
      |  currentBankBalance REAL CHECK (currentBankBalance >= 0.0) NOT NULL,
      |  birthDate DATE NOT NULL,
      |  breakfastTime TIME NOT NULL
      |);
      |
      |CREATE TABLE fred (
      |  field TEXT,
      |  friend UUID NOT NULL,
      |  image BYTEA CHECK (octet_length(image) <= 10000000) NOT NULL
      |);""".stripMargin

  val dockerfile: String =
    """FROM golang:1.13.7-alpine
      |
      |WORKDIR /simple-temple-test-user
      |
      |COPY go.mod go.sum ./
      |
      |RUN ["go", "mod", "download"]
      |
      |COPY . .
      |
      |COPY config.json /etc/simple-temple-test-user-service/
      |
      |RUN ["go", "build", "-o", "simple-temple-test-user"]
      |
      |ENTRYPOINT ["./simple-temple-test-user"]
      |
      |EXPOSE 1025
      |""".stripMargin

  val configureKong: String =
    """#!/bin/sh
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/ \
      |  --data 'name=simple-temple-test-user-service' \
      |  --data 'url=http://simple-temple-test-user:1025/simple-temple-test-user'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/ \
      |  --data 'name=booking-service' \
      |  --data 'url=http://booking:1027/booking'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/ \
      |  --data 'name=simple-temple-test-group-service' \
      |  --data 'url=http://simple-temple-test-group:1029/simple-temple-test-group'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/simple-temple-test-user-service/routes \
      |  --data "hosts[]=$KONG_ENTRY" \
      |  --data 'paths[]=/api/simple-temple-test-user'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/booking-service/routes \
      |  --data "hosts[]=$KONG_ENTRY" \
      |  --data 'paths[]=/api/booking'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/simple-temple-test-group-service/routes \
      |  --data "hosts[]=$KONG_ENTRY" \
      |  --data 'paths[]=/api/simple-temple-test-group'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/simple-temple-test-user-service/plugins \
      |  --data 'name=jwt' \
      |  --data 'config.claims_to_verify=exp'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/booking-service/plugins \
      |  --data 'name=jwt' \
      |  --data 'config.claims_to_verify=exp'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/simple-temple-test-group-service/plugins \
      |  --data 'name=jwt' \
      |  --data 'config.claims_to_verify=exp'""".stripMargin

  val kubeDeployment: String =
    """apiVersion: apps/v1
      |kind: Deployment
      |metadata:
      |  name: simple-temple-test-user
      |  labels:
      |    app: simple-temple-test-user
      |    kind: service
      |spec:
      |  replicas: 1
      |  selector:
      |    matchLabels:
      |      app: simple-temple-test-user
      |      kind: service
      |  template:
      |    metadata:
      |      name: simple-temple-test-user
      |      labels:
      |        app: simple-temple-test-user
      |        kind: service
      |    spec:
      |      hostname: simple-temple-test-user
      |      containers:
      |      - image: simple-temple-test-simple-temple-test-user
      |        name: simple-temple-test-user
      |        ports:
      |        - containerPort: 1025
      |      imagePullSecrets:
      |      - name: regcred
      |      restartPolicy: Always
      |""".stripMargin

  val kubeDbDeployment: String =
    """apiVersion: apps/v1
      |kind: Deployment
      |metadata:
      |  name: simple-temple-test-user-db
      |  labels:
      |    app: simple-temple-test-user
      |    kind: db
      |spec:
      |  replicas: 1
      |  selector:
      |    matchLabels:
      |      app: simple-temple-test-user
      |      kind: db
      |  strategy:
      |    type: Recreate
      |  template:
      |    metadata:
      |      name: simple-temple-test-user-db
      |      labels:
      |        app: simple-temple-test-user
      |        kind: db
      |    spec:
      |      hostname: simple-temple-test-user-db
      |      containers:
      |      - env:
      |        - name: PGUSER
      |          value: postgres
      |        image: postgres:12.1
      |        name: simple-temple-test-user-db
      |        volumeMounts:
      |        - mountPath: /var/lib/postgresql/data
      |          name: simple-temple-test-user-db-claim
      |        - mountPath: /docker-entrypoint-initdb.d/init.sql
      |          subPath: init.sql
      |          name: simple-temple-test-user-db-init
      |        lifecycle:
      |          postStart:
      |            exec:
      |              command:
      |              - /bin/sh
      |              - -c
      |              - psql -U postgres -f /docker-entrypoint-initdb.d/init.sql && echo done
      |      restartPolicy: Always
      |      volumes:
      |      - name: simple-temple-test-user-db-init
      |        configMap:
      |          name: simple-temple-test-user-db-config
      |      - name: simple-temple-test-user-db-claim
      |        persistentVolumeClaim:
      |          claimName: simple-temple-test-user-db-claim
      |""".stripMargin

  val kubeService: String =
    """apiVersion: v1
      |kind: Service
      |metadata:
      |  name: simple-temple-test-user
      |  labels:
      |    app: simple-temple-test-user
      |    kind: service
      |spec:
      |  ports:
      |  - name: api
      |    port: 1025
      |    targetPort: 1025
      |  selector:
      |    app: simple-temple-test-user
      |    kind: service
      |""".stripMargin

  val kubeDbStorage: String =
    """apiVersion: v1
      |kind: PersistentVolume
      |metadata:
      |  name: simple-temple-test-user-db-volume
      |  labels:
      |    app: simple-temple-test-user
      |    type: local
      |spec:
      |  storageClassName: manual
      |  capacity:
      |    storage: 1.0Gi
      |  accessModes:
      |  - ReadWriteMany
      |  persistentVolumeReclaimPolicy: Delete
      |  hostPath:
      |    path: /data/simple-temple-test-user-db
      |---
      |apiVersion: v1
      |kind: PersistentVolumeClaim
      |metadata:
      |  name: simple-temple-test-user-db-claim
      |  labels:
      |    app: simple-temple-test-user
      |spec:
      |  accessModes:
      |  - ReadWriteMany
      |  volumeName: simple-temple-test-user-db-volume
      |  storageClassName: manual
      |  resources:
      |    requests:
      |      storage: 100.0Mi
      |""".stripMargin

  val kubeDbService: String =
    """apiVersion: v1
      |kind: Service
      |metadata:
      |  name: simple-temple-test-user-db
      |  labels:
      |    app: simple-temple-test-user
      |    kind: db
      |spec:
      |  ports:
      |  - name: db
      |    port: 5432
      |    targetPort: 5432
      |  selector:
      |    app: simple-temple-test-user
      |    kind: db
      |""".stripMargin

  val grafanaDashboard: String =
    FileUtils.readResources("grafana/simple-temple-user.json").stripLineEnd

  val grafanaDashboardConfig: String =
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

  val grafanaDatasourceConfig: String =
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

  val prometheusConfig: String =
    """global:
      |  scrape_interval: 15s
      |  evaluation_interval: 15s
      |scrape_configs:
      |- job_name: simple-temple-test-user
      |  static_configs:
      |  - targets:
      |    - simple-temple-test-user:1026
      |- job_name: booking
      |  static_configs:
      |  - targets:
      |    - booking:1028
      |- job_name: simple-temple-test-group
      |  static_configs:
      |  - targets:
      |    - simple-temple-test-group:1030
      |""".stripMargin

  val authGoFile: String      = FileUtils.readResources("go/auth/auth.go.snippet")
  val authGoModFile: String   = FileUtils.readResources("go/auth/go.mod.snippet")
  val authHookFile: String    = FileUtils.readResources("go/auth/hook.go.snippet")
  val authUtilFile: String    = FileUtils.readResources("go/auth/util/util.go.snippet")
  val authDaoFile: String     = FileUtils.readResources("go/auth/dao/dao.go.snippet")
  val authErrorsFile: String  = FileUtils.readResources("go/auth/dao/errors.go.snippet")
  val authHandlerFile: String = FileUtils.readResources("go/auth/comm/handler.go.snippet")
  val authMetricFile: String  = FileUtils.readResources("go/auth/metric/metric.go.snippet")
}
