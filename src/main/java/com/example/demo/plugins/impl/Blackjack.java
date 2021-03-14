package com.example.demo.plugins.impl;

import com.example.demo.plugins.GuildMessageReceivedPlugin;
import com.example.demo.plugins.Plugin;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.stereotype.Service;

@Service
public class Blackjack extends Plugin implements GuildMessageReceivedPlugin {

    public Blackjack() {
        setName("Blackjack");
        setPrefix("blackjack");
        setDescription("Play a fun game of Blackjack!");
        addCommands("play");
    }

    @Override
    public boolean guildMessageReceived(GuildMessageReceivedEvent event, String command, String param, String prefix) {

        if (!commands().contains(command)) {
            return false;
        }

        TextChannel channel = event.getChannel();

        String dealerCard;
        String playerCardOne;
        String playerCardTwo;

        if (command.equals("play")) {
            channel.sendMessage("TODO: Blackjack").queue();
        }

        return true;
    }
}
