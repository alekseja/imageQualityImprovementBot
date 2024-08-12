# Используем более распространенный образ Maven с OpenJDK 17
FROM maven:3.8.4-openjdk-17-slim AS build

# Устанавливаем рабочую директорию внутри контейнера
WORKDIR /app

# Копируем pom.xml и загружаем зависимости
COPY pom.xml /app/
RUN mvn dependency:go-offline -B

# Копируем код проекта
COPY src /app/src

# Собираем приложение
RUN mvn package -DskipTests

# Используем минимальный образ OpenJDK для финального контейнера
FROM openjdk:17-jdk-slim

# Устанавливаем рабочую директорию внутри контейнера
WORKDIR /app

# Копируем JAR файл из стадии сборки и модель в контейнер
COPY --from=build /app/target/imageQualityImprovementBot-0.0.1-SNAPSHOT.jar /app/app.jar
COPY FSRCNN-small_x2.pb /app/FSRCNN-small_x2.pb

# Открываем порт, на котором будет работать приложение
EXPOSE 80

# Запускаем приложение
CMD ["java", "-Xmx1024m", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
