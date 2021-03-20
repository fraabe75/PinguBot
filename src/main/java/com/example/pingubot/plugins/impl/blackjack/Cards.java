package com.example.pingubot.plugins.impl.blackjack;

public enum Cards {

    C2("2 of Clubs", "clubs", 2, ""),
    S2("2 of Spades", "spades", 2, ""),
    H2("2 of Hearts", "hearts", 2, ""),
    D2("2 of Diamonds", "diamonds", 2, ""),
    C3("3 of Clubs", "clubs", 3, ""),
    S3("3 of Spades", "spades", 3, ""),
    H3("3 of Hearts", "hearts", 3, ""),
    D3("3 of Diamonds", "diamonds", 3, ""),
    C4("4 of Clubs", "clubs", 4, ""),
    S4("4 of Spades", "spades", 4, ""),
    H4("4 of Hearts", "hearts", 4, ""),
    D4("4 of Diamonds", "diamonds", 4, ""),
    C5("5 of Clubs", "clubs", 5, ""),
    S5("5 of Spades", "spades", 5, ""),
    H5("5 of Hearts", "hearts", 5, ""),
    D5("5 of Diamonds", "diamonds", 5, ""),
    C6("6 of Clubs", "clubs", 6, ""),
    S6("6 of Spades", "spades", 6, ""),
    H6("6 of Hearts", "hearts", 6, ""),
    D6("6 of Diamonds", "diamonds", 6, ""),
    C7("7 of Clubs", "clubs", 7, ""),
    S7("7 of Spades", "spades", 7, ""),
    H7("7 of Hearts", "hearts", 7, ""),
    D7("7 of Diamonds", "diamonds", 7, ""),
    C8("8 of Clubs", "clubs", 8, ""),
    S8("8 of Spades", "spades", 8, ""),
    H8("8 of Hearts", "hearts", 8, ""),
    D8("8 of Diamonds", "diamonds", 8, ""),
    C9("9 of Clubs", "clubs", 9, ""),
    S9("9 of Spades", "spades", 9, ""),
    H9("9 of Hearts", "hearts", 9, ""),
    D9("9 of Diamonds", "diamonds", 9, ""),
    C10("10 of Clubs", "clubs", 10, ""),
    S10("10 of Spades", "spades", 10, ""),
    H10("10 of Hearts", "hearts", 10, ""),
    D10("10 of Diamonds", "diamonds", 10, ""),
    JC("Jack of Clubs", "clubs", 10, ""),
    JS("Jack of Spades", "spades", 10, ""),
    JH("Jack of Hearts", "hearts", 10, ""),
    JD("Jack of Diamonds", "diamonds", 10, ""),
    QC("Queen of Clubs", "clubs", 10, ""),
    QS("Queen of Spades", "spades", 10, ""),
    QH("Queen of Hearts", "hearts", 10, ""),
    QD("Queen of Diamonds", "diamonds", 10, ""),
    KC("King of Clubs", "clubs", 10, ""),
    KS("King of Spades", "spades", 10, ""),
    KH("King of Hearts", "hearts", 10, ""),
    KD("King of Diamonds", "diamonds", 10, ""),
    AC("Ace of Clubs", "clubs", 11, ""),
    AS("Ace of Spades", "spades", 11, ""),
    AH("Ace of Hearts", "hearts", 11, ""),
    AD("Ace of Diamonds", "diamonds", 11, "");

    Cards(String name, String pips, int value, String ref) {
        this.name = name;
        this.pips = pips;
        this.value = value;
        this.ref = ref;
    }

    private final String name;
    private final String pips;
    private final int value;
    private final String ref;

    public String getName() {
        return name;
    }

    public String getPips() {
        return pips;
    }

    public int getValue() {
        return value;
    }

    public String getRef() {
        return ref;
    }
}
