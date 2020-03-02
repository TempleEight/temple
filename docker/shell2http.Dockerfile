FROM hadolint/hadolint:v1.17.5-4-g8db7482-debian AS hadolint
FROM golang:1.13.7-alpine AS golang

FROM msoap/shell2http:1.13

RUN apk update
RUN apk upgrade
RUN apk add python3

# Handolint config
COPY --from=hadolint /bin/hadolint /bin/hadolint

# Golang config
COPY --from=golang /usr/local/go/ /usr/local/go/
ENV PATH /usr/local/go/bin:$PATH

# Install jq for json parsing
RUN wget https://github.com/stedolan/jq/releases/download/jq-1.6/jq-linux64
RUN chmod +x jq-linux64

# Copy go build script across
COPY configure_go.py configure_go.py

ENTRYPOINT /app/shell2http -show-errors\
 -form GET:/hadolint "echo \$v_dockerfile | ./jq-linux64 -r .contents > Dockerfile && hadolint Dockerfile"\
 GET:/go "python3 configure_go.py && cd \$v_root && go build \$v_entrypoint 2>&1"
