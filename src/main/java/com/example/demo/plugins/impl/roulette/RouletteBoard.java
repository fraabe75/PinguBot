package com.example.demo.plugins.impl.roulette;

import com.example.demo.database.entities.UserEntity;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class RouletteBoard {

    // Color type for every number field, where the index is the actual board field number
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
    int timeRemaining;

    public RouletteBoard(Consumer<RouletteBoard> resultConsumer) {
        fields = new ArrayList<>();
        updateMessage = Long.MIN_VALUE;
        players = new HashMap<>();
        timeRemaining = 30;

        for (int i = 0; i <= 36; i++) {
            fields.add(new Number(String.valueOf(i), colorTable[i]));
        }
        fields.addAll(Arrays.asList(Field.values()));
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                if (timeRemaining <= 0) {
                    resultConsumer.accept(this);
                    return;
                }
                timeRemaining--;
                Thread.yield();
                try {
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    System.err.println(e.toString());
                }
            }
        }).start();
    }

    public boolean isNewPlayer(long userID) {
        return !players.containsKey(userID);
    }

    public List<MessageEmbed.Field> getFieldHelpFields() {
        List<MessageEmbed.Field> fieldInfos = new ArrayList<>();

        for (Field field : Field.values()) {
            fieldInfos.add(new MessageEmbed.Field(field.toString(), (field.getPayout() - 1) + ":1", true));
        }
        fieldInfos.add(new MessageEmbed.Field("0 - 36", "36:1", true));
        return fieldInfos;
    }

    public boolean isFinished() {
        return finished;
    }

    public int getTimeRemaining() {
        return timeRemaining;
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
                                                               .map(Object::toString)
                                                               .collect(Collectors.joining(", ")))
        );

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

        public String getEmote() {
            return emote;
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
            currentBets.put(userID, amount);
        }

        @Override public String toString() {
            return emote;
        }

        @Override public Map<Long, Long> getCurrentBets() {
            return currentBets;
        }
    }

    private static class Number implements RouletteField {

        private final String emote;
        private final Map<Long, Long> currentBets;
        private final Color col;

        Number(String number, Color col) {
            this.emote = number;
            this.col = col;
            this.currentBets = new HashMap<>();
        }

        public String getEmote() {
            return emote;
        }

        public Color getCol() {
            return col;
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
    }

    private enum Color {
        BLACK, RED, GREEN
    }
}
