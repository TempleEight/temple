FROM golang:1.13.7-alpine

WORKDIR /simple-temple-test-group

COPY go.mod ./

RUN ["go", "mod", "download"]

COPY . .

COPY config.json /etc/simple-temple-test-group-service/

RUN ["go", "build", "-o", "simple-temple-test-group"]

ENTRYPOINT ["./simple-temple-test-group"]

EXPOSE 1030
