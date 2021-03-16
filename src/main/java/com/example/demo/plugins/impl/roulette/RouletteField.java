package com.example.demo.plugins.impl.roulette;

import java.util.Map;

interface RouletteField {
    boolean isThisField(String userInput);

    long calculatePayout(long userBet);

    void addBet(long userID, long amount);

    Map<Long, Long> getCurrentBets();

    boolean isSpecialField();

    String getDescription();
}
