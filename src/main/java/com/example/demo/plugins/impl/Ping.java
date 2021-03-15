package com.example.demo.plugins.impl;

import com.example.demo.plugins.GuildMessageReceivedPlugin;
import com.example.demo.plugins.Plugin;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.stereotype.Service;

@Service
public class Ping extends Plugin implements GuildMessageReceivedPlugin {

    public Ping() {
        setName("Ping");
        setDescription("My ping is 0.0ms, what about yours?");
        addCommands("ping");
    }

    @Override
    public boolean guildMessageReceived(GuildMessageReceivedEvent event, String command, String param, String prefix) {

        if (!commands().contains(command)) {
            return false;
        }

        TextChannel channel = event.getChannel();

        if (command.equals("")) {
            channel.sendMessage("Try something naughty...").queue();
        }

        return true;
    }
}
