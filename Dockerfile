FROM quay.io/democracyworks/didor:latest

RUN mkdir -p /usr/src/partner-http-api
WORKDIR /usr/src/partner-http-api

COPY project.clj /usr/src/partner-http-api/

RUN lein deps

COPY . /usr/src/partner-http-api

RUN lein test
RUN lein immutant war --name partner-http-api --destination target --nrepl-port=11111 --nrepl-start --nrepl-host=0.0.0.0
