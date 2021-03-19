package com.example.demo.plugins.impl.baccarat;

public enum PlayerBet {

    Player("Player", 2),
    Bank("Bank", 1.95),
    Tie("Tie", 8);


    private final String TYPE;
    private final double RATE;


    PlayerBet(String type, double rate) {
        this.TYPE = type;
        this.RATE = rate;
    }

    public String getTYPE() {
        return TYPE;
    }

    public double getRATE() {
        return RATE;
    }
}
