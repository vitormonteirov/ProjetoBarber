# ==============================================================================
# ESTÁGIO 1: Build da Aplicação com Maven & JDK 21
# ==============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Copia o Maven Wrapper e o pom.xml primeiro para aproveitar cache de camadas
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copia o código-fonte e empacota o JAR executável (sem rodar testes no build de imagem)
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# ==============================================================================
# ESTÁGIO 2: Runtime Enxuto (JRE 21 Alpine) com Usuário Não-Root (Hardening)
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Configura timezone padrão para America/Sao_Paulo e cria grupo/usuário não-root
RUN apk add --no-cache tzdata curl \
    && cp /usr/share/zoneinfo/America/Sao_Paulo /etc/localtime \
    && echo "America/Sao_Paulo" > /etc/timezone \
    && addgroup -S barbergroup && adduser -S barberuser -G barbergroup

COPY --from=builder /build/target/*.jar app.jar
RUN chown -R barberuser:barbergroup /app

USER barberuser

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=25s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8080/actuator/health || exit 1

ENV TZ=America/Sao_Paulo \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
