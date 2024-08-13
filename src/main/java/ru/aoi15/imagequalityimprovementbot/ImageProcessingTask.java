package ru.aoi15.imagequalityimprovementbot;

public class ImageProcessingTask {
    private final long startTime;

    public ImageProcessingTask() {
        this.startTime = System.currentTimeMillis();
    }

    public boolean isTimedOut(long timeoutMillis) {
        return System.currentTimeMillis() - startTime > timeoutMillis;
    }
}

