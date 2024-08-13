package ru.aoi15.imagequalityimprovementbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class ImageSuperResolutionBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(ImageSuperResolutionBot.class);

    @Value("${bot.token}")
    private String botToken;

    @Value("${bot.name}")
    private String botName;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final SuperResolutionProcessor processor = new SuperResolutionProcessor();
    private final ConcurrentHashMap<Long, ImageProcessingTask> activeUsers = new ConcurrentHashMap<>();
    private static final long MAX_PROCESSING_TIME = TimeUnit.MINUTES.toMillis(5);

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @PostConstruct
    public void setupCommands() {
        List<BotCommand> botCommands = List.of(
                new BotCommand("/start", "Запустить бота"),
                new BotCommand("/help", "Показать меню помощи"),
                new BotCommand("/about", "Информация о боте")
        );

        try {
            execute(new SetMyCommands(botCommands, new BotCommandScopeDefault(), null));
            logger.info("Команды бота успешно установлены.");
        } catch (TelegramApiException e) {
            logger.error("Ошибка при установке команд бота: {}", e.getMessage(), e);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        logger.debug("Получено обновление: {}", update);
        executorService.submit(() -> processUpdate(update));
    }

    private void processUpdate(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (message.hasText()) {
                handleTextMessage(message);
            } else if (message.hasDocument() || message.hasPhoto()) {
                handleImageMessage(message);
            }
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        }
    }

    private void handleTextMessage(Message message) {
        String chatId = message.getChatId().toString();
        String text = message.getText();
        logger.info("Получено текстовое сообщение от пользователя {}: {}", chatId, text);

        Map<String, Runnable> commandHandlers = Map.of(
                "/start", () -> sendWelcomeMessage(chatId),
                "/help", () -> sendHelpMessage(chatId),
                "/about", () -> sendAboutMessage(chatId)
        );

        commandHandlers.getOrDefault(text, () -> sendTextMessage(chatId, "Неизвестная команда. Используйте /help для получения списка команд.")).run();
    }

    private void handleImageMessage(Message message) {
        long chatId = message.getChatId();

        if (activeUsers.containsKey(chatId)) {
            ImageProcessingTask task = activeUsers.get(chatId);
            if (task.isTimedOut(MAX_PROCESSING_TIME)) {
                activeUsers.remove(chatId);
                logger.info("Задача пользователя {} превысила максимальное время обработки. Пользователю разрешено отправить новое изображение.", chatId);
            } else {
                sendTextMessage(String.valueOf(chatId), "Ваше предыдущее изображение еще обрабатывается. Пожалуйста, подождите.");
                logger.info("Пользователь {} пытался отправить новое изображение до завершения обработки предыдущего.", chatId);
                return;
            }
        }

        activeUsers.put(chatId, new ImageProcessingTask());
        logger.info("Начата новая задача по обработке изображения для пользователя {}", chatId);

        executorService.submit(() -> {
            String filePath = downloadFileFromMessage(message);
            if (filePath != null) {
                sendTextMessage(String.valueOf(chatId), "Начинаю улучшение изображения...");

                try {
                    CompletableFuture<String> future = processor.process(filePath);
                    future.thenAccept(outputImagePath -> handleProcessingResult(chatId, filePath, outputImagePath));
                } catch (Exception e) {
                    logger.error("Ошибка при обработке изображения для пользователя {}: {}", chatId, e.getMessage(), e);
                    sendTextMessage(String.valueOf(chatId), "Произошла ошибка при обработке изображения.");
                    activeUsers.remove(chatId);
                }
            } else {
                activeUsers.remove(chatId);
            }
        });
    }

    private String downloadFileFromMessage(Message message) {
        String fileId;
        try {
            if (message.hasPhoto()) {
                fileId = message.getPhoto().get(message.getPhoto().size() - 1).getFileId();
                return downloadPhotoById(fileId);
            } else if (message.hasDocument()) {
                fileId = message.getDocument().getFileId();
                return downloadDocumentById(fileId);
            }
        } catch (TelegramApiException e) {
            logger.error("Ошибка при загрузке файла от пользователя {}: {}", message.getChatId(), e.getMessage(), e);
            sendTextMessage(String.valueOf(message.getChatId()), "Ошибка при загрузке файла.");
        }
        return null;
    }

    private void handleProcessingResult(long chatId, String inputFilePath, String outputImagePath) {
        if (outputImagePath == null) {
            sendTextMessage(String.valueOf(chatId), "Изображение слишком большое. Максимально допустимый размер: 1920x1080 пикселей.");
            logger.info("Изображение от пользователя {} оказалось слишком большим для обработки.", chatId);
            activeUsers.remove(chatId);
            return;
        }

        try {
            SendDocument msg = new SendDocument();
            msg.setChatId(String.valueOf(chatId));
            msg.setDocument(new InputFile(new File(outputImagePath)));
            execute(msg);

            sendTextMessage(String.valueOf(chatId),
                    "Улучшение изображения завершено. Время обработки: " + processor.getLastProcessingTime() + " мс");

            cleanupFiles(inputFilePath, outputImagePath);
            logger.info("Обработка изображения для пользователя {} завершена успешно.", chatId);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке изображения пользователю {}: {}", chatId, e.getMessage(), e);
            sendTextMessage(String.valueOf(chatId), "Произошла ошибка при отправке изображения.");
        } finally {
            activeUsers.remove(chatId);
        }
    }

    private void cleanupFiles(String inputFilePath, String outputImagePath) {
        File inputFile = new File(inputFilePath);
        File outputFile = new File(outputImagePath);
        if (inputFile.delete()) {
            logger.info("Файл {} успешно удален.", inputFile.getName());
        } else {
            logger.warn("Не удалось удалить файл {}.", inputFile.getName());
        }
        if (outputFile.delete()) {
            logger.info("Файл {} успешно удален.", outputFile.getName());
        } else {
            logger.warn("Не удалось удалить файл {}.", outputFile.getName());
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String chatId = callbackQuery.getMessage().getChatId().toString();
        String data = callbackQuery.getData();
        logger.info("Получен запрос от пользователя {}: {}", chatId, data);

        Map<String, Runnable> commandHandlers = Map.of(
                "/start", () -> sendWelcomeMessage(chatId),
                "/help", () -> sendHelpMessage(chatId),
                "/about", () -> sendAboutMessage(chatId)
        );

        commandHandlers.getOrDefault(data, () -> sendTextMessage(chatId, "Неизвестная команда. Используйте /help для получения списка команд.")).run();
    }

    private String downloadPhotoById(String fileId) throws TelegramApiException {
        String filePath = execute(new org.telegram.telegrambots.meta.api.methods.GetFile(fileId)).getFilePath();
        String savePath = "downloaded_" + fileId + ".jpg";
        downloadFile(filePath, new File(savePath));
        logger.info("Фотография с ID {} успешно загружена и сохранена по пути {}", fileId, savePath);
        return savePath;
    }

    private String downloadDocumentById(String fileId) throws TelegramApiException {
        String filePath = execute(new org.telegram.telegrambots.meta.api.methods.GetFile(fileId)).getFilePath();
        String savePath = "downloaded_" + fileId + ".jpg";
        downloadFile(filePath, new File(savePath));
        logger.info("Документ с ID {} успешно загружен и сохранен по пути {}", fileId, savePath);
        return savePath;
    }

    private void sendWelcomeMessage(String chatId) {
        String welcomeText = "Добро пожаловать в PhotoImprovingBot!\n" +
                "Я помогу вам улучшить качество ваших фотографий.\n" +
                "Просто отправьте мне изображение, и я обработаю его для вас.\n\n" +
                "Выберите действие в меню ниже.";

        sendTextMessageWithMenu(chatId, welcomeText);
        logger.info("Отправлено приветственное сообщение пользователю {}", chatId);
    }

    private void sendHelpMessage(String chatId) {
        String helpText = "Помощь по PhotoImprovingBot:\n\n" +
                "/start - Запустить бота\n" +
                "/help - Показать это сообщение\n" +
                "/about - Информация о боте\n\n" +
                "Отправьте изображение, чтобы я улучшил его качество.";
        sendTextMessage(chatId, helpText);
        logger.info("Отправлено сообщение с информацией о помощи пользователю {}", chatId);
    }

    private void sendAboutMessage(String chatId) {
        String aboutText = "PhotoImprovingBot был создан Алексеем Берко.\n" +
                "Этот бот использует технологии суперразрешения изображений для повышения их качества.\n" +
                "Отправьте изображение, и я улучшу его для вас!";
        sendTextMessage(chatId, aboutText);
        logger.info("Отправлено сообщение с информацией о боте пользователю {}", chatId);
    }

    private void sendTextMessageWithMenu(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("/start"));
        row1.add(new KeyboardButton("/help"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("/about"));

        keyboard.add(row1);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
            logger.info("Сообщение с меню отправлено пользователю {}", chatId);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке сообщения пользователю {}: {}", chatId, e.getMessage(), e);
        }
    }

    private void sendTextMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
            logger.info("Текстовое сообщение отправлено пользователю {}", chatId);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке текстового сообщения пользователю {}: {}", chatId, e.getMessage(), e);
        }
    }
}
