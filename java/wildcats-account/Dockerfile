FROM eclipse-temurin:21-jre

ENV TZ=Europe/Istanbul \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport -XX:+UseG1GC"

WORKDIR /app
COPY target/wildcats-account-1.0.0.jar /app/app.jar

EXPOSE 8081

ENTRYPOINT ["java","-jar","/app/app.jar"]
