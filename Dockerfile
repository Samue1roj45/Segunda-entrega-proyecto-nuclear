# SOMA · backend único (módulos 1 a 6) en un solo contenedor.
# Etapa 1: compila el jar con Maven y Java 21. Etapa 2: sólo el JRE y el jar.
FROM maven:3.9-eclipse-temurin-21 AS compilacion
WORKDIR /fuente
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY aplicacion aplicacion
COPY modulo-1/backend modulo-1/backend
COPY modulo-2/backend modulo-2/backend
COPY modulo-3-4/backend modulo-3-4/backend
COPY modulo-5-6/backend modulo-5-6/backend
RUN mvn -B -q -DskipTests package && cp target/plataforma-casos-*.jar /fuente/soma.jar

FROM eclipse-temurin:21-jre
ENV TZ=America/Bogota ZONA_HORARIA=America/Bogota
RUN useradd --system --uid 1001 soma
WORKDIR /app
COPY --from=compilacion /fuente/soma.jar soma.jar
USER soma
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=America/Bogota", "-jar", "/app/soma.jar"]
