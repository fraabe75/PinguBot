package com.example.demo.plugins.impl.roulette;

import com.example.demo.database.entities.UserEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class RouletteBoard {

    // Color type for every number field, where the index is the actual board field number
    private final Color[] colorTable = {
            Color.GREEN, Color.RED, Color.BLACK, Color.RED, Color.BLACK, Color.RED, Color.BLACK, Color.RED,
            Color.BLACK, Color.RED, Color.BLACK, Color.BLACK, Color.RED, Color.BLACK, Color.RED,
            Color.BLACK, Color.RED, Color.BLACK, Color.RED, Color.RED, Color.BLACK, Color.RED, Color.BLACK,
            Color.RED, Color.BLACK, Color.RED, Color.BLACK, Color.RED, Color.BLACK, Color.BLACK, Color.RED,
            Color.BLACK, Color.RED, Color.BLACK, Color.RED, Color.BLACK, Color.RED
    };

    private final List<RouletteField> fields;
    private final Map<Long, UserEntity> players;
    private long updateMessage;
    private boolean finished;

    // time for users to submit bets in milliseconds
    private int betTime = 59000;

    public RouletteBoard(Consumer<RouletteBoard> resultConsumer) {
        fields = new ArrayList<>();
        updateMessage = Long.MIN_VALUE;
        players = new HashMap<>();

        for (int i = 0; i <= 36; i++) {
            fields.add(new Number(String.valueOf(i)));
        }
        fields.addAll(Arrays.asList(Field.values()));

        new Thread(() -> {
            while (betTime > 0) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(5000);
                    betTime -= 5000;
                } catch (InterruptedException e) {
                    System.err.println(e.toString());
                }
            }
            resultConsumer.accept(this);
        }).start();
    }

    public boolean isNewPlayer(long userID) {
        return !players.containsKey(userID);
    }

    public static List<MessageEmbed.Field> getFieldHelpFields() {
        List<MessageEmbed.Field> fieldInfos = new ArrayList<>();

        for (int i = 0; i < Field.values().length; i++) {
            Field field = Field.values()[i];
            fieldInfos.add(
                    new MessageEmbed.Field(
                            field.toString(),
                            field.getDescription() + ", " + (field.getPayout() - 1) + ":1", true)
            );
        }
        fieldInfos.add(new MessageEmbed.Field("0 - 36", "35:1", true));
        return fieldInfos;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public int getBetTimeRemaining() {
        return betTime;
    }

    public Color[] getColorTable() {
        return colorTable;
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
        userBets.forEach(
                (uID, rltFields) -> betBuilder.append(players.get(uID).getUserName())
                                              .append(": ")
                                              .append(rltFields.stream()
                                                               .map(rltField -> rltField.toString()
                                                                                + " ("
                                                                                + rltField.getCurrentBets().get(uID)
                                                                                + ")")
                                                               .collect(Collectors.joining(", ")))
        );

        if (betBuilder.toString().isBlank()) {
            if (betTime <= 0) {
                betBuilder.append("No fish was placed on the board!");
            } else {
                betBuilder.append("No bets, be the first to loose fish!");
            }
        }
        return new MessageEmbed.Field("Current Bets:", betBuilder.toString(), false);
    }

    public Map<Long, Long> addPayoutPerUser(EmbedBuilder builder, int rolledNumber) {
        Map<Long, Long> userPayoutMap = new HashMap<>();
        fields.forEach(field -> field.getCurrentBets().forEach(
                (uID, betAmount) -> userPayoutMap.merge(
                        uID,
                        -betAmount + (isPayoutField(field, rolledNumber) ? field.calculatePayout(betAmount) : 0),
                        Long::sum
                ))
        );
        userPayoutMap.forEach((uID, investReturn) ->
                builder.addField(players.get(uID).getUserName(), String.valueOf(investReturn), true));
        if (userPayoutMap.isEmpty()) {
            builder.setDescription("No fish was thrown on the board,\nso there won't be any payout!");
        }
        return userPayoutMap;
    }

    private boolean isPayoutField(RouletteField field, int rolledNumber) {
        if (!field.isSpecialField()) {
            return Integer.parseInt(field.toString()) == rolledNumber;
        }
        return switch ((Field) field) {
            case BLACK, RED -> colorTable[rolledNumber] == Color.getColor(((Field) field).name());
            case EVEN -> rolledNumber % 2 == 0;
            case ODD -> rolledNumber % 2 == 1;
            case COL_1 -> rolledNumber % 3 == 1;
            case COL_2 -> rolledNumber % 3 == 2;
            case COL_3 -> rolledNumber % 3 == 0 && rolledNumber != 0;
            case SQR_1 -> rolledNumber != 0 && rolledNumber <= 12;
            case SQR_2 -> rolledNumber >= 13 && rolledNumber <= 24;
            case SQR_3 -> rolledNumber >= 25;
            case LOW -> rolledNumber <= 18 && rolledNumber != 0;
            case HIGH -> rolledNumber >= 19;
        };
    }

    private interface RouletteField {
        boolean isThisField(String userInput);

        long calculatePayout(long userBet);

        void addBet(long userID, long amount);

        Map<Long, Long> getCurrentBets();

        boolean isSpecialField();

        String getDescription();
    }

    private enum Field implements RouletteField {
        BLACK(2, "black", "black tiles"),
        RED(2, "red", "red tiles"),

        EVEN(2, "pair", "even tiles"),
        ODD(2, "impair", "odd tiles"),

        COL_1(3, "col1", "left column"),
        COL_2(3, "col2", "middle column"),
        COL_3(3, "col3", "right column"),

        SQR_1(3, "p12", "1 - 12"),
        SQR_2(3, "m12", "13 - 24"),
        SQR_3(3, "d12", "25 - 36"),

        LOW(2, "manque", "1 - 18"),
        HIGH(2, "passe", "19 - 36");

        private final String emote;
        private final int payout;
        private final Map<Long, Long> currentBets;
        private final String description;

        Field(int payout, String emote, String description) {
            this.emote = emote;
            this.payout = payout;
            this.currentBets = new HashMap<>();
            this.description = description;
        }

        public int getPayout() {
            return payout;
        }

        @Override public boolean isThisField(String userInput) {
            return this.emote.equals(userInput);
        }

        @Override public long calculatePayout(long userBet) {
            return userBet * payout;
        }

        @Override public void addBet(long userID, long amount) {
            currentBets.merge(userID, amount, Long::sum);
        }

        @Override public String toString() {
            return emote;
        }

        @Override public Map<Long, Long> getCurrentBets() {
            return currentBets;
        }

        @Override public boolean isSpecialField() {
            return true;
        }

        @Override public String getDescription() {
            return description;
        }
    }

    private static class Number implements RouletteField {

        private final String emote;
        private final Map<Long, Long> currentBets;

        Number(String number) {
            this.emote = number;
            this.currentBets = new HashMap<>();
        }

        @Override public boolean isThisField(String userInput) {
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

        @Override public Map<Long, Long> getCurrentBets() {
            return currentBets;
        }

        @Override public boolean isSpecialField() {
            return false;
        }

        @Override public String getDescription() {
            return emote;
        }
    }
}
