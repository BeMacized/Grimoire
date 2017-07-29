FROM openjdk:8-jdk

# Compile Grimoire
WORKDIR /usr/bin/app
COPY . .
RUN ./gradlew clean build uberjar

# Run Grimoire
CMD java -jar build/libs/Grimoire-2.0.jar