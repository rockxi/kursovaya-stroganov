# Используем многоступенчатую сборку для оптимизации размера образа
FROM maven:3.9-eclipse-temurin-17 AS build

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем pom.xml и исходный код
COPY pom.xml .
COPY src ./src

# Собираем приложение
RUN mvn clean package -DskipTests

# Финальный образ
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Копируем собранный JAR из предыдущего этапа
COPY --from=build /app/target/frontend-1.0.0.jar app.jar

# Открываем порт
EXPOSE 8081

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "app.jar"]
