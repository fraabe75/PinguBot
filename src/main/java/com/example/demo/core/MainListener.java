package com.example.demo.core;

import com.example.demo.plugins.GuildMessageReceivedPlugin;
import com.example.demo.plugins.Plugin;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.Event;
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

        for (GuildMessageReceivedPlugin guildMessageReceivedPlugin : guildMessageReceivedPlugins) {
            if (((Plugin) guildMessageReceivedPlugin).getPrefix().equals(args[0])) {
                if (!notifyOnGuildMessageReceived(event, args[0], args[1]))
                    channel.sendMessage("Couldn't find command in any plugins").queue();
                return;
            }
        }

        //!help
        //!help <plugin>
        if (args[0].equals("help")) {
            if (args.length == 2) {
                for (Plugin plugin : plugins) {
                    if (plugin.getName().equals(args[1])) {
                        channel.sendMessage(plugin.help(prefix)).queue();
                        return;
                    }
                }
                channel.sendMessage("Couldn't find plugin!").queue();
            } else {
                channel.sendMessage(help()).queue();
            }
            return;
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

    private boolean notifyOnGuildMessageReceived(GuildMessageReceivedEvent event, String command, String param) {
        boolean found = false;

        for (GuildMessageReceivedPlugin guildMessageReceivedPlugin : guildMessageReceivedPlugins) {
            found = guildMessageReceivedPlugin.guildMessageReceived(event, command, param, prefix) || found;
        }

        return found;
    }

}
