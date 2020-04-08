FROM hadolint/hadolint:v1.17.5-4-g8db7482-debian AS hadolint
FROM golang:1.13.7-alpine AS golang

FROM msoap/shell2http:1.13

RUN apk update
RUN apk upgrade
RUN apk add python3
RUN apk add git
RUN apk add --update nodejs

# Handolint config
COPY --from=hadolint /bin/hadolint /bin/hadolint

# Golang config
COPY --from=golang /usr/local/go/ /usr/local/go/
ENV PATH /usr/local/go/bin:$PATH

# Swagger config
RUN npm install -g swagger-cli

# Install jq for json parsing
RUN wget https://github.com/stedolan/jq/releases/download/jq-1.6/jq-linux64
RUN chmod +x jq-linux64

# Install Kubeval
RUN wget https://github.com/instrumenta/kubeval/releases/latest/download/kubeval-linux-amd64.tar.gz
RUN tar -xzf kubeval-linux-amd64.tar.gz
RUN mv kubeval /usr/local/bin

# Copy go build script across
COPY configure_go.py configure_go.py

ENTRYPOINT /app/shell2http -show-errors\
 -form GET:/hadolint "echo \$v_dockerfile | ./jq-linux64 -r .contents > Dockerfile && hadolint Dockerfile"\
 GET:/go "python3 configure_go.py && cd \$v_root && go mod tidy &>/dev/null; go build -o output 2>&1"\
 GET:/swagger "echo \"\${v_openapi}\" | ./jq-linux64 -r .contents > openapi.yaml && swagger-cli validate openapi.yaml 2>&1"\
 GET:/kube "python3 configure_go.py && cd \$v_root; find . -name '*.yaml' | xargs kubeval 2>&1"
