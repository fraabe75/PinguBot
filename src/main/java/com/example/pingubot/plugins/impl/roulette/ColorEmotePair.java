package com.example.pingubot.plugins.impl.roulette;

import java.awt.*;

class ColorEmotePair {
    String emote;
    Color color;

    public ColorEmotePair(Color color, String emote) {
        this.emote = emote;
        this.color = color;
    }

    public String getEmote() {
        return emote;
    }

    public Color getColor() {
        return color;
    }
}
