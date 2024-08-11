package ru.aoi15.imagequalityimprovementbot;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_dnn_superres.DnnSuperResImpl;
import org.springframework.stereotype.Component;

@Component
public class SuperResolutionProcessor {

    public String process(String filePath) {
        long startTime = System.nanoTime();

        // Применение суперразрешения
        Mat inputImage = opencv_imgcodecs.imread(filePath);

        // Проверка, удалось ли загрузить изображение
        if (inputImage.empty()) {
            System.out.println("Не удалось загрузить изображение: " + filePath);
            return filePath;
        }

        // Создание объекта DnnSuperResImpl для суперразрешения
        DnnSuperResImpl sr = new DnnSuperResImpl();
        String modelPath = "FSRCNN_x4.pb";
        sr.readModel(modelPath);
        sr.setModel("fsrcnn", 4); // Использование модели FSRCNN с масштабом 4

        // Применение модели для увеличения разрешения изображения
        Mat outputImage = new Mat();
        sr.upsample(inputImage, outputImage);

        // Сохранение результата
        String outputImagePath = "upscaled_" + filePath.replace("downloaded\\", "");
        opencv_imgcodecs.imwrite(outputImagePath, outputImage);

        long endTime = System.nanoTime();
        long durationInMillis = (endTime - startTime) / 1_000_000;
        System.out.println("Время обработки: " + durationInMillis + " мс");

        return outputImagePath;
    }
}
