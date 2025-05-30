# Usa una imagen oficial de Java 17 para ejecutar el JAR
FROM eclipse-temurin:17-jre-alpine

# Crea un directorio de trabajo
WORKDIR /app

# Copia el JAR generado al contenedor
COPY target/algoritmos-0.0.1-SNAPSHOT.jar app.jar

# Expone el puerto (Render usará la variable PORT)
EXPOSE 8080

# Comando para ejecutar el JAR, usando el puerto de Render si está definido
CMD ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8081}"]
