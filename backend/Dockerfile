# Imagen del backend (RNF-04). Se construye desde la raíz del repositorio para incluir las migraciones de Flyway:
#   docker build -f backend/Dockerfile .
# Etapa de construcción: descarga las dependencias en una capa propia y empaqueta el JAR sin ejecutar las pruebas,
# que ya se ejecutan en CI con Testcontainers.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline
COPY backend/src src
RUN ./mvnw -B -ntp -DskipTests package && cp target/smartops-*.jar app.jar

# Etapa de ejecución: JRE sin privilegios de administrador.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S migal && adduser -S -G migal migal && mkdir -p /app/almacen-documentos \
    && chown migal:migal /app/almacen-documentos
COPY --from=build /build/app.jar app.jar
COPY database/migraciones /app/migraciones
ENV FLYWAY_LOCATIONS=filesystem:/app/migraciones \
    MIGAL_DOCUMENTOS_DIRECTORIO=/app/almacen-documentos
USER migal
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
