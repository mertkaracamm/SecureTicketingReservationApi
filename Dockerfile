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
ENTRYPOINT ["sh", "-c", "java -jar app.jar --spring.datasource.url=jdbc:postgresql://tramway.proxy.rlwy.net:43514/railway --spring.datasource.username=postgres --spring.datasource.password=${SPRING_DATASOURCE_PASSWORD} --spring.flyway.url=jdbc:postgresql://tramway.proxy.rlwy.net:43514/railway --spring.flyway.user=postgres --spring.flyway.password=${SPRING_FLYWAY_PASSWORD} --spring.data.redis.host=redis.railway.internal --spring.data.redis.port=6379 --spring.data.redis.password=${REDIS_PASSWORD} --spring.data.redis.username=default"]
  
  