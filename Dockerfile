# Etapa 1: Build con Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Etapa 2: Imagen ligera para producción
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/algoritmos-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
CMD ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8081}"]
