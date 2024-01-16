#
# Build
#
FROM gradle:jdk17-alpine AS build
COPY . /home/gradle
RUN ls -alh /home/gradle
RUN gradle build --no-daemon

#
# Package stage
#
FROM openjdk:17-alpine
EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/build/libs/*.jar /app/spring-boot-application.jar
ENTRYPOINT ["java", "-XX:+UnlockExperimenta- ficlVMOptions", "-Djava.security.egd=file:/dev/./urandom","-jar","/app/spring-boot-application.jar", "--spring.config.location=file:/app/application.properties"]