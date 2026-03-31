FROM eclipse-temurin:21-jdk-alpine AS build

ENV BUILD_DIR=/build

ARG RELAY_SERVER_VERSION

RUN mkdir -p $BUILD_DIR
WORKDIR $BUILD_DIR

# copy only gradle-based resources for optimized caching
COPY gradle $BUILD_DIR/gradle
COPY gradlew $BUILD_DIR/gradlew
COPY build.gradle.kts $BUILD_DIR/build.gradle.kts
COPY settings.gradle.kts $BUILD_DIR/settings.gradle.kts

RUN chmod +x $BUILD_DIR/gradlew
RUN cd $BUILD_DIR

RUN ./gradlew dependencies --no-daemon

# copy rest of resources
COPY src $BUILD_DIR/src
COPY docker $BUILD_DIR/docker

RUN ./gradlew clean --no-daemon

RUN RELAY_SERVER_VERSION=${RELAY_SERVER_VERSION} \
  ./gradlew shadowJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

ENV BUILD_DIR=/build
ENV ENTRY_DIR=/app
ENV JAR_NAME=relay_server.jar

WORKDIR $ENTRY_DIR

RUN addgroup -S relaygroup && \
    adduser -S relayuser -G relaygroup

COPY --chown=relayuser:relaygroup --from=build $BUILD_DIR/.bin/$JAR_NAME $ENTRY_DIR/$JAR_NAME
COPY --chown=relayuser:relaygroup --from=build $BUILD_DIR/docker/entrypoint $ENTRY_DIR/entrypoint

RUN sed -i \
  -e "s/\$JAR_NAME/$JAR_NAME/g" \
  entrypoint

LABEL maintainer="Miłosz Gilga <miloszgilga@gmail.com>"

EXPOSE 8080 1883

USER relayuser

ENTRYPOINT [ "./entrypoint" ]
