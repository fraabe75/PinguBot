package com.example.demo.plugins.impl.roulette;

import com.example.demo.database.entities.UserEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class RouletteBoard {

    private Stack<ColorEmotePair> colorEmoteMap;
    private final List<RouletteField> fields;
    private final Map<Long, UserColorEmoteTriple> players;
    private long updateMessage;
    private boolean finished = false;

    // time for users to submit bets in milliseconds
    private int betTime = 60000;

    public RouletteBoard(Consumer<RouletteBoard> resultConsumer) {
        fields = new ArrayList<>();
        updateMessage = Long.MIN_VALUE;
        players = new HashMap<>();

        fillPlayerColorStack();

        for (int i = 0; i <= 36; i++) {
            fields.add(new Number(String.valueOf(i)));
        }
        fields.addAll(Arrays.asList(Field.values()));

        new Thread(() -> {
            while (betTime > 0) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(1000);
                    betTime -= 1000;
                } catch (InterruptedException e) {
                    System.err.println(e.toString());
                }
            }
            resultConsumer.accept(this);
        }).start();
    }

    private void fillPlayerColorStack() {
        colorEmoteMap = new Stack<>();
        //colorEmoteMap.add(new ColorEmotePair(Color.BLACK, ":black_circle:"));
        colorEmoteMap.add(new ColorEmotePair(Color.WHITE, ":white_circle:"));
        colorEmoteMap.add(new ColorEmotePair(Color.RED, ":red_circle:"));
        colorEmoteMap.add(new ColorEmotePair(Color.BLUE, ":blue_circle:"));
        colorEmoteMap.add(new ColorEmotePair(Color.getHSBColor(40, 100, 40), ":brown_circle:"));
        colorEmoteMap.add(new ColorEmotePair(Color.MAGENTA, ":purple_circle:"));
        //colorEmoteMap.add(new ColorEmotePair(Color.YELLOW, ":yellow_circle:"));
        colorEmoteMap.add(new ColorEmotePair(Color.ORANGE, ":orange_circle:"));
    }

    public boolean isNewPlayer(long userID) {
        return !players.containsKey(userID);
    }

    public static List<MessageEmbed.Field> getFieldHelpFields() {
        List<MessageEmbed.Field> fieldInfos = new ArrayList<>();

        for (int i = 0; i < Field.values().length; i++) {
            Field field = Field.values()[i];
            fieldInfos.add(
                    new MessageEmbed.Field(
                            field.toString(),
                            field.getDescription() + ", " + (field.getPayout() - 1) + ":1", true)
            );
            if (i == 1 || i == 3) {
                fieldInfos.add(new MessageEmbed.Field("", "", true));
            }
        }
        fieldInfos.add(new MessageEmbed.Field("0 - 36", "35:1", true));
        return fieldInfos;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished() {
        this.finished = true;
        for (RouletteField field : fields) {
            if (field.isSpecialField()) {
                ((Field) field).clear();
            }
        }
    }

    public int getBetTimeRemaining() {
        return betTime;
    }

    public int maxColorPlayers() {
        return 6;
    }

    public List<RouletteField> getFields() {
        return fields;
    }

    public Map<Long, UserColorEmoteTriple> getPlayers() {
        return players;
    }

    public Long getUpdateMessage() {
        return updateMessage;
    }

    public void setUpdateMessage(Long updateMessage) {
        this.updateMessage = updateMessage;
    }

    public boolean addPlayer(UserEntity player) {
        if (colorEmoteMap.isEmpty()) {
            players.put(player.getUserId(), new UserColorEmoteTriple(player, null, ""));
            return false;
        }
        ColorEmotePair color = colorEmoteMap.pop();
        players.put(player.getUserId(), new UserColorEmoteTriple(player, color.getColor(), color.getEmote()));
        return true;
    }

    public boolean addBet(long userID, long amount, String fieldSelect) {
        for (RouletteField field : fields) {
            if (field.isThisField(fieldSelect.strip())) {
                field.addBet(userID, amount);
                return true;
            }
        }
        return false;
    }

    public MessageEmbed.Field getBetsRepr() {
        StringBuilder betBuilder = new StringBuilder();
        Map<Long, List<RouletteField>> userBets = new HashMap<>();

        for (RouletteField field : fields) {
            field.getCurrentBets().forEach(
                    (uID, bet) -> {
                        if (userBets.containsKey(uID)) {
                            userBets.get(uID).add(field);
                        } else {
                            List<RouletteField> newList = new ArrayList<>();
                            newList.add(field);
                            userBets.put(uID, newList);
                        }
                    }
            );
        }
        userBets.forEach((uID, rltFields) ->
                betBuilder.append(players.get(uID).getEmote())
                          .append(" ")
                          .append(players.get(uID).getUser().getUserName())
                          .append(": ")
                          .append(rltFields.stream()
                                           .map(rltField -> rltField +
                                                            " (" +
                                                            rltField.getCurrentBets()
                                                                    .get(uID) +
                                                            ")"
                                           )
                                           .collect(Collectors.joining(", "))
                          )
                          .append("\n")
        );

        if (betBuilder.toString().isBlank()) {
            if (betTime <= 0) {
                betBuilder.append("No fish was placed on the board!");
            } else {
                betBuilder.append("No bets, be the first to loose fish!");
            }
        }
        return new MessageEmbed.Field("Current Bets:", betBuilder.toString(), false);
    }

    public Map<Long, Long> addPayoutPerUser(EmbedBuilder builder, int rolledNumber) {
        Map<Long, Long> userPayoutMap = new HashMap<>();
        fields.forEach(field -> field.getCurrentBets().forEach(
                (uID, betAmount) -> userPayoutMap.merge(
                        uID,
                        (isPayoutField(field, rolledNumber) ? field.calculatePayout(betAmount) : 0),
                        Long::sum
                ))
        );
        userPayoutMap.forEach((uID, investReturn) -> builder.addField(
                players.get(uID).getUser().getUserName(),
                investReturn + " :fish: (" + (investReturn > 0 ? "+" : "-") + "1 Mateability)",
                false)
        );
        if (userPayoutMap.isEmpty()) {
            builder.setDescription("No fish was thrown on the board,\nso there won't be any payout!");
        } else {
            String[] inspirationalGIFS = {
                    "https://media1.tenor.com/images/67c3c645965f9dc695a0685c4ca67c1b/tenor.gif",
                    "https://media1.tenor.com/images/e763b96b083acc07b12db01cac2f0c2a/tenor.gif",
                    "https://media1.tenor.com/images/e5a5b641e155541428d17730ff14e929/tenor.gif",
                    "https://media1.tenor.com/images/d478e6308d8b73b438600912e0d8c853/tenor.gif",
                    "https://media1.tenor.com/images/f240426d58c969c2da5bff787c0a0113/tenor.gif",
                    "https://media1.tenor.com/images/eafc3eb70afa2cd617dc0ff940f47131/tenor.gif",
                    "https://media.giphy.com/media/l2SpO2558KNLdARcQ/source.gif",
                    "https://media.giphy.com/media/l2SpNj080EmduKVEs/source.gif"
            };
            if (new Random().nextInt(10) == 1) {
                builder.setImage(inspirationalGIFS[new Random().nextInt(inspirationalGIFS.length)]);
            }
        }
        return userPayoutMap;
    }

    private boolean isPayoutField(RouletteField field, int rolledNumber) {
        if (!field.isSpecialField()) {
            return Integer.parseInt(field.toString()) == rolledNumber;
        }
        try {
            return switch ((Field) field) {
                case BLACK, RED -> Number.COLOR_TABLE[rolledNumber] == Color.class.getField(field.toString()).get(null);
                case EVEN -> rolledNumber % 2 == 0;
                case ODD -> rolledNumber % 2 == 1;
                case COL_1 -> rolledNumber % 3 == 1;
                case COL_2 -> rolledNumber % 3 == 2;
                case COL_3 -> rolledNumber % 3 == 0 && rolledNumber != 0;
                case SQR_1 -> rolledNumber != 0 && rolledNumber <= 12;
                case SQR_2 -> rolledNumber >= 13 && rolledNumber <= 24;
                case SQR_3 -> rolledNumber >= 25;
                case LOW -> rolledNumber <= 18 && rolledNumber != 0;
                case HIGH -> rolledNumber >= 19;
            };
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return false;
        }
    }

}
