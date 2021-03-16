package com.example.demo.plugins.impl.roulette;

import java.util.HashMap;
import java.util.Map;

class Number implements RouletteField {

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
