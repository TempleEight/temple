FROM hadolint/hadolint:latest AS hadolint

FROM msoap/shell2http

COPY --from=hadolint /bin/hadolint /bin/hadolint

RUN wget https://github.com/stedolan/jq/releases/download/jq-1.6/jq-linux64
RUN chmod +x jq-linux64

ENTRYPOINT /app/shell2http -show-errors -form GET:/hadolint "echo \$v_dockerfile | ./jq-linux64 -r .contents > Dockerfile && hadolint Dockerfile"
