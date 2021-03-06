package de.penguins.pingubot.plugins.impl.roulette;

import de.penguins.pingubot.database.entities.UserEntity;
import de.penguins.pingubot.database.repositories.UserRepository;
import de.penguins.pingubot.plugins.GuildMessageReceivedPlugin;
import de.penguins.pingubot.plugins.Plugin;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class Roulette extends Plugin implements GuildMessageReceivedPlugin {

    private final UserRepository userRepository;
    private RouletteBoard board;
    private Long lastErrorMessageID;

    public Roulette(UserRepository userRepository) {
        setName("Roulette");
        setDescription("Play your favorite, not at all random,\ngambling game!");
        addCommands("roulette", "rlt", "r", "rtl", "rl");

        this.userRepository = userRepository;
    }

    public static MessageEmbed help() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Roulette Help");
        builder.setDescription("They see me rollin'");
        builder.addField(
                "roulette play",
                "start a new game",
                false
        );
        builder.addField(
                "roulette bet <field> <value>",
                "place a bet (fish) on a field",
                false
        );
        builder.addField(
                "roulette fields",
                "display fields and payout rates",
                false
        );
        builder.setFooter("Shortcuts: 'rlt', 'r'");
        return builder.build();
    }

    public MessageEmbed helpFields() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Roulette Help");

        for (MessageEmbed.Field field : RouletteBoard.getFieldHelpFields()) {
            builder.addField(field);
        }
        return builder.build();
    }

    @Override
    public boolean guildMessageReceived(GuildMessageReceivedEvent event, String command, String param, String prefix) {
        if (!commands().contains(command)) {
            return false;
        }

        final TextChannel channel = event.getChannel();

        if (lastErrorMessageID != null) {
            channel.deleteMessageById(lastErrorMessageID).queue(m -> {
            }, e -> {
            });
            lastErrorMessageID = null;
        }

        switch (param.trim().split(" ")[0]) {
            case "start", "play", "new", "game", "p" -> {
                if (board == null || board.isFinished()) {
                    board = new RouletteBoard((b) -> printOrUpdateBoard(b, channel, () -> {
                        int rolledNumber = new Random().nextInt(37);
                        channel.sendMessage(
                                new EmbedBuilder().setTitle("Rolled Number: " + rolledNumber)
                                        .setColor(Number.COLOR_TABLE[rolledNumber])
                                        .build()
                        ).queue();

                        EmbedBuilder builder = new EmbedBuilder();
                        builder.setTitle("Payout");
                        b.addPayoutPerUser(builder, rolledNumber)
                                .forEach((key, value) -> {
                                    UserEntity author = userRepository.getOne(key);
                                    if (value <= 0) {
                                        author.subMateability(1);
                                    } else {
                                        author.addFish(value);
                                        if (value < 30) {
                                            author.addMateability(1);
                                        } else if (value < 100) {
                                            author.addMateability(2);
                                        } else {
                                            author.addMateability(3);
                                        }
                                    }
                                    userRepository.save(author);
                                });
                        userRepository.flush();
                        b.setFinished();
                        channel.sendMessage(builder.build()).queue();
                    }));
                } else {
                    channel.sendMessage("There is already a game in progress.")
                            .queue(m -> lastErrorMessageID = m.getIdLong());
                }
                printOrUpdateBoard(board, channel, () -> {
                });
            }
            case "bet", "set", "place", "b" -> {
                param = param.replace("bet", "")
                        .replace("set", "")
                        .replace("place", "")
                        .trim();

                if (board == null || board.isFinished() || board.getBetTimeRemaining() <= 0) {
                    channel.sendMessage(
                            "No game in progress, start a new one with '"
                                    + prefix
                                    + " rlt play'"
                    ).queue(m -> lastErrorMessageID = m.getIdLong());
                } else if (!param.matches("^[\\d\\w]+ (\\d+|all)$")) {
                    channel.sendMessage("Bot does not compute (invalid bet format)\n" +
                            "Better try this: '" + prefix + " rlt bet <field> <amount>'")
                            .queue(m -> lastErrorMessageID = m.getIdLong());
                } else {
                    long authorID = event.getAuthor().getIdLong();
                    UserEntity author;

                    if (board.isNewPlayer(authorID)) {
                        if (!userRepository.existsById(authorID)) {
                            userRepository.saveAndFlush(
                                    new UserEntity(authorID, event.getAuthor().getName().toLowerCase())
                            );
                        }
                        author = UserEntity.getUserByIdLong(
                                event.getMember(),
                                event.getAuthor(),
                                userRepository
                        );
                        if (!board.addPlayer(author)) {
                            channel.sendMessage("Reached player limit of " +
                                    board.maxColorPlayers() +
                                    " players \n" +
                                    "which can be displayed on the board")
                                    .queue(m -> lastErrorMessageID = m.getIdLong());
                        }
                    } else {
                        author = UserEntity.getUserByIdLong(
                                event.getMember(),
                                event.getAuthor(),
                                userRepository
                        );
                    }
                    String betField = param.trim().split(" ")[0];
                    long betAmount;
                    if (param.trim().split(" ")[1].equals("all")) {
                        betAmount = author.getFish();
                    } else {
                        try {
                            betAmount = Long.parseUnsignedLong(param.trim().split(" ")[1]);
                        } catch (NumberFormatException e) {
                            channel.sendMessage("Invalid bet! (Trying to overflow our bet system, he?)")
                                    .queue(m -> lastErrorMessageID = m.getIdLong());
                            break;
                        }
                    }
                    if (author.getFish() < betAmount) {
                        channel.sendMessage("We looked everywhere, "
                                + author.getUserName()
                                + ", but we "
                                + "couldn't find any more fish in your bank.\nYou are officially bankrupt.")
                                .queue(m -> lastErrorMessageID = m.getIdLong());
                    } else if (betAmount <= 0) {
                        channel.sendMessage("Sorry, no bets <= 0 fish accepted.")
                                .queue(m -> lastErrorMessageID = m.getIdLong());
                    } else {
                        if (!board.addBet(authorID, betAmount, betField)) {
                            EmbedBuilder builder = new EmbedBuilder();
                            builder.setTitle("Beep Boop");
                            builder.setDescription("""
                                    It seems like you entered a wrong field
                                    (do you even know how to read help pages?)

                                    Available fields and their payouts:

                                    """);
                            for (MessageEmbed.Field field : RouletteBoard.getFieldHelpFields()) {
                                builder.addField(field);
                            }
                            channel.sendMessage(builder.build()).queue(m -> lastErrorMessageID = m.getIdLong());
                        } else {
                            author.subFish(betAmount);
                            userRepository.saveAndFlush(author);
                            printOrUpdateBoard(board, channel, () -> {
                            });
                        }
                    }
                }
            }
            case "fields", "field", "rules" -> channel.sendMessage(helpFields()).queue(m -> lastErrorMessageID = null);
            default -> channel.sendMessage(help()).queue(m -> lastErrorMessageID = null);
        }
        event.getMessage().delete().queueAfter(5, TimeUnit.SECONDS);

        return true;
    }

    private void printOrUpdateBoard(RouletteBoard b, TextChannel ch, Runnable downstream) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setImage("attachment://" + BoardImageGenerator.getImageFileName());
        builder.addField(b.getBetsRepr());

        int timeRemaining = b.getBetTimeRemaining();

        // add timeout information to footer
        if (timeRemaining <= 60000 && timeRemaining > 0) {
            builder.setThumbnail("https://media.giphy.com/media/26uf2YTgF5upXUTm0/source.gif");

            builder.setFooter(
                    "s bet time remaining!",
                    "https://media.giphy.com/media/Uc12D9PJloR40ALxwQ/source.gif"
            );
        }

        if (timeRemaining <= 0) {
            builder.setFooter("rien ne va plus!");
        }

        if (b.getUpdateMessage() < 0) {
            ch.sendMessage(builder.build())
                    .addFile(BoardImageGenerator.getImageFile(board), BoardImageGenerator.getImageFileName())
                    .queue(m -> {
                        b.setUpdateMessage(m.getIdLong());
                        downstream.run();
                    });
        } else {
            ch.retrieveMessageById(b.getUpdateMessage())
                    .queue(oldMessage -> oldMessage.delete().queue(
                            oldMessageDelete -> ch.sendMessage(builder.build())
                                    .addFile(BoardImageGenerator.getImageFile(board),
                                            BoardImageGenerator.getImageFileName())
                                    .queue(m -> {
                                        b.setUpdateMessage(m.getIdLong());
                                        downstream.run();
                                    })
                            )
                    );
        }
    }
}
