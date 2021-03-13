package com.example.demo.plugins.impl;

import com.example.demo.plugins.GuildMessageReceivedPlugin;
import com.example.demo.plugins.Plugin;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class Help extends Plugin implements GuildMessageReceivedPlugin {

    public Help() {
        setName("Help");
        setPrefix("help");

        addCommands("help");
    }

    @Override
    public boolean guildMessageReceived(GuildMessageReceivedEvent event, String command, String param, String prefix) {

        if (!commands().contains(command))
            return false;

        TextChannel channel = event.getChannel();

        if (command.equals("help")) channel.sendMessage("pong" + prefix).queue();

        return true;


    }

}
