FROM golang:1.13.7-alpine

WORKDIR /auth

COPY go.mod go.sum ./

RUN ["go", "mod", "download"]

COPY . .

COPY config.json /etc/auth-service/

RUN ["go", "build", "-o", "auth"]

ENTRYPOINT ["./auth"]

EXPOSE 1024
