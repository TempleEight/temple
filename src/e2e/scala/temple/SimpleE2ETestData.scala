package temple

import temple.utils.FileUtils

object SimpleE2ETestData {

  val createStatement: String =
    """CREATE TABLE temple_user (
      |  username TEXT NOT NULL,
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
      |EXPOSE 1025
      |""".stripMargin

  val configureKong: String =
    """#!/bin/sh
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/ \
      |  --data 'name=temple-user-service' \
      |  --data 'url=http://temple-user:1025/temple-user'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/ \
      |  --data 'name=booking-service' \
      |  --data 'url=http://booking:1027/booking'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/ \
      |  --data 'name=event-service' \
      |  --data 'url=http://event:1029/event'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/temple-user-service/routes \
      |  --data "hosts[]=$KONG_ENTRY" \
      |  --data 'paths[]=/api/temple-user'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/booking-service/routes \
      |  --data "hosts[]=$KONG_ENTRY" \
      |  --data 'paths[]=/api/booking'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/event-service/routes \
      |  --data "hosts[]=$KONG_ENTRY" \
      |  --data 'paths[]=/api/event'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/temple-user-service/plugins \
      |  --data 'name=jwt' \
      |  --data 'config.claims_to_verify=exp'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/booking-service/plugins \
      |  --data 'name=jwt' \
      |  --data 'config.claims_to_verify=exp'
      |
      |curl -X POST \
      |  --url $KONG_ADMIN/services/event-service/plugins \
      |  --data 'name=jwt' \
      |  --data 'config.claims_to_verify=exp'""".stripMargin

  val kubeDeployment: String =
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
      |      - image: simple-temple-test-temple-user
      |        name: temple-user
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

  val kubeService: String =
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
      |    port: 1025
      |    targetPort: 1025
      |  selector:
      |    app: temple-user
      |    kind: service
      |""".stripMargin

  val kubeDbStorage: String =
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

  val kubeDbService: String =
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

  val grafanaDashboard: String = FileUtils.readResources("grafana/simple-temple-user.json").stripLineEnd

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
      |- job_name: temple-user
      |  static_configs:
      |  - targets:
      |    - temple-user:1026
      |- job_name: booking
      |  static_configs:
      |  - targets:
      |    - booking:1028
      |- job_name: event
      |  static_configs:
      |  - targets:
      |    - event:1030
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
