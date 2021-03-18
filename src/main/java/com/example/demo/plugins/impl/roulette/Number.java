package com.example.demo.plugins.impl.roulette;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

class Number implements RouletteField {

    // reference points for field position on printout board
    private int upperX, upperY, lowerX, lowerY;

    // first field reference point (= 1)
    private static final int refUpperX = 299, refUpperY = 103, refLowerX = 389, refLowerY = 162, border = 7;

    // Color type for every number field, where the index is the actual board field number
    protected static final Color[] COLOR_TABLE = {
            Color.GREEN, Color.RED, Color.BLACK, Color.RED, Color.BLACK, Color.RED, Color.BLACK, Color.RED,
            Color.BLACK, Color.RED, Color.BLACK, Color.BLACK, Color.RED, Color.BLACK, Color.RED,
            Color.BLACK, Color.RED, Color.BLACK, Color.RED, Color.RED, Color.BLACK, Color.RED, Color.BLACK,
            Color.RED, Color.BLACK, Color.RED, Color.BLACK, Color.RED, Color.BLACK, Color.BLACK, Color.RED,
            Color.BLACK, Color.RED, Color.BLACK, Color.RED, Color.BLACK, Color.RED
    };

    private final String emote;
    private final Map<Long, Long> currentBets;

    Number(String number) {
        this.emote = number;
        this.currentBets = new HashMap<>();
        fillPositionCoordinates();
    }

    private void fillPositionCoordinates() {
        int num_shift = Integer.parseInt(this.emote) - 1;
        if (num_shift == -1) {
            upperX = 299;
            upperY = 37;
            lowerX = 400;
            lowerY = 96;
        } else {
            int col = num_shift % 3;
            int row = num_shift / 3;

            int hor_rect_len = (refLowerX - refUpperX);
            int vert_rect_len = (refLowerY - refUpperY);

            upperX = refUpperX + col * (hor_rect_len + border);
            upperY = refUpperY + row * (vert_rect_len + border);
            lowerX = upperX + hor_rect_len;
            lowerY = upperY + vert_rect_len;
        }
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
