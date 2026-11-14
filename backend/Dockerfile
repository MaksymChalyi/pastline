# Збірка: Maven Wrapper з репозиторію, тести пропущені (Testcontainers потребує Docker, якого тут немає;
# тести проходять локально перед кожним комітом).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
RUN chmod +x mvnw && ./mvnw -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 pastline
COPY --from=build /app/target/pastline-backend-0.0.1-SNAPSHOT.jar app.jar
USER pastline
# 512 МБ на безкоштовному Render: купа до 70% пам'яті, SerialGC з найменшими накладними витратами
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -XX:+ExitOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE=cloud
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
