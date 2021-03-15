package com.example.demo.core;

import com.example.demo.plugins.GuildMessageReactionAddPlugin;
import com.example.demo.plugins.GuildMessageReceivedPlugin;
import com.example.demo.plugins.Plugin;
import com.example.demo.plugins.impl.UserManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
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
    private final List<GuildMessageReactionAddPlugin> guildMessageReactionAddPlugins;

    public MainListener(List<Plugin> plugins, List<GuildMessageReceivedPlugin> guildMessageReceivedPlugins,
                        List<GuildMessageReactionAddPlugin> guildMessageReactionAddPlugins) {
        this.plugins = plugins;
        this.guildMessageReceivedPlugins = guildMessageReceivedPlugins;
        this.guildMessageReactionAddPlugins = guildMessageReactionAddPlugins;
    }

    @Override
    public void onGuildMessageReactionAdd(@NotNull GuildMessageReactionAddEvent event) {
        for (GuildMessageReactionAddPlugin guildMessageReactionAddPlugin : guildMessageReactionAddPlugins) {
            guildMessageReactionAddPlugin.guildMessageReactionAdd(event, event.getMessageId());
        }
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {

        if (event.getAuthor().isBot()) {
            return;
        }

        TextChannel channel = event.getChannel();

        String message = event.getMessage().getContentRaw().toLowerCase();

        if (!message.startsWith(prefix)) {
            return;
        }

        String[] args = message.substring(prefix.length()).trim().split(" ", 2);
        args = new String[]{
                args[0],
                args.length < 2 ? "" : args[1]
        };

        if (args[0].equals("help")) {
            event.getChannel().sendMessage(
                    switch (args[1]) {
                        case "users", "user", "usermanager" -> UserManager.help();
                        default -> {
                            EmbedBuilder builder = new EmbedBuilder();
                            builder.setTitle("Help! You need somebody?");
                            builder.setDescription("Not just anybody?");
                            for (Plugin plugin : plugins) {
                                if(!plugin.commands().contains("naughty"))
                                builder.addField(
                                        new MessageEmbed.Field(plugin.getName(), plugin.getDescription(), false)
                                );
                            }
                            builder.setFooter("For detailed help messages:\n" + prefix + " help <command>");
                            yield builder.build();
                        }
                    }
            ).queue();
            return;
        }

        for (GuildMessageReceivedPlugin guildMessageReceivedPlugin : guildMessageReceivedPlugins) {
            if (((Plugin) guildMessageReceivedPlugin).commands().contains(args[0])) {
                if (!guildMessageReceivedPlugin.guildMessageReceived(event, args[0], args[1], prefix)) {
                    channel.sendMessage("Couldn't find command").queue();
                }
                return;
            }
        }
        channel.sendMessage("Couldn't find command in any plugins").queue();
    }
}
