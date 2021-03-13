package com.example.demo.mangos.impl;

import com.example.javatemplatespringboot.mangos.GuildMessageReceivedMango;
import com.example.javatemplatespringboot.mangos.Mango;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.stereotype.Service;

@Service
public class Ping extends Mango implements GuildMessageReceivedMango {

    public Ping() {
        setName("Ping");

        addCommands("ping");
    }

    @Override
    public boolean guildMessageReceived(GuildMessageReceivedEvent event, String command, String param, String prefix) {

        if (!commands().contains(command))
            return false;

        TextChannel channel = event.getChannel();

        if (command.equals("ping")) channel.sendMessage("pong" + prefix).queue();

        return true;
    }
}
