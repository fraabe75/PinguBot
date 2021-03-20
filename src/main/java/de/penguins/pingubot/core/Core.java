package de.penguins.pingubot.core;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.security.auth.login.LoginException;
import java.util.EnumSet;

@Configuration
public class Core {

    @Value("${token}")
    private String token;
    @Value("${prefix}")
    private String prefix;

    @Bean
    public JDA login(MainListener mainListener) throws LoginException {
        EnumSet<GatewayIntent> intents = EnumSet.of(
                GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS
        );

        return JDABuilder.create(token, intents).setActivity(Activity.listening(prefix + " help"))
                .addEventListeners(mainListener)
                .build();
    }

}

