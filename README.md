# PinguBot

Discord Bot written in Java for the TUM Computer Science first semester Discord server gamejam.

The JDA is used for Discord API communication. The token and the MariaDB password, url and port are defined in the
`/src/main/resources/application.properties` file.

For starting the springboot application, either install the maven dependencies and run the application using 
`/src/main/java/com/example/pingubot/PinguBotApplication.java` as the main class, or use the maven wrapper script:

`mvnw clean install`

The application can then easily be started using:

`mvnw spring-boot:run`

To export the application to an executable jar, run the following wrapper command:

`mvnw clean compile package`

This will generate a `pingubot-springboot-0.1.0.jar` file.