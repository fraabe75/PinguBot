package com.example.demo.plugins.impl.roulette;

import java.awt.*;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

interface RouletteField {

    boolean isThisField(String userInput);

    long calculatePayout(long userBet);

    void addBet(long userID, long amount);

    Map<Long, Long> getCurrentBets();

    boolean isSpecialField();

    String getDescription();

    int getLowerX();

    int getUpperX();

    int getLowerY();

    int getUpperY();

    default void drawPlayerPieces(Graphics g, Map<Long, UserColorEmoteTriple> colorMapping, double scale, int offset) {
        int player_square_area =
                (int) (((getLowerX() - getUpperX()) * (getLowerY() - getUpperY())) / 8 * scale) - 2 * offset;
        int square_len = (int) ((getLowerX() - getUpperX()) / 3 * scale);
        int circle_rect_side_length = (int) Math.sqrt(player_square_area / 3.0);

        int startX = (int) (getUpperX() * scale + offset);
        int startY = (int) (getUpperY() * scale + offset);

        AtomicInteger col = new AtomicInteger(0);
        AtomicInteger row = new AtomicInteger(0);

        getCurrentBets().keySet()
                        .stream()
                        .filter(aLong -> colorMapping.get(aLong).getColor() != null)
                        .map(aLong -> colorMapping.get(aLong).getColor())
                        .sorted(Comparator.comparing(Color::toString))
                        .forEachOrdered(
                                color -> {
                                    if (col.get() == 3) {
                                        col.set(0);
                                        row.incrementAndGet();
                                    }
                                    g.setColor(color);
                                    g.fillOval(
                                            (col.getAndIncrement() * square_len) + startX,
                                            (row.get() * square_len) + startY,
                                            circle_rect_side_length,
                                            circle_rect_side_length
                                    );
                                }
                        );
    }
}
