package ru.aoi15.imagequalityimprovementbot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;

@Component
public class ImageSuperResolutionBot extends TelegramLongPollingBot {

//    @Value("${bot.token}")
    private String botToken = "7422775013:AAGwPCl6HwM1EReUvbYfREYe6gbI6B-PLvg";

//    @Value("${bot.name}")
    private String botName = "PhotoImprovingBot";

    private final SuperResolutionProcessor processor = new SuperResolutionProcessor();

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && (update.getMessage().hasDocument() || update.getMessage().hasPhoto())) {
            Message message = update.getMessage();
            String fileId;
            String filePath = null;

            if (message.hasPhoto()) {
                fileId = message.getPhoto().get(message.getPhoto().size() - 1).getFileId();
                try {
                    filePath = downloadPhotoById(fileId);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                    sendTextMessage(message.getChatId().toString(), "Ошибка при загрузке фотографии.");
                    return;
                }
            } else {
                Document document = message.getDocument();
                fileId = document.getFileId();
                try {
                    filePath = downloadDocumentById(fileId);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                    sendTextMessage(message.getChatId().toString(), "Ошибка при загрузке документа.");
                    return;
                }
            }

            if (filePath != null) {
                try {
                    long startTime = System.nanoTime();

                    String outputImagePath = processor.process(filePath);

                    long endTime = System.nanoTime();
                    long durationInMillis = (endTime - startTime) / 1_000_000;

                    // Отправка улучшенного изображения обратно пользователю
                    SendDocument msg = new SendDocument();
                    msg.setChatId(message.getChatId().toString());
                    msg.setDocument(new InputFile(new File(outputImagePath)));
                    execute(msg);

                    // Отправка сообщения с временем обработки
                    sendTextMessage(message.getChatId().toString(), "Изображение обработано за " + durationInMillis + " мс.");

                    // Удаление временного файла
                    File file = new File(outputImagePath);
                    if (file.delete()) {
                        System.out.println(file.getName() + " deleted");
                    } else {
                        System.out.println(file.getName() + " not deleted");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    sendTextMessage(message.getChatId().toString(), "Произошла ошибка при обработке изображения.");
                }
            }
        } else {
            sendTextMessage(update.getMessage().getChatId().toString(), "Пожалуйста, отправьте изображение.");
        }
    }

    private String downloadPhotoById(String fileId) throws TelegramApiException {
        String filePath = execute(new org.telegram.telegrambots.meta.api.methods.GetFile(fileId)).getFilePath();
        String savePath = "downloaded\\" + "downloaded_" + fileId + ".jpg";
        downloadFile(filePath, new File(savePath));
        return savePath;
    }

    private String downloadDocumentById(String fileId) throws TelegramApiException {
        String filePath = execute(new org.telegram.telegrambots.meta.api.methods.GetFile(fileId)).getFilePath();
        String savePath = "downloaded\\" + "downloaded_" + fileId + ".jpg";
        downloadFile(filePath, new File(savePath));
        return savePath;
    }

    private void sendTextMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
