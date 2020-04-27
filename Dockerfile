FROM openjdk:13-alpine
WORKDIR /usr/bin

RUN apk add --update ca-certificates openssl && update-ca-certificates
RUN wget -O /usr/bin/temple https://github.com/TempleEight/temple/releases/download/v0.1.0/temple-latest
RUN chmod +x /usr/bin/temple

CMD ["temple", "--version"]
