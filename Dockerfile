# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file and the model file into the container
COPY target/imageQualityImprovementBot-0.0.1-SNAPSHOT.jar /app/app.jar
COPY FSRCNN_x4.pb /app/FSRCNN_x4.pb

# Expose the port that your application will run on
EXPOSE 8080

# Run the application
CMD ["java", "-Xmx1024m", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]