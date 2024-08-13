# Image Quality Improvement Bot

Этот проект представляет собой телеграм-бота, который использует технологии суперразрешения изображений для повышения их качества.

## Как использовать

1. Запустите бота с помощью команды `/start`.
2. Отправьте изображение для обработки.
3. Получите улучшенное изображение от бота.

## Особенности

- Поддержка изображений до 1920x1080 пикселей.
- Использование модели FSRCNN для суперразрешения.

## Требования

- Java 17
- Docker (если вы хотите развернуть приложение в контейнере)

## Установка

1. Склонируйте репозиторий: <br>
   git clone https://github.com/ваш-аккаунт/imageQualityImprovementBot.git
2.  Соберите проект с помощью Maven:<br>
   mvn clean package -DskipTests
    
## Использование Docker
1. Сборка Docker контейнера:<br>
docker build -t image-quality-bot .<br>
2. Запуск Docker контейнера:<br>
docker run -d -p 8080:80 image-quality-bot<br>

## Тестирование бота
Вы можете протестировать работу бота в Telegram по следующей ссылке:<br>
t.me/PhotoImprovingBot

## Лицензия
Этот проект лицензирован под лицензией MIT.


