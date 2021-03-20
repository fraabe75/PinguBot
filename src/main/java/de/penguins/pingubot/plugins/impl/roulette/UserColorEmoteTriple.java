package de.penguins.pingubot.plugins.impl.roulette;

import de.penguins.pingubot.database.entities.UserEntity;

import java.awt.*;

class UserColorEmoteTriple {
    UserEntity user;
    Color color;
    String emote;

    public UserColorEmoteTriple(UserEntity user, Color color, String emote) {
        this.user = user;
        this.color = color;
        this.emote = emote;
    }

    public UserEntity getUser() {
        return user;
    }

    public Color getColor() {
        return color;
    }

    public String getEmote() {
        return emote;
    }
}
