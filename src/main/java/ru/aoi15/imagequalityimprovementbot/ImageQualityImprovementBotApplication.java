package ru.aoi15.imagequalityimprovementbot;

import javax.annotation.PostConstruct;
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
    @Autowired
    private ImageSuperResolutionBot bot;

        public static void main(String[] args) {
            SpringApplication.run(ImageQualityImprovementBotApplication.class, args);
        }

        @PostConstruct
        public void init() {
            try {
                TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
                botsApi.registerBot(bot);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

}

