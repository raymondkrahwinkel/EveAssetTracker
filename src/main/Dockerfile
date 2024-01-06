#
# Build
#
FROM gradle:jdk17-alpine AS build
COPY ../.. /home/gradle
RUN gradle build --no-daemon

#
# Package stage
#
FROM openjdk:17-alpine
ENV CONFIG_LOCATION='file:/app/application.properties'
EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/build/libs/*.jar /app/spring-boot-application.jar
ENTRYPOINT ["java", "-XX:+UnlockExperimentalVMOptions", "-Djava.security.egd=file:/dev/./urandom","-jar","/app/spring-boot-application.jar", "--spring.config.location=$CONFIG_LOCATION"]