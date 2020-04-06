FROM golang:1.13.7-alpine

WORKDIR /simple-temple-test-user

COPY go.mod go.sum ./

RUN ["go", "mod", "download"]

COPY . .

COPY config.json /etc/simple-temple-test-user-service/

RUN ["go", "build", "-o", "simple-temple-test-user"]

ENTRYPOINT ["./simple-temple-test-user"]

EXPOSE 1026
