package com.example.demo.core;

import com.example.demo.plugins.GuildMessageReactionAddPlugin;
import com.example.demo.plugins.GuildMessageReceivedPlugin;
import com.example.demo.plugins.Plugin;
import com.example.demo.plugins.impl.UserManager;
import com.example.demo.plugins.impl.blackjack.Blackjack;
import com.example.demo.plugins.impl.roulette.Roulette;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
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

        String[] args = message.substring(prefix.length()).trim().split(" ", 3);
        args = new String[]{
                args[0],
                args.length < 2 ? "" : args[1],
                args.length < 3 ? "" : args[2]
        };

        if (args[0].equals("help") || args[0].equals("")) {
            event.getChannel().sendMessage(
                    switch (args[1]) {
                        case "usermanager", "user", "u" -> UserManager.help();
                        case "roulette", "rlt", "r" -> Roulette.help();
                        case "blackjack", "bj", "b" -> Blackjack.help();
                        default -> {
                            EmbedBuilder builder = new EmbedBuilder();
                            builder.setTitle("Help! You need somebody?");
                            builder.setDescription("Not just anybody?");
                            for (Plugin plugin : plugins) {
                                if (!plugin.commands().contains("naughty"))
                                    builder.addField(
                                            new MessageEmbed.Field(plugin.getName(), plugin.getDescription(), false)
                                    );
                            }
                            builder.setFooter("For detailed help messages:\n'" + prefix + " help <command>'");
                            yield builder.build();
                        }
                    }
            ).queue();
            return;
        }

        String command, param;
        switch (args[0]) {
            case "bp", "pb", "rp", "pr", "eb", "be" -> {
                args[0] = args[0].substring(0, 1);
                args[1] = args[0].substring(1);
            }

        }
        switch (args[0]) {
            case "start", "play", "new", "game", "p", "end", "termninate", "e" -> {
                command = args[1];
                param = (args[0] + " " + args[2]).trim();
            }
            default -> {
                command = args[0];
                param = (args[1] + " " + args[2]).trim();
            }
        }
        for (GuildMessageReceivedPlugin guildMessageReceivedPlugin : guildMessageReceivedPlugins) {
            if (((Plugin) guildMessageReceivedPlugin).commands().contains(command)) {
                if (!guildMessageReceivedPlugin.guildMessageReceived(event, command, param, prefix)) {
                    channel.sendMessage("Couldn't find command! Try 'dp! help'").queue();
                }
                return;
            }
        }
        channel.sendMessage("Couldn't find command in any plugins!").queue();
    }
}
