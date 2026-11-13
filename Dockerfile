FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN mkdir -p /var/data/foods /var/data/restaurants
COPY --from=build /workspace/target/foodie-express-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 10000
ENTRYPOINT ["sh", "-c", "exec java -XX:MaxRAMPercentage=65.0 -jar /app/app.jar"]
