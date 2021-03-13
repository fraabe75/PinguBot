package com.example.demo.core;

import com.example.demo.plugins.GuildMessageReceivedPlugin;
import com.example.demo.plugins.Plugin;
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

    private final List<Plugin> plugins;
    private final List<GuildMessageReceivedPlugin> guildMessageReceivedPlugins;

    public MainListener(List<Plugin> plugins, List<GuildMessageReceivedPlugin> guildMessageReceivedPlugins) {
        this.plugins = plugins;
        this.guildMessageReceivedPlugins = guildMessageReceivedPlugins;
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {

        if (event.getAuthor().isBot())
            return;

        TextChannel channel = event.getChannel();

        String message = event.getMessage().getContentRaw().toLowerCase();

        if (!message.startsWith(prefix))
            return;

        String[] args = message.substring(prefix.length()).trim().split(" ", 3);

        switch (args[0]) {
            case "help" -> {
                if (args.length == 2) {
                    for (Plugin plugin : plugins) {
                        if (plugin.getName().equals(args[1])) {
                            channel.sendMessage(plugin.help(prefix)).queue();
                            return;
                        }
                    }
                    channel.sendMessage("Couldn't find command").queue();
                } else {
                    channel.sendMessage(help()).queue();
                }
                return;
            }
            case "user" -> {
                if(args.length == 2) {
                    channel.sendMessage(usermanagement.guildMessageReceived(event, args[1], args[2], prefix)).queue();
                } else {
                    channel.sendMessage(user()).queue();
                }
                return;
            }
            case "roulette" -> {
                if(args.length == 2) {
                    channel.sendMessage(roullette.guildMessageReceived(event, args[1], args[2], prefix)).queue();
                } else {
                    channel.sendMessage(user()).queue();
                }
                return;
            }
            case "blackjack" -> {
                if(args.length == 2) {
                    channel.sendMessage(blackjack.guildMessageReceived(event, args[1], args[2], prefix)).queue();
                } else {
                    channel.sendMessage(user()).queue();
                }
                return;
            }
        }

        if (args[0].equals("commands")) {
            channel.sendMessage(plugins.toString()).queue();
            return;
        }

        if (args.length != 2) {
            args = new String[]{args[0], ""};
        }

        if (!notifyOnGuildMessageReceived(event, args[0], args[1]))
            channel.sendMessage("Couldn't find command in any plugins").queue();
    }

    private MessageEmbed help() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Main help page");
        builder.setDescription(prefix + "commands\n" + prefix + "help <command>");
        return builder.build();
    }

    private MessageEmbed user() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Your Account:");
        builder.setDescription("acc data...");
        return builder.build();
    }

    private boolean notifyOnGuildMessageReceived(GuildMessageReceivedEvent event, String command, String param) {
        boolean found = false;

        for (GuildMessageReceivedPlugin guildMessageReceivedPlugin : guildMessageReceivedPlugins) {
            found = guildMessageReceivedPlugin.guildMessageReceived(event, command, param, prefix) || found;
        }

        return found;
    }

}
