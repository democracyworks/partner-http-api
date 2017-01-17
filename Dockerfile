FROM clojure:lein-2.7.1-alpine

RUN mkdir -p /usr/src/partner-http-api
WORKDIR /usr/src/partner-http-api

COPY project.clj /usr/src/partner-http-api/

ARG env=production

RUN lein with-profile $env deps

COPY . /usr/src/partner-http-api

RUN lein with-profiles $env,test test
RUN lein with-profile $env uberjar

CMD ["java", "-XX:+UseG1GC", "-jar", "target/partner-http-api.jar"]
