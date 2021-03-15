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

import java.util.*;
import java.util.stream.Collectors;

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
                        } else {
                            printOrUpdateBoard(board, channel);
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

    private static class RouletteBoard {

        Color[] colorTable = {
                Color.GREEN, Color.RED, Color.BLACK, Color.RED, Color.BLACK, Color.RED, Color.BLACK, Color.RED,
                Color.BLACK, Color.RED, Color.BLACK, Color.BLACK, Color.RED, Color.BLACK, Color.RED,
                Color.BLACK, Color.RED, Color.BLACK, Color.RED, Color.RED, Color.BLACK, Color.RED, Color.BLACK,
                Color.RED, Color.BLACK, Color.RED, Color.BLACK, Color.RED, Color.BLACK, Color.BLACK, Color.RED,
                Color.BLACK, Color.RED, Color.BLACK, Color.RED, Color.BLACK, Color.RED
        };

        List<RouletteField> fields;
        Map<Long, UserEntity> players;
        long updateMessage;
        boolean finished;

        public RouletteBoard() {
            fields = new ArrayList<>();
            updateMessage = Long.MIN_VALUE;
            players = new HashMap<>();

            for (int i = 0; i <= 36; i++) {
                fields.add(new Number(String.valueOf(i), colorTable[i]));
            }
            fields.addAll(Arrays.asList(Field.values()));
        }

        public boolean isNewPlayer(long userID) {
            return players.containsKey(userID);
        }

        public List<MessageEmbed.Field> getFieldHelpFields() {
            List<MessageEmbed.Field> fieldInfos = new ArrayList<>();

            for (Field field : Field.values()) {
                fieldInfos.add(new MessageEmbed.Field(field.toString(), (field.payout - 1) + ":1", true));
            }
            fieldInfos.add(new MessageEmbed.Field("0 - 36", "36:1", true));
            return fieldInfos;
        }

        public boolean isFinished() {
            return finished;
        }

        public Long getUpdateMessage() {
            return updateMessage;
        }

        public void setUpdateMessage(Long updateMessage) {
            this.updateMessage = updateMessage;
        }

        public void addPlayer(UserEntity player) {
            players.put(player.getUserId(), player);
        }

        public boolean addBet(long userID, long amount, String fieldSelect) {
            for (RouletteField field : fields) {
                if (field.isThisField(fieldSelect.strip())) {
                    field.addBet(userID, amount);
                    return true;
                }
            }
            return false;
        }

        public MessageEmbed.Field getBetsRepr() {
            StringBuilder betBuilder = new StringBuilder();
            Map<Long, List<RouletteField>> userBets = new HashMap<>();

            for (RouletteField field : fields) {
                field.getCurrentBets().forEach(
                        (uID, bet) -> {
                            if (userBets.containsKey(uID)) {
                                userBets.get(uID).add(field);
                            } else {
                                List<RouletteField> newList = new ArrayList<>();
                                newList.add(field);
                                userBets.put(uID, newList);
                            }
                        }
                );
            }

            userBets.forEach((uID, rltFields) -> betBuilder.append(players.get(uID).getUserName())
                                                           .append(": ")
                                                           .append(
                                                                   rltFields.stream().map(Object::toString)
                                                                            .collect(Collectors.joining(", "))
                                                           ));

            if (betBuilder.toString().isBlank()) {
                betBuilder.append("No Bets, be the first to loose fish!");
            }
            return new MessageEmbed.Field("Current Bets:", betBuilder.toString(), false);
        }

        public String getBoardImg() {
            return "https://upload.wikimedia.org/wikipedia/commons/" +
                   "thumb/4/47/Roulette_pfad.svg/881px-Roulette_pfad.svg.png";
        }

        private interface RouletteField {
            boolean isThisField(String userInput);

            long calculatePayout(long userBet);

            void addBet(long userID, long amount);

            Map<Long, Long> getCurrentBets();
        }

        private enum Field implements RouletteField {
            BLACK(2, "black"), RED(2, "red"),
            EVEN(2, "pair"), ODD(2, "impair"), COL_1(3, "col1"),
            COL_2(3, "col2"), COL_3(3, "col3"), SQR_1(3, "p12"),
            SQR_2(3, "m12"), SQR_3(3, "d12"), LOW(2, "manque"),
            HIGH(2, "passe");

            private final String emote;
            private final int payout;
            private final Map<Long, Long> currentBets;

            Field(int payout, String emote) {
                this.emote = emote;
                this.payout = payout;
                this.currentBets = new HashMap<>();
            }

            public boolean isThisField(String userInput) {
                return this.emote.equals(userInput);
            }

            @Override public long calculatePayout(long userBet) {
                return userBet * payout;
            }

            @Override public void addBet(long userID, long amount) {
                currentBets.put(userID, amount);
            }

            @Override public String toString() {
                return emote;
            }

            public Map<Long, Long> getCurrentBets() {
                return currentBets;
            }
        }

        private class Number implements RouletteField {

            private final String emote;
            private final Map<Long, Long> currentBets;
            private final Color col;

            Number(String number, Color col) {
                this.emote = number;
                this.col = col;
                this.currentBets = new HashMap<>();
            }

            public boolean isThisField(String userInput) {
                return this.emote.equals(userInput);
            }

            @Override public long calculatePayout(long userBet) {
                return 36 * userBet;
            }

            @Override public String toString() {
                return emote;
            }

            @Override public void addBet(long userID, long amount) {
                currentBets.put(userID, amount);
            }

            public Map<Long, Long> getCurrentBets() {
                return currentBets;
            }
        }

        private enum Color {
            BLACK, RED, GREEN
        }
    }
}
