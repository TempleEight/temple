FROM golang:1.13.7-alpine

WORKDIR /example

COPY go.mod ./

RUN ["go", "mod", "download"]

COPY . .

COPY config.json /etc/example-service/

RUN ["go", "build", "-o", "example"]

ENTRYPOINT ["./example"]

EXPOSE 1026
