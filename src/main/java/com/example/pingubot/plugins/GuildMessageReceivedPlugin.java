package com.example.pingubot.plugins;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.stereotype.Component;

@Component
public interface GuildMessageReceivedPlugin {

    boolean guildMessageReceived(GuildMessageReceivedEvent event, String command, String param, String prefix);

}
