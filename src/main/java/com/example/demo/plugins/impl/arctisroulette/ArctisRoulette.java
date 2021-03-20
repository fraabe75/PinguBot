package com.example.demo.plugins.impl.arctisroulette;

import com.example.demo.database.entities.UserEntity;
import com.example.demo.database.repositories.UserRepository;
import com.example.demo.plugins.GuildMessageReactionAddPlugin;
import com.example.demo.plugins.GuildMessageReceivedPlugin;
import com.example.demo.plugins.Plugin;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Service
public class ArctisRoulette extends Plugin implements GuildMessageReceivedPlugin, GuildMessageReactionAddPlugin {

    private final UserRepository userRepository;
    private final List<ArctisRouletteSession> arcticGames;

    public ArctisRoulette(UserRepository userRepository) {
        setName("Arctis Roulette");
        setDescription("The famous gambling game's evil twin");
        addCommands("arctis", "ar", "arc");

        this.userRepository = userRepository;
        this.arcticGames = new LinkedList<>();
    }

    public static MessageEmbed help() {
        return ArctisRouletteSession.help();
    }

    @Override
    public boolean guildMessageReceived(GuildMessageReceivedEvent event, String command, String param, String prefix) {
        if (!commands().contains(command)) {
            return false;
        }

        final TextChannel channel = event.getChannel();

        if (param.matches("^(\\d+|all)$")) {
            if (arcticGames.stream()
                           .filter(aG -> !aG.isWaiting())
                           .anyMatch(aG -> aG.getMessage().getChannel().equals(event.getChannel()))
            ) {
                channel.sendMessage("Sorry, but one of these games per channel is already too much. " +
                                    "(limit reached)").queue();
            } else {
                Optional<ArctisRouletteSession> alreadyWaiting;
                if ((alreadyWaiting = arcticGames.stream()
                                                 .filter(ArctisRouletteSession::isWaiting)
                                                 .filter(m -> m.getMessage().getChannel()
                                                               .equals(event.getChannel()))
                                                 .findAny()).isPresent()
                ) {
                    arcticGames.remove(alreadyWaiting.get());
                    alreadyWaiting.get().getMessage().delete().complete();
                }
                UserEntity startingEntity = UserEntity.getUserByIdLong(
                        event.getMember(),
                        event.getAuthor(),
                        userRepository
                );
                long betAmount;
                if (!param.equals("all")) {
                    try {
                        betAmount = Long.parseUnsignedLong(param);
                    } catch (NumberFormatException e) {
                        channel.sendMessage("Invalid bet! Trying to overflow our bet system, ha?").queue();
                        return true;
                    }
                    if (betAmount <= 0 || betAmount > startingEntity.getFish()) {
                        channel.sendMessage(
                                "Invalid bet! Either you try to bet unnatural amounts of fish or " +
                                            "you are just broke. Probably even both?"
                        ).queue();
                        return true;
                    }
                } else {
                    betAmount = startingEntity.getFish();
                }
                arcticGames.add(new ArctisRouletteSession(userRepository, channel, event.getMessage(), betAmount));
            }
        } else {
            channel.sendMessage(help()).queue();
        }
        return true;
    }

    @Override
    public boolean guildMessageReactionAdd(GuildMessageReactionAddEvent event, String messageId) {
        if (!event.getUser().isBot()) {
            Optional<ArctisRouletteSession> eventGameOption = arcticGames.stream()
                                                                         .filter(aR -> aR.getMessage().getId()
                                                                                         .equals(messageId))
                                                                         .findAny();
            if (eventGameOption.isPresent()) {
                if (eventGameOption.get().reactionAdded(event)) {
                    arcticGames.remove(eventGameOption.get());
                    event.getChannel().sendMessage("Thanks for playing. In case you are unhappy with the result, " +
                                                   "please consider writing a letter to our postal address.").queue();
                }
                return true;
            }
        }
        return false;
    }
}
