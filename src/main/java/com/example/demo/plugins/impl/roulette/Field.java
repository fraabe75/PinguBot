package com.example.demo.plugins.impl.roulette;

import java.util.HashMap;
import java.util.Map;

enum Field implements RouletteField {
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
