FROM golang:1.13.7-alpine

WORKDIR /temple-user

COPY go.mod ./

RUN ["go", "mod", "download"]

COPY . .

COPY config.json /etc/temple-user-service/

RUN ["go", "build", "-o", "temple-user"]

ENTRYPOINT ["./temple-user"]

EXPOSE 1026
