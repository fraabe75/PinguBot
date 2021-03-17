package com.example.demo.plugins.impl.roulette;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

enum Field implements RouletteField {
    BLACK(2, "black", "black tiles", 80, 630, 290, 850),
    RED(2, "red", "red tiles", 590, 630, 800, 850),

    EVEN(2, "pair", "even tiles", 80, 370, 290, 625),
    ODD(2, "impair", "odd tiles", 590, 370, 800, 625),

    COL_1(3, "col1", "left column", 300, 900, 390, 960),
    COL_2(3, "col2", "middle column", 395, 900, 487, 958),
    COL_3(3, "col3", "right column", 492, 898, 583, 958),

    SQR_1(3, "p12", "1 - 12", 40, 860, 121, 909),
    SQR_2(3, "m12", "13 - 24", 125, 875, 206, 928),
    SQR_3(3, "d12", "25 - 36", 211, 892, 294, 946),

    LOW(2, "manque", "1 - 18", 590, 142, 800, 360),
    HIGH(2, "passe", "19 - 36", 80, 150, 290, 360);

    private final String emote;
    private final int payout;
    private final Map<Long, Long> currentBets;
    private final String description;

    // reference points for field position on printout board
    private final int upperX, upperY, lowerX, lowerY;

    Field(int payout, String emote, String description, int upperX, int upperY, int lowerX, int lowerY) {
        this.emote = emote;
        this.payout = payout;
        this.currentBets = new HashMap<>();
        this.description = description;
        this.upperX = upperX;
        this.upperY = upperY;
        this.lowerX = lowerX;
        this.lowerY = lowerY;
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

    public void clear() {
        currentBets.clear();
    }

    @Override public String toString() {
        return emote;
    }

    @Override public boolean isSpecialField() {
        return true;
    }

    @Override public String getDescription() {
        return description;
    }

    @Override public Map<Long, Long> getCurrentBets() {
        return currentBets;
    }

    public int getPayout() {
        return payout;
    }

    @Override public int getUpperX() {
        return upperX;
    }

    @Override public int getUpperY() {
        return upperY;
    }

    @Override public int getLowerX() {
        return lowerX;
    }

    @Override public int getLowerY() {
        return lowerY;
    }
}
