FROM golang:1.13.7-alpine

WORKDIR /complex-user

COPY go.mod ./

RUN ["go", "mod", "download"]

COPY . .

COPY config.json /etc/complex-user-service/

RUN ["go", "build", "-o", "complex-user"]

ENTRYPOINT ["./complex-user"]

EXPOSE 1026
