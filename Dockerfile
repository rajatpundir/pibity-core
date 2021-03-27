# Build Stage 1
FROM gradle as stage1
COPY . /pibity-core
WORKDIR /pibity-core
RUN gradle clean build

# Build Stage 2
FROM openjdk:11-jdk-alpine
COPY --from=stage1 /pibity-core/build/libs/pibity-core-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
RUN apk add --no-cache bash
CMD java -jar app.jar
