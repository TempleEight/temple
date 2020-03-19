package temple

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
      |  friend INT NOT NULL
      |);""".stripMargin

  val dockerfile: String =
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

  val configureKong: String =
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
      |  --data 'config.claims_to_verify=exp'""".stripMargin

  val kubeDeployment: String =
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
      |      - image: temple-SimpleTempleTest-TempleUser
      |        name: TempleUser
      |        ports:
      |        - containerPort: 1024
      |      imagePullSecrets:
      |      - name: regcred
      |      restartPolicy: Always
      |""".stripMargin

  val kubeDbDeployment: String =
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
      |""".stripMargin

  val kubeService: String =
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
      |""".stripMargin

  val kubeDbStorage: String =
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
      |""".stripMargin

  val kubeDbService: String =
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
      |""".stripMargin
}
