FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# fonts-dejavu-core: ffmpeg's drawtext watermark filter needs an explicit
# font file (see VideoCompositionService's LINUX_DEFAULT_FONT).
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg \
    fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/kidsstory-backend.jar ./app.jar

EXPOSE 8000

CMD ["java", "-jar", "app.jar"]
