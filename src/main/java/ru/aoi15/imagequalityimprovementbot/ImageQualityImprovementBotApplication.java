package ru.aoi15.imagequalityimprovementbot;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
@Component
public class ImageQualityImprovementBotApplication {

    private static final Logger logger = LoggerFactory.getLogger(ImageQualityImprovementBotApplication.class);

    @Autowired
    private ImageSuperResolutionBot bot;

    public static void main(String[] args) {
        SpringApplication.run(ImageQualityImprovementBotApplication.class, args);
        logger.info("Приложение успешно запущено.");
    }

    @PostConstruct
    public void init() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
            logger.info("Бот успешно зарегистрирован в Telegram API.");
        } catch (TelegramApiException e) {
            logger.error("Ошибка при регистрации бота: {}", e.getMessage(), e);
        }
    }
}
