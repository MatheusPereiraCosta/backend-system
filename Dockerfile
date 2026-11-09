# ==============================================================================
# ESTÁGIO 1: Build do Backend Spring Boot 3 com Maven e Java 21
# ==============================================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copia apenas o pom.xml primeiro para cache das dependências do Maven
COPY pom.xml .

RUN mvn dependency:go-offline -B

# Copia o código fonte
COPY src ./src

# Compila e gera o JAR executável (testes já validados na pipeline)
RUN mvn clean package -DskipTests

# ==============================================================================
# ESTÁGIO 2: Runtime com JRE 21 Alpine Enxuta e Usuário Não-Root
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Criação de usuário e grupo de sistema não-root para execução segura
RUN addgroup -S agendapro && adduser -S agendapro -G agendapro

# Copia o JAR executável gerado no estágio de build
COPY --from=build /app/target/*.jar app.jar

# Define permissão para o usuário da aplicação
RUN chown -R agendapro:agendapro /app

# Executa como usuário não-root
USER agendapro

# Configurações otimizadas da JVM para containers Docker
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Healthcheck usando endpoint do Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
