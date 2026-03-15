FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
COPY src ./src
RUN chmod +x mvnw && ./mvnw clean package -DskipTests
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", \
  "--spring.datasource.url=jdbc:postgresql://tramway.proxy.rlwy.net:43514/railway", \
  "--spring.datasource.username=postgres", \
  "--spring.datasource.password=FnwcUAQlZgEqOoKuLzPULMnMlURyanfh", \
  "--spring.flyway.url=jdbc:postgresql://tramway.proxy.rlwy.net:43514/railway", \
  "--spring.flyway.user=postgres", \
  "--spring.flyway.password=FnwcUAQlZgEqOoKuLzPULMnMlURyanfh", \
  "--spring.data.redis.host=redis.railway.internal", \
  "--spring.data.redis.port=6379"]