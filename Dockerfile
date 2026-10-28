# ACMS - container image.  Build and run with:  docker compose up -d --build
# Nothing secret is copied into the image: .env is excluded by .dockerignore and read only when the
# container STARTS (env_file in compose.yaml).

# ---------- 1. Build the jar with Maven and JDK 25 ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

# Dependencies first: this layer is cached and only re-downloaded when pom.xml changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# mvnw may have Windows line endings (CRLF) after a Git checkout on Windows; Linux needs LF.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Tests are not run here: they need the acms_test database. Run them with .\mvnw.cmd test on Windows.
RUN ./mvnw -B -q package -DskipTests && cp target/*.jar target/app.jar

# ---------- 2. Split the jar into layers (Spring Boot 3.3+ / 4 "tools" mode) ----------
# Libraries change rarely, our code often: separate layers make rebuilds and pulls much faster.
RUN java -Djarmode=tools -jar target/app.jar extract --layers --destination target/extracted \
 && mkdir -p target/extracted/dependencies target/extracted/spring-boot-loader \
             target/extracted/snapshot-dependencies target/extracted/application

# ---------- 3. Run with only the Java runtime (no JDK, no Maven, no source code) ----------
FROM eclipse-temurin:25-jre
# Never run as root inside the container.
RUN groupadd --system acms && useradd --system --gid acms --no-create-home acms
WORKDIR /app
COPY --from=build /build/target/extracted/dependencies/ ./
COPY --from=build /build/target/extracted/spring-boot-loader/ ./
COPY --from=build /build/target/extracted/snapshot-dependencies/ ./
COPY --from=build /build/target/extracted/application/ ./
USER acms
EXPOSE 8081
# /app/app.jar finds its libraries in /app/lib through its manifest.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
