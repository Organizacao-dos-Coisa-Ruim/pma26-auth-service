# Imagem a partir do jar já buildado.
# O build não acontece dentro do Docker porque o service depende das interfaces pma:auth e pma:account,
# que só existem no repositório Maven local (./mvnw install em pma26-auth e pma-26-account).
#
#   ./mvnw clean package
#   docker build -t auth .
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/auth-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
