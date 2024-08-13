package ru.aoi15.imagequalityimprovementbot;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_dnn_superres.DnnSuperResImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class SuperResolutionProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SuperResolutionProcessor.class);
    private static final int MAX_WIDTH = 4000;  // Максимальная ширина
    private static final int MAX_HEIGHT = 4000; // Максимальная высота
    private long lastProcessingTime;

    @Async
    public CompletableFuture<String> process(String filePath) {
        long startTime = System.nanoTime();
        logger.info("Начата обработка изображения: {}", filePath);

        Mat inputImage = opencv_imgcodecs.imread(filePath);

        if (inputImage.empty()) {
            logger.error("Не удалось загрузить изображение: {}", filePath);
            return CompletableFuture.completedFuture(filePath);
        }

        if (isImageTooLarge(inputImage)) {
            logger.warn("Ошибка: Изображение слишком большое. Максимально допустимый размер: {}x{} пикселей. Файл: {}",
                    MAX_WIDTH, MAX_HEIGHT, filePath);
            return CompletableFuture.completedFuture(null);
        }

        try {
            DnnSuperResImpl sr = new DnnSuperResImpl();
            String modelPath = "FSRCNN-small_x2.pb";
            sr.readModel(modelPath);
            sr.setModel("fsrcnn", 2);

            Mat outputImage = new Mat();
            sr.upsample(inputImage, outputImage);

            String outputImagePath = "upscaled_" + filePath;
            opencv_imgcodecs.imwrite(outputImagePath, outputImage);

            long endTime = System.nanoTime();
            lastProcessingTime = (endTime - startTime) / 1_000_000;
            logger.info("Обработка изображения завершена: {}. Время обработки: {} мс", outputImagePath, lastProcessingTime);

            return CompletableFuture.completedFuture(outputImagePath);
        } catch (Exception e) {
            logger.error("Ошибка при обработке изображения: {}", filePath, e);
            return CompletableFuture.completedFuture(null);
        }
    }

    public long getLastProcessingTime() {
        return lastProcessingTime;
    }

    private boolean isImageTooLarge(Mat image) {
        boolean tooLarge = image.size().width() > MAX_WIDTH || image.size().height() > MAX_HEIGHT;
        if (tooLarge) {
            logger.debug("Изображение слишком большое: {}x{}", image.size().width(), image.size().height());
        }
        return tooLarge;
    }
}
