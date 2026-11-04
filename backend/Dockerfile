# Programų serverio atvaizdas (NFR-36)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 mealplanner
COPY --from=build /app/target/meal-planner.jar meal-planner.jar
USER mealplanner
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/meal-planner.jar"]
