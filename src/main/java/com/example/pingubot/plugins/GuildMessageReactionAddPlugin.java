package com.example.pingubot.plugins;

import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import org.springframework.stereotype.Component;

@Component
public interface GuildMessageReactionAddPlugin {

    boolean guildMessageReactionAdd(GuildMessageReactionAddEvent event, String messageId);
}
