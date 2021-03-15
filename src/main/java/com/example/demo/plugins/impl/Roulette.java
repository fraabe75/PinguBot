package com.example.demo.plugins.impl;

import com.example.demo.database.entities.UserEntity;
import com.example.demo.database.repositories.UserRepository;
import com.example.demo.plugins.GuildMessageReceivedPlugin;
import com.example.demo.plugins.Plugin;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class Roulette extends Plugin implements GuildMessageReceivedPlugin {

    private final UserRepository userRepository;
    private RouletteBoard board;
    private Long lastErrorMessageID;

    public Roulette(UserRepository userRepository) {
        setName("Roulette");
        setDescription("Play your favorite, not at all random,\ngambling game!");
        addCommands("rlt", "roulette", "r", "rtl");

        this.userRepository = userRepository;
    }

    public static MessageEmbed help() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Roulette Help");
        builder.setDescription("They see me rollin'");
        builder.addField(
                "How to play:",
                """
                Lorem Ipsum
                """,
                false
        );
        return builder.build();
    }

    @Override
    public boolean guildMessageReceived(GuildMessageReceivedEvent event, String command, String param, String prefix) {
        if (!commands().contains(command)) {
            return false;
        }

        final TextChannel channel = event.getChannel();

        if (lastErrorMessageID != null) {
            channel.deleteMessageById(lastErrorMessageID).queue();
            lastErrorMessageID = null;
        }

        switch (param.trim().split(" ")[0]) {
            case "start", "play", "new", "game" -> {
                if (board == null || board.isFinished()) {
                    board = new RouletteBoard();
                } else {
                    channel.sendMessage("There is already a game in progress.")
                           .queue(m -> lastErrorMessageID = m.getIdLong());
                }
                printOrUpdateBoard(board, channel);
                event.getMessage().delete().queue();
            }
            case "bet", "set" -> {
                param = param.replace("bet", "").replace("set", "").trim();
                event.getMessage().delete().queue();

                if (board == null || board.isFinished()) {
                    channel.sendMessage(
                            "No game in progress, start a new one with `"
                            + prefix
                            + " rlt play`"
                    ).queue(m -> lastErrorMessageID = m.getIdLong());
                } else if (!param.matches("^[\\d\\w]+ \\d+$")) {
                    channel.sendMessage("Bot does not compute (invalid bet format)\n" +
                                                   "Better try this: `" + prefix + " rlt bet <field> <amount>`")
                         .queue(m -> lastErrorMessageID = m.getIdLong());
                } else {
                    long authorID = event.getAuthor().getIdLong();
                    UserEntity author;

                    if (board.isNewPlayer(authorID)) {
                        if (!userRepository.existsById(authorID)) {
                            userRepository
                                    .saveAndFlush(new UserEntity(authorID, event.getAuthor().getName().toLowerCase()));
                        }
                        author = userRepository.getOne(authorID);
                        board.addPlayer(author);
                    } else {
                        author = userRepository.getOne(authorID);
                    }
                    String betField = param.split(" ")[0];
                    long betAmount = Long.parseUnsignedLong(param.split(" ")[1]);
                    if (author.getFish() < betAmount) {
                        channel.sendMessage("We looked every, "
                                          + author.getUserName()
                                          + ", but we "
                                          + "couldn't find any more fish in your bank.\nYou are officially bankrupt.")
                             .queue(m -> lastErrorMessageID = m.getIdLong());
                    } else {
                        if (!board.addBet(authorID, betAmount, betField)) {
                            EmbedBuilder builder = new EmbedBuilder();
                            builder.setTitle("Beep Boop");
                            builder.setDescription("It seems like you entered a wrong field\n" +
                                                   "(do you even know how to " +
                                                   "read help pages?)\n\nAvailable fields and their payouts:\n\n");
                            for (MessageEmbed.Field field : board.getFieldHelpFields()) {
                                builder.addField(field);
                            }
                            channel.sendMessage(builder.build()).queue(m -> lastErrorMessageID = m.getIdLong());
                        }
                    }
                }
            }
            default -> {
                channel.sendMessage(help()).queue(m -> lastErrorMessageID = m.getIdLong());
            }
        }

        return true;
    }

    private void printOrUpdateBoard(RouletteBoard b, TextChannel ch) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setImage(b.getBoardImg());
        builder.addField(b.getBetsRepr());

        if (b.getUpdateMessage() < 0) {
            ch.sendMessage(builder.build()).queue(m -> b.setUpdateMessage(m.getIdLong()));
        }
        else {
            ch.getHistoryAfter(b.getUpdateMessage(), 6).queue(
                    mh -> {
                        if (mh.size() > 5) {
                            ch.deleteMessageById(b.getUpdateMessage()).queue();
                            ch.sendMessage(builder.build()).queue(m -> b.setUpdateMessage(m.getIdLong()));
                        } else {
                            ch.editMessageById(b.getUpdateMessage(), builder.build()).queue();
                        }
                    }
            );
        }
    }
}
