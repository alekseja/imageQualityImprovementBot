# Используем минимальный образ OpenJDK для финального контейнера
FROM openjdk:17-jdk-slim

# Устанавливаем рабочую директорию внутри контейнера
WORKDIR /app

# Копируем JAR файл из локальной сборки в контейнер
COPY target/imageQualityImprovementBot-0.0.1-SNAPSHOT.jar /app/app.jar
COPY FSRCNN-small_x2.pb /app/FSRCNN-small_x2.pb

# Открываем порт, на котором будет работать приложение
EXPOSE 80

# Запускаем приложение
CMD ["java", "-Xmx1024m", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
