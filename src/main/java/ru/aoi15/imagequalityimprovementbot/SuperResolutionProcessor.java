package ru.aoi15.imagequalityimprovementbot;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_dnn_superres.DnnSuperResImpl;
import org.springframework.stereotype.Component;

@Component
public class SuperResolutionProcessor {
    public String process(String filePath) {
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
//        String modelPath = "EDSR_x2.pb";

        sr.readModel(modelPath);
        sr.setModel("fsrcnn", 4); // Использование модели FSRCNN с масштабом 4
//        sr.setModel("edsr", 2); // Использование модели FSRCNN с масштабом 4

        // Применение модели для увеличения разрешения изображения
        Mat outputImage = new Mat();
        sr.upsample(inputImage, outputImage);

        // Сохранение результата
        String outputImagePath = "improved\\" + "upscaled_" + filePath.toString().replace("downloaded\\", "");
        opencv_imgcodecs.imwrite(outputImagePath, outputImage);

        return outputImagePath;


    }

}
