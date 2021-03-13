package com.example.demo.mangos;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.stereotype.Component;

@Component
public interface GuildMessageReceivedMango {

    boolean guildMessageReceived(GuildMessageReceivedEvent event, String command, String param, String prefix);

}
