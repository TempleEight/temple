FROM golang:1.13.7-alpine

WORKDIR /booking

COPY go.mod ./

RUN ["go", "mod", "download"]

COPY . .

COPY config.json /etc/booking-service/

RUN ["go", "build", "-o", "booking"]

ENTRYPOINT ["./booking"]

EXPOSE 1028
