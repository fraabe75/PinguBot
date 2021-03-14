package com.example.demo.plugins.impl;

import com.example.demo.plugins.GuildMessageReceivedPlugin;
import com.example.demo.plugins.Plugin;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.stereotype.Service;

import java.awt.*;


@Service
public class Nils extends Plugin implements GuildMessageReceivedPlugin {

    public Nils() {
        setName("Nils");
        setPrefix("naughty");
        setDescription("Slightly overused insider");
        addCommands("slin");
    }

    @Override
    public boolean guildMessageReceived(GuildMessageReceivedEvent event, String command, String param, String prefix) {

        if (!commands().contains(command)) {
            return false;
        }

        TextChannel channel = event.getChannel();

        if (command.equals("slin")) {
            channel.sendMessage(onlyfans()).queue();
            channel.sendMessage("<@137294148397957120>").queue();
        }

        return true;
    }

    private MessageEmbed onlyfans() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setColor(Color.MAGENTA);
        builder.setTitle("Subscribe to my Onlyfans!");
        builder.setDescription("https://onlyfans.com/schnils69");
        builder.setThumbnail("https://cdn.pocket-lint.com/r/s/1200x/assets/images/153545-apps-news-feature-what-is" +
                             "-onlyfans-and-how-does-it-work-image2-sisy2dmz3f.JPG");
        return builder.build();
    }

}
