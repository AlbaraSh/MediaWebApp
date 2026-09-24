# syntax=docker/dockerfile:1

FROM node:22-alpine AS frontend
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9.11-eclipse-temurin-17 AS backend
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY --from=frontend /web/dist ./src/main/resources/static
RUN mvn -B -DskipTests package \
	&& mv target/backend-*.jar /app/app.jar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=backend /app/app.jar ./app.jar
COPY --chmod=755 docker/entrypoint.sh ./entrypoint.sh
RUN sed -i 's/\r$//' ./entrypoint.sh
USER spring
EXPOSE 8080
ENTRYPOINT ["./entrypoint.sh"]
