package com.example.demo.core;

import com.example.demo.mangos.GuildMessageReceivedMango;
import com.example.demo.mangos.Mango;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MainListener extends ListenerAdapter {

    @Value("${prefix}")
    private String prefix;

    private final List<Mango> mangos;
    private final List<GuildMessageReceivedMango> guildMessageReceivedMangos;

    public MainListener(List<Mango> mangos, List<GuildMessageReceivedMango> guildMessageReceivedMangos) {
        this.mangos = mangos;
        this.guildMessageReceivedMangos = guildMessageReceivedMangos;
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {

        if (event.getAuthor().isBot())
            return;

        TextChannel channel = event.getChannel();

        String message = event.getMessage().getContentRaw().toLowerCase();

        if (!message.startsWith(prefix))
            return;

        String[] args = message.substring(prefix.length()).trim().split(" ", 2);

        if(args[0].equals("naughty") && args[1].equals("slin")) {
            channel.sendMessage(onlyfans()).queue();
            return;
        }

        if (args[0].equals("mangos")) {
            channel.sendMessage(mangos.toString()).queue();
            return;
        }

        //!help
        //!help <mango>
        if (args[0].equals("help")) {
            if (args.length == 2) {
                for (Mango mango : mangos) {
                    if (mango.getName().equals(args[1])) {
                        channel.sendMessage(mango.help(prefix)).queue();
                        return;
                    }
                }
                channel.sendMessage("Couldn't find mango").queue();
            } else {
                channel.sendMessage(help()).queue();
            }
            return;
        }

        if (args.length != 2) {
            args = new String[]{args[0], ""};
        }

        if (!notifyOnGuildMessageReceived(event, args[0], args[1]))
            channel.sendMessage("Couldn't find command in any mangos").queue();
    }

    private MessageEmbed help() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Main help page");
        builder.setDescription(prefix + "mangos\n" + prefix + "help <mango>");
        return builder.build();
    }

    private MessageEmbed onlyfans() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Subscribe to my Onlyfans!");
        builder.setDescription("https://onlyfans.com/schnils69");
        builder.setThumbnail("https://cdn.pocket-lint.com/r/s/1200x/assets/images/153545-apps-news-feature-what-is-onlyfans-and-how-does-it-work-image2-sisy2dmz3f.JPG");
        return builder.build();
    }

    private boolean notifyOnGuildMessageReceived(GuildMessageReceivedEvent event, String command, String param) {
        boolean found = false;

        for (GuildMessageReceivedMango guildMessageReceivedMango : guildMessageReceivedMangos) {
            found = guildMessageReceivedMango.guildMessageReceived(event, command, param, prefix) || found;
        }

        return found;
    }

}
